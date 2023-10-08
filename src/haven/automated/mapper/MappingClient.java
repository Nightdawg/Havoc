package haven.automated.mapper;

import haven.*;
import haven.MCache.LoadingMap;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/** @author Vendan **/
public class MappingClient {
    private ExecutorService gridsUploader = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    private static volatile MappingClient INSTANCE = null;
    
    private int spamPreventionVal = 3;
    private int spamCount = 0;
    private Glob glob;
    
    public static void init(Glob glob) {
	synchronized (MappingClient.class) {
	    if(INSTANCE == null) {
		INSTANCE = new MappingClient(glob);
	    } else {
		throw new IllegalStateException("MappingClient can only be initialized once!");
	    }
	}
    }
    
    public static void destroy() {
	synchronized (MappingClient.class) {
	    if(INSTANCE != null) {
	        INSTANCE.gridsUploader.shutdown();
	        INSTANCE.scheduler.shutdown();
		INSTANCE = null;
	    }
	}
    }
    
    public static boolean initialized() {return INSTANCE != null;}
    
    public static MappingClient getInstance() {
	synchronized (MappingClient.class) {
	    if(INSTANCE == null) {
		return null;
	    }
	    return INSTANCE;
	}
    }

    private PositionUpdates pu = new PositionUpdates();
    
    private MappingClient(Glob glob) {
	this.glob = glob;
	scheduler.scheduleAtFixedRate(pu, 2L, 2L, TimeUnit.SECONDS);
    }

    private String playerName;

    public void SetPlayerName(String name) {
	playerName = name;
    }

    public void Track(long id, Coord2d coordinates) {
	try {
	    MCache.Grid g = glob.map.getgrid(toGC(coordinates));
	    pu.Track(id, coordinates, g.id);
	} catch (Exception ex) {}
    }
    
    private Coord lastGC = null;

    public void EnterGrid(Coord gc) {
	lastGC = gc;
	scheduler.execute(new GenerateGridUpdateTask(gc));
    }

    public void CheckGridCoord(Coord2d c) {
	Coord gc = toGC(c);
	if(lastGC == null || !gc.equals(lastGC)) {
	    EnterGrid(gc);
	}
    }
    
    private final Map<Long, MapRef> cache = new HashMap<Long, MapRef>();

    public void ProcessMap(MapFile mapfile, Predicate<MapFile.Marker> uploadCheck) {
	scheduler.schedule(new ExtractMapper(mapfile, uploadCheck), 5, TimeUnit.SECONDS);
    }

    private class ExtractMapper implements Runnable {
	MapFile mapfile;
	Predicate<MapFile.Marker> uploadCheck;
	int retries = 5;

	ExtractMapper(MapFile mapfile, Predicate<MapFile.Marker> uploadCheck) {
	    this.mapfile = mapfile;
	    this.uploadCheck = uploadCheck;
	}

	@Override
	public void run() {
	    if(mapfile.lock.readLock().tryLock()) {
		List<MarkerData> markers = mapfile.markers.stream().filter(uploadCheck).map(m -> {
		    Coord mgc = new Coord(Math.floorDiv(m.tc.x, 100), Math.floorDiv(m.tc.y, 100));
		    Indir<MapFile.Grid> indirGrid = mapfile.segments.get(m.seg).grid(mgc);
		    return new MarkerData(m, indirGrid);
		}).collect(Collectors.toList());
		mapfile.lock.readLock().unlock();
		scheduler.execute(new ProcessMapper(mapfile, markers));
	    } else {
		if(retries-- > 0) {
		    scheduler.schedule(this, 5, TimeUnit.SECONDS);
		}
	    }
	}
    }

    private class MarkerData {
	MapFile.Marker m;
	Indir<MapFile.Grid> indirGrid;

	MarkerData(MapFile.Marker m, Indir<MapFile.Grid> indirGrid) {
	    this.m = m;
	    this.indirGrid = indirGrid;
	}
    }

    private class ProcessMapper implements Runnable {
	MapFile mapfile;
	List<MarkerData> markers;

	ProcessMapper(MapFile mapfile, List<MarkerData> markers) {
	    this.mapfile = mapfile;
	    this.markers = markers;
	}

	@Override
	public void run() {
	    ArrayList<JSONObject> loadedMarkers = new ArrayList<>();
	    while (!markers.isEmpty()) {
		Iterator<MarkerData> iterator = markers.iterator();
		while (iterator.hasNext()) {
		    MarkerData md = iterator.next();
		    try {
			Coord mgc = new Coord(Math.floorDiv(md.m.tc.x, 100), Math.floorDiv(md.m.tc.y, 100));
			long gridId = md.indirGrid.get().id;
			JSONObject o = new JSONObject();
			o.put("name", md.m.nm);
			o.put("gridID", String.valueOf(gridId));
			Coord gridOffset = md.m.tc.sub(mgc.mul(100));
			o.put("x", gridOffset.x);
			o.put("y", gridOffset.y);

			if(md.m instanceof MapFile.SMarker) {
			    o.put("type", "shared");
			    o.put("id", ((MapFile.SMarker) md.m).oid);
			    o.put("image", ((MapFile.SMarker) md.m).res.name);
			} else if(md.m instanceof MapFile.PMarker) {
			    o.put("type", "player");
			    o.put("color", ((MapFile.PMarker) md.m).color);
			}
			loadedMarkers.add(o);
			iterator.remove();
		    } catch (Loading ex) {
		    }
		}
		try {
		    Thread.sleep(50);
		} catch (InterruptedException ex) { }
	    }
	    try {
		scheduler.execute(new MarkerUpdate(new JSONArray(loadedMarkers.toArray())));
	    } catch (Exception ex) {
		System.out.println(ex);
	    }
	}
    }

    private class MarkerUpdate implements Runnable {
	JSONArray data;

	MarkerUpdate(JSONArray data) {
	    this.data = data;
	}

	@Override
	public void run() {
	    try {
		HttpURLConnection connection =
		    (HttpURLConnection) new URL(OptWnd.mapClientEndpoint + "/markerUpdate").openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		connection.setDoOutput(true);
		try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
		    final String json = data.toString();
		    out.write(json.getBytes(StandardCharsets.UTF_8));
		}
		int code = connection.getResponseCode();
		connection.disconnect();
	    } catch (Exception ex) {
		System.out.println(ex);
	    }
	}
    }
    
    private class PositionUpdates implements Runnable {
	private class Tracking {
	    public String name;
	    public String type;
	    public long gridId;
	    public Coord2d coords;
	    
	    public JSONObject getJSON() {
		JSONObject j = new JSONObject();
		j.put("name", name);
		j.put("type", type);
		j.put("gridID", String.valueOf(gridId));
		JSONObject c = new JSONObject();
		c.put("x", (int) (coords.x / 11));
		c.put("y", (int) (coords.y / 11));
		j.put("coords", c);
		return j;
	    }
	}
	
	private Map<Long, Tracking> tracking = new ConcurrentHashMap<Long, Tracking>();
	
	private PositionUpdates() {
	}
	
	private void Track(long id, Coord2d coordinates, long gridId) {
	    Tracking t = tracking.get(id);
	    if(t == null) {
		t = new Tracking();
		tracking.put(id, t);
		
		if(id == glob.sess.ui.gui.map.plgob) {
		    t.name = playerName;
		    t.type = "player";
		} else {
		    Glob g = glob;
		    Gob gob = g.oc.getgob(id);
		    t.name = "???";
		    t.type = "white";
		    if(gob != null) {
			KinInfo ki = gob.getattr(KinInfo.class);
			if(ki != null) {
			    t.name = ki.name;
			    t.type = Integer.toHexString(BuddyWnd.gc[ki.group].getRGB());
			}
		    }
		}
	    }
	    t.gridId = gridId;
	    t.coords = gridOffset(coordinates);
	}
	
	@Override
	public void run() {
	    if(spamCount == spamPreventionVal) {
		spamCount = 0;
		if(OptWnd.trackingEnableBoolean) {
		    Glob g = glob;
		    Iterator<Map.Entry<Long, Tracking>> i = tracking.entrySet().iterator();
		    JSONObject upload = new JSONObject();
		    while (i.hasNext()) {
			Map.Entry<Long, Tracking> e = i.next();
			if(g.oc.getgob(e.getKey()) == null) {
			    i.remove();
			} else {
			    upload.put(String.valueOf(e.getKey()), e.getValue().getJSON());
			}
		    }
		    
		    try {
			final HttpURLConnection connection =
			    (HttpURLConnection) new URL(OptWnd.mapClientEndpoint + "/positionUpdate").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			connection.setDoOutput(true);
			try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
			    final String json = upload.toString();
			    out.write(json.getBytes(StandardCharsets.UTF_8));
			} catch (Exception e) {
			}
			connection.getResponseCode();
		    } catch (final Exception ex) {
		    }
		}
	    } else {
		spamCount++;
	    }
	}
    }
    
    private static class GridUpdate {
	String[][] grids;
	Map<String, WeakReference<MCache.Grid>> gridRefs;
	
	GridUpdate(final String[][] grids, Map<String, WeakReference<MCache.Grid>> gridRefs) {
	    this.grids = grids;
	    this.gridRefs = gridRefs;
	}
	
	@Override
	public String toString() {
	    return String.format("GridUpdate (%s)", grids[1][1]);
	}
    }
    
    private class GenerateGridUpdateTask implements Runnable {
	Coord coord;
	int retries = 3;
	
	GenerateGridUpdateTask(Coord c) {
	    this.coord = c;
	}
	
	@Override
	public void run() {
	    if(OptWnd.mapUploadBoolean) {
		final String[][] gridMap = new String[3][3];
		Map<String, WeakReference<MCache.Grid>> gridRefs = new HashMap<String, WeakReference<MCache.Grid>>();
		try {
		    for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
			    final MCache.Grid subg = glob.map.getgrid(coord.add(x, y));
			    gridMap[x + 1][y + 1] = String.valueOf(subg.id);
			    gridRefs.put(String.valueOf(subg.id), new WeakReference<MCache.Grid>(subg));
			}
		    }
		    scheduler.execute(new UploadGridUpdateTask(new GridUpdate(gridMap, gridRefs)));
		} catch (LoadingMap lm) {
		    retries--;
		    if(retries >= 0) {
			scheduler.schedule(this, 1L, TimeUnit.SECONDS);
		    }
		} catch (Exception e) {
		    System.out.println(e);
		}
		;
	    }
	}
    }
    
    private class UploadGridUpdateTask implements Runnable {
	private final GridUpdate gridUpdate;
	
	UploadGridUpdateTask(final GridUpdate gridUpdate) {
	    this.gridUpdate = gridUpdate;
	}
	
	@Override
	public void run() {
	    if(OptWnd.mapUploadBoolean) {
		HashMap<String, Object> dataToSend = new HashMap<>();
		
		dataToSend.put("grids", this.gridUpdate.grids);
		try {
		    HttpURLConnection connection =
			(HttpURLConnection) new URL(OptWnd.mapClientEndpoint + "/gridUpdate").openConnection();
		    connection.setRequestMethod("POST");
		    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		    connection.setDoOutput(true);
		    try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
			String json = new JSONObject(dataToSend).toString();
			out.write(json.getBytes(StandardCharsets.UTF_8));
		    }
		    if(connection.getResponseCode() == 200) {
			DataInputStream dio = new DataInputStream(connection.getInputStream());
			int nRead;
			byte[] data = new byte[1024];
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			while ((nRead = dio.read(data, 0, data.length)) != -1) {
			    buffer.write(data, 0, nRead);
			}
			buffer.flush();
			String response = buffer.toString(StandardCharsets.UTF_8.name());
			JSONObject jo = new JSONObject(response);
			JSONArray reqs = jo.optJSONArray("gridRequests");
			synchronized (cache) {
			    cache.put(Long.valueOf(gridUpdate.grids[1][1]), new MapRef(jo.getLong("map"), new Coord(jo.getJSONObject("coords").getInt("x"), jo.getJSONObject("coords").getInt("y"))));
			}
			for (int i = 0; reqs != null && i < reqs.length(); i++) {
			    gridsUploader.execute(new GridUploadTask(reqs.getString(i), gridUpdate.gridRefs.get(reqs.getString(i))));
			}
		    }
		    
		} catch (Exception ignored) {}
	    }
	}
    }
    
    private class GridUploadTask implements Runnable {
	private final String gridID;
	private final WeakReference<MCache.Grid> grid;
	
	GridUploadTask(String gridID, WeakReference<MCache.Grid> grid) {
	    this.gridID = gridID;
	    this.grid = grid;
	}
	
	@Override
	public void run() {
	    try {
		MCache.Grid g = grid.get();
		if(g != null && glob != null) {
			BufferedImage image = MinimapImageGenerator.drawmap(glob.map, g);
		    if(image == null) {
			throw new Loading();
		    }
		    try {
			JSONObject extraData = new JSONObject();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
			MultipartUtility multipart = new MultipartUtility(OptWnd.mapClientEndpoint + "/gridUpload", "utf-8");
			multipart.addFormField("id", this.gridID);
			multipart.addFilePart("file", inputStream, "minimap.png");
			extraData.put("season", glob.ast.is);
			multipart.addFormField("extraData", extraData.toString());
			MultipartUtility.Response response = multipart.finish();
		    } catch (IOException ignored) {}
		}
	    } catch (Loading ex) {
		gridsUploader.submit(this);
	    }
	    
	}
    }
    
    private static Coord toGC(Coord2d c) {
	return new Coord(Math.floorDiv((int) c.x, 1100), Math.floorDiv((int) c.y, 1100));
    }
    
    private static Coord toGridUnit(Coord2d c) {
	return new Coord(Math.floorDiv((int) c.x, 1100) * 1100, Math.floorDiv((int) c.y, 1100) * 1100);
    }
    
    private static Coord2d gridOffset(Coord2d c) {
	Coord gridUnit = toGridUnit(c);
	return new Coord2d(c.x - gridUnit.x, c.y - gridUnit.y);
    }
    
    public class MapRef {
	public Coord gc;
	public long mapID;
	
	private MapRef(long mapID, Coord gc) {
	    this.gc = gc;
	    this.mapID = mapID;
	}
	
	public String toString() {
	    return (gc.toString() + " in map space " + mapID);
	}
    }
}