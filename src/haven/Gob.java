/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.automated.GobSearcher;
import haven.automated.helpers.HitBoxes;
import haven.automated.mapper.MappingClient;
import haven.render.*;
import haven.render.gl.GLObject;
import haven.res.gfx.fx.msrad.MSRad;
import haven.res.lib.svaj.GobSvaj;
import haven.res.lib.tree.TreeScale;
import haven.sprites.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;


public class Gob implements RenderTree.Node, Sprite.Owner, Skeleton.ModOwner, EquipTarget, Skeleton.HasPose {
	public static Set<Long> listHighlighted = new HashSet<>();
    public Coord2d rc;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public boolean removed = false;
    public final Glob glob;
    public ConcurrentHashMap<Class<? extends GAttrib>, GAttrib> attr = new ConcurrentHashMap<>();
    public final Collection<Overlay> ols = new ArrayList<Overlay>();
	public Collection<Overlay> tempOls = new ArrayList<Overlay>();
    public final Collection<RenderTree.Slot> slots = new ArrayList<>(1);
    public int updateseq = 0;
    private final Collection<SetupMod> setupmods = new ArrayList<>();
    private final LinkedList<Runnable> deferred = new LinkedList<>();
    private Loader.Future<?> deferral = null;
	private GobDamageInfo damage;
	public StatusUpdates status = new StatusUpdates();
	public CollisionBoxGobSprite<CollisionBox> collisionBox = null;
	private CollisionBoxGobSprite<HidingBox> collisionBox2 = null;
	private CollisionBoxGobSprite<HidingBoxFilled> hidingBox = null;
	private GobGrowthInfo growthInfo;
	private GobQualityInfo qualityInfo;
	private final List<Overlay> dols = new ArrayList<>();
	private Overlay customRadiusOverlay;
	private Overlay customSearchOverlay;
	private Overlay customOverlay;
	private Overlay gobChaseVector = null;
	public Boolean knocked = null;  // knocked will be null if pose update request hasn't been received yet
	public int playerPoseUpdatedCounter = 0;
	public Boolean isMannequin = null;
	private Boolean isMe = null;
	private Boolean playerAlarmPlayed = false;
	public Boolean isComposite = false;
	public Boolean isDeadPlayer = false;
	public double gobSpeed = 0;
	public static final HashSet<Long> alarmPlayed = new HashSet<Long>();

	private Overlay archeryVector;
	private Overlay archeryRadius;

	public static Boolean batsLeaveMeAlone = false; // ND: Check for Bat Cape
	public static Boolean batsFearMe = false; // ND: Check for Bat Dungeon Experience (Defeated Bat Queen)

	public final ArrayList<Gob> occupants = new ArrayList<Gob>();
	public Long occupiedGobID = null;
	public static boolean somethingJustDied = false;
	public static final ScheduledExecutorService gobDeathExecutor = Executors.newSingleThreadScheduledExecutor();
	private static Future<?> gobDeathFuture;
	private boolean malePlayer = false;
	private boolean femalePlayer = false;
	public Boolean imInCoracle = false;
	public Boolean imDrinking = false;
	private Boolean itsLoftar = null;
	private long lastKnockSoundtime = 0;

	//some fps increasing features
	private final Set<String> animatedGobs = new HashSet<>(Arrays.asList("dreca", "pow", "kiln", "cauldron", "beehive", "stockpile-trash"));
	private boolean disableThisGobAnimations = false;
	public static boolean disableGlobalGobAnimations = Utils.getprefb("disableSomeGobAnimations", false);
	public static boolean disableScentSmoke = Utils.getprefb("disableScentSmoke", false);
	public static boolean disableIndustrialSmoke = Utils.getprefb("disableIndustrialSmoke", false);

	/**
	 * This method is run after all gob attributes has been loaded first time
	 * throwloading=true causes the loader/thread that ran the init to try again
	 */
	public void init(boolean throwLoading) {
		Resource res = getres();
		if (res != null) {
			String resName = res.basename();
			for (String substring : animatedGobs) {
				if (resName.contains(substring)) {
					disableThisGobAnimations = true;
					break;
				}
			}
			setGobSearchOverlay();
			setHighlightedObjects();
			initiateSupportOverlays();
			toggleMineLadderRadius();
			toggleBeeSkepRadius();
			toggleTroughsRadius();
			HitBoxes.addHitBox(this);
			if (getattr(Drawable.class) instanceof Composite) {
				try {
					initComp((Composite)getattr(Drawable.class));
//					knocked = false;
					isComposite = true;
					if(!alarmPlayed.contains(id)) {
						if(AlarmManager.play(res.name, Gob.this))
							alarmPlayed.add(id);
					}
					initiateAnimalOverlays();
					initCustomGAttrs();
				} catch (Loading e) {
					if (!throwLoading) {
						glob.loader.syncdefer(() -> this.init(true), null, this); // ND: I don't understand what this was supposed to do.
					} else {
						throw e;
					}
				}
			} else {
				initCustomGAttrs();
				if(!alarmPlayed.contains(id)) {
					if(AlarmManager.play(res.name, Gob.this))
						alarmPlayed.add(id);
				}
				initiateAnimalOverlays();
			}
			toggleSpeedBuffAuras();
		}
	}

	public void updPose(HashSet<String> poses) {
		isComposite = true;
		Iterator<String> iter = poses.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (s.contains("knock") || s.contains("dead") || s.contains("waterdead")) {
				knocked = true;
				break;
			} else {
				knocked = false;
			}
			if (s.contains("mannequin")){
				isMannequin = true;
				break;
			} else {
				isMannequin = false;
			}
		}
		if (knocked != null && knocked) {
			try {
				removeOl(customRadiusOverlay);
				customRadiusOverlay = null;
			} catch (Exception e){
				CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
			}
		} else {
			knocked = false;
		}
		if (this.getres().name.equals("gfx/borka/body") && isMannequin != null && !isMannequin){
			boolean imOnLand = true;
			imInCoracle = false;
			Iterator<String> iter2 = poses.iterator();
			while (iter2.hasNext()) {
				String s = iter2.next();
				if (s.contains("coracleidle") || s.contains("coraclerowan")) {
					imOnLand = false;
					imInCoracle = true;
					break;
				}
				if (s.contains("rowboat") || s.contains("snekkja") || s.contains("knarr") || s.contains("dugout")) {
					imOnLand = false;
					break;
				}
			}
			if (poses.contains("spear-ready")) {
				archeryIndicator(155, imOnLand);
			} else if (poses.contains("sling-aim")) {
				archeryIndicator(155, imOnLand);
			} else if (poses.contains("drawbow")) {
				for (GAttrib g : this.attr.values()) {
					if (g instanceof Drawable) {
						if (g instanceof Composite) {
							Composite c = (Composite) g;
							if (c.comp.cequ.size() > 0) {
								for (Composited.ED item : c.comp.cequ) {
									if (item.res.res.get().basename().equals("huntersbow"))
										archeryIndicator(195, imOnLand);
									else if (item.res.res.get().basename().equals("rangersbow"))
										archeryIndicator(252, imOnLand);
								}
							}
						}
					}
				}
			} else {
				removeOl(archeryVector);
				archeryVector = null;
				removeOl(archeryRadius);
				archeryRadius = null;
			}
			if (poses.contains("drinkan")) {
				imDrinking = true;
			} else {
				imDrinking = false;
			}
			if  (!isDeadPlayer){
				checkIfPlayerIsDead(poses);
				if (playerPoseUpdatedCounter >= 2) { // ND: Do this to prevent the sounds from being played if you load in an already knocked/killed hearthling.
					knockedOrDeadPlayerSoundEfect(poses);
				}
				playerPoseUpdatedCounter = playerPoseUpdatedCounter + 1;
			}
		}
	}
	public void initComp(Composite c) {
		c.cmpinit(this);
	}

	public void initPlayerName() {
		if (getattr(KinInfo.class) == null && isMannequin != null && isMannequin == false && glob.sess.ui.gui != null && glob.sess.ui.gui.map != null) {
			if (getres() != null) {
				if (getres().name.equals("gfx/borka/body")) {
					long plgobid = glob.sess.ui.gui.map.plgob;
					if (plgobid != -1 && plgobid != id) {
						setattr(new KinInfo(this, "Unknown", 0, 0));
					}
				}
			}
		}
	}

	public void checkIfItsLoftar() {
		if (getres() != null) {
			if (getres().name.equals("gfx/borka/body")) {
				for (GAttrib g : attr.values()) {
					if (g instanceof Drawable) {
						if (g instanceof Composite) {
							Composite c = (Composite) g;
							boolean ismale = false;
							if (c.comp.cmod.size() > 0) {
								for (Composited.MD item : c.comp.cmod) {
									if (item.mod.get().basename().equals("male")){
										ismale = true;
										break;
									}
								}
								if (!ismale) {
									itsLoftar = false;
									return;
								}
							}
							if (ismale) {
								boolean isgandalfhat = false;
								boolean isravens = false;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) {
										if (item.res.res.get().basename().equals("gandalfhat")){
											isgandalfhat = true;
										} else if (item.res.res.get().basename().equals("ravens")){
											isravens = true;
										}
										if (isgandalfhat && isravens){
											break;
										}
									}
									if (isgandalfhat && isravens){
										itsLoftar = true;
										setattr(new KinInfo(this, "Big Daddy Loftar", 0, 0));
										return;
									} else {
										itsLoftar = false;
										return;
									}
								}
							} else {
								return;
							}
						}
					}
				}
			} else {
				itsLoftar = false;
			}
		}
	}

    public static class Overlay implements RenderTree.Node {
	public final int id;
	public final Gob gob;
	public final Indir<Resource> res;
	public MessageBuf sdt;
	public Sprite spr;
	public boolean delign = false;
	private Collection<RenderTree.Slot> slots = null;
	private boolean added = false;

	public Overlay(Gob gob, int id, Indir<Resource> res, Message sdt) {
	    this.gob = gob;
	    this.id = id;
	    this.res = res;
	    this.sdt = new MessageBuf(sdt);
	    this.spr = null;
	}

		public Overlay(Gob gob, int id, Sprite spr) {
			this.gob = gob;
			this.id = id;
			this.res = null;
			this.sdt = null;
			this.spr = spr;
		}

	public Overlay(Gob gob, Sprite spr) {
	    this.gob = gob;
	    this.id = -1;
	    this.res = null;
	    this.sdt = null;
	    this.spr = spr;
	}

	private void init() {
	    if(spr == null) {
			//Minesweeper data
//			if(res != null && res.get().name.contains("gfx/fx/cavewarn")){
//				try{
//					System.out.println(this.gob.rc + " cavein: " + sdt.peekUint8());
//				} catch (Exception e){
//
//				}
//			}
//			if(this.gob != null) {
//				if (gob.getres() != null && res.get().name.contains("mineout")) {
//					System.out.println(gob.getres().name + " - " + gob.rc + " - " + res.get().name + System.currentTimeMillis());
//				}
//			}
//			if(this.gob != null){
//				if(gob.getres() == null && res.get().name.contains("mineout")) {
//					System.out.println(gob.getClass().getName() + " - " + gob.virtual + gob.rc + System.currentTimeMillis());
//				}
//			}
		spr = Sprite.create(gob, res.get(), sdt);
		if(added && (spr instanceof SetupMod)) {
		    gob.setupmods.add((SetupMod)spr);
		}
	    }
	    if(slots == null)
		RUtils.multiadd(gob.slots, this);
	}

	private void add0() {
	    if(added)
		throw(new IllegalStateException());
	    if(spr instanceof SetupMod)
		gob.setupmods.add((SetupMod)spr);
	    added = true;
	}

	private void remove0() {
	    if(!added)
		throw(new IllegalStateException());
	    if(slots != null) {
		RUtils.multirem(new ArrayList<>(slots));
		slots = null;
	    }
	    if(spr instanceof SetupMod)
		gob.setupmods.remove(spr);
	    added = false;
	}

	public void remove(boolean async) {
	    if(async) {
			gob.defer(() -> remove(false));
			return;
	    }
	    remove0();
	    gob.ols.remove(this);
		gob.olRemoved();
	}

	public void remove() {
	    remove(true);
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	    if(slots == null)
		slots = new ArrayList<>(1);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    if(slots != null)
		slots.remove(slot);
	}
    }
	public void removeOl(Overlay ol) {
		if (ol != null) {
			synchronized (ols) {
				ol.remove();
			}
		}
	}

    public static interface SetupMod {
	public default Pipe.Op gobstate() {return(null);}
	public default Pipe.Op placestate() {return(null);}
    }

    public static interface Placer {
	/* XXX: *Quite* arguably, the distinction between getc and
	 * getr should be abolished and a single transform matrix
	 * should be used instead, but that requires first abolishing
	 * the distinction between the gob/gobx location IDs. */
	public Coord3f getc(Coord2d rc, double ra);
	public Matrix4f getr(Coord2d rc, double ra);
    }

    public static interface Placing {
	public Placer placer();
    }

    public static class DefaultPlace implements Placer {
	public final MCache map;
	public final MCache.SurfaceID surf;

	public DefaultPlace(MCache map, MCache.SurfaceID surf) {
	    this.map = map;
	    this.surf = surf;
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    return(map.getzp(surf, rc));
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra));
	}
    }

    public static class InclinePlace extends DefaultPlace {
	public InclinePlace(MCache map, MCache.SurfaceID surf) {
	    super(map, surf);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    Matrix4f ret = super.getr(rc, ra);
	    Coord3f norm = map.getnorm(surf, rc);
	    norm.y = -norm.y;
	    Coord3f rot = Coord3f.zu.cmul(norm);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		ret = incl.mul(ret);
	    }
	    return(ret);
	}
    }

    public static class BasePlace extends DefaultPlace {
	public final Coord2d[][] obst;
	private Coord2d cc;
	private double ca;
	private int seq = -1;
	private float z;

	public BasePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] obst) {
	    super(map, surf);
	    this.obst = obst;
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private float getz(Coord2d rc, double ra) {
	    Coord2d[][] no = this.obst, ro = new Coord2d[no.length][];
	    {
		double s = Math.sin(ra), c = Math.cos(ra);
		for(int i = 0; i < no.length; i++) {
		    ro[i] = new Coord2d[no[i].length];
		    for(int o = 0; o < ro[i].length; o++)
			ro[i][o] = Coord2d.of((no[i][o].x * c) - (no[i][o].y * s), (no[i][o].y * c) + (no[i][o].x * s)).add(rc);
		}
	    }
	    float ret = Float.NaN;
	    for(int i = 0; i < no.length; i++) {
		for(int o = 0; o < ro[i].length; o++) {
		    Coord2d a = ro[i][o], b = ro[i][(o + 1) % ro[i].length];
		    for(Coord2d c : new Line2d.GridIsect(a, b, MCache.tilesz, false)) {
			double z = map.getz(surf, c);
			if(Float.isNaN(ret) || (z < ret))
			    ret = (float)z;
		    }
		}
	    }
	    return(ret);
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		this.z = getz(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	    return(Coord3f.of((float)rc.x, (float)rc.y, this.z));
	}
    }

    public static class LinePlace extends DefaultPlace {
	public final double max, min;
	public final Coord2d k;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public LinePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points, Coord2d k) {
	    super(map, surf);
	    Line2d l = Line2d.from(Coord2d.z, k);
	    double max = 0, min = 0;
	    for(int i = 0; i < points.length; i++) {
		for(int o = 0; o < points[i].length; o++) {
		    int p = (o + 1) % points[i].length;
		    Line2d edge = Line2d.twixt(points[i][o], points[i][p]);
		    Coord2d t = l.cross(edge);
		    if((t.y >= 0) && (t.y <= 1)) {
			max = Math.max(t.x, max);
			min = Math.min(t.x, min);
		    }
		}
	    }
	    if((max == 0) || (min == 0))
		throw(new RuntimeException("illegal bounds for LinePlace"));
	    this.k = k;
	    this.max = max;
	    this.min = min;
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, String id, Coord2d k) {
	    this(map, surf, res.flayer(Resource.obst, id).p, k);
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, Coord2d k) {
	    this(map, surf, res, "", k);
	}

	private void recalc(Coord2d rc, double ra) {
	    Coord2d rk = k.rot(ra);
	    double maxz = map.getz(surf, rc.add(rk.mul(max)));
	    double minz = map.getz(surf, rc.add(rk.mul(min)));
	    Coord3f rax = Coord3f.of((float)-rk.y, (float)-rk.x, 0);
	    float dz = (float)(maxz - minz);
	    float dx = (float)(max - min);
	    float hyp = (float)Math.sqrt((dx * dx) + (dz * dz));
	    float sin = dz / hyp, cos = dx / hyp;
	    c = Coord3f.of((float)rc.x, (float)rc.y, (float)minz + (dz * (float)(-min / (max - min))));
	    r = Transform.makerot(new Matrix4f(), rax, sin, cos).mul(super.getr(rc, ra));
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(r);
	}
    }

    public static class PlanePlace extends DefaultPlace {
	public final Coord2d[] points;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public static Coord2d[] flatten(Coord2d[][] points) {
	    int n = 0;
	    for(int i = 0; i < points.length; i++)
		n += points[i].length;
	    Coord2d[] ret = new Coord2d[n];
	    for(int i = 0, o = 0; i < points.length; o += points[i++].length)
		System.arraycopy(points[i], 0, ret, o, points[i].length);
	    return(ret);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[] points) {
	    super(map, surf);
	    this.points = points;
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points) {
	    this(map, surf, flatten(points));
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private void recalc(Coord2d rc, double ra) {
	    double s = Math.sin(ra), c = Math.cos(ra);
	    Coord3f[] pp = new Coord3f[points.length];
	    for(int i = 0; i < pp.length; i++) {
		Coord2d rv = Coord2d.of((points[i].x * c) - (points[i].y * s), (points[i].y * c) + (points[i].x * s));
		pp[i] = map.getzp(surf, rv.add(rc));
	    }
	    int I = 0, O = 1, U = 2;
	    Coord3f mn = Coord3f.zu;
	    double ma = 0;
	    for(int i = 0; i < pp.length - 2; i++) {
		for(int o = i + 1; o < pp.length - 1; o++) {
		    plane: for(int u = o + 1; u < pp.length; u++) {
			Coord3f n = pp[o].sub(pp[i]).cmul(pp[u].sub(pp[i])).norm();
			for(int p = 0; p < pp.length; p++) {
			    if((p == i) || (p == o) || (p == u))
				continue;
			    float pz = (((n.x * (pp[i].x - pp[p].x)) + (n.y * (pp[i].y - pp[p].y))) / n.z) + pp[i].z;
			    if(pz < pp[p].z - 0.01)
				continue plane;
			}
			double a = n.cmul(Coord3f.zu).abs();
			if(a > ma) {
			    mn = n;
			    ma = a;
			    I = i; O = o; U = u;
			}
		    }
		}
	    }
	    this.c = Coord3f.of((float)rc.x, (float)rc.y, (((mn.x * (pp[I].x - (float)rc.x)) + (mn.y * (pp[I].y - (float)rc.y))) / mn.z) + pp[I].z);
	    this.r = Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra);
	    mn.y = -mn.y;
	    Coord3f rot = Coord3f.zu.cmul(mn);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		this.r = incl.mul(this.r);
	    }
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(this.c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(this.r);
	}
    }

    public Gob(Glob glob, Coord2d c, long id) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	if(id < 0)
	    virtual = true;
	if(GobDamageInfo.has(this)) {
		addDmg();
	}
	growthInfo = new GobGrowthInfo(this);
	setattr(GobGrowthInfo.class, growthInfo);
	qualityInfo = new GobQualityInfo(this);
	setattr(GobQualityInfo.class, qualityInfo);
	updwait(this::drawableUpdated, waiting -> {});
    }

    public Gob(Glob glob, Coord2d c) {
	this(glob, c, -1);
    }

	private Map<Class<? extends GAttrib>, GAttrib> cloneattrs() {
		synchronized (this.attr) {
			return new HashMap<>(this.attr);
		}
	}

    public void ctick(double dt) {
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values()){
		if(a instanceof ResDrawable){
			if(!disableThisGobAnimations || !disableGlobalGobAnimations){
				a.ctick(dt);
			}
		} else {
			a.ctick(dt);
		}

	}
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.slots == null) {
		try {
		    ol.init();
		} catch(Loading e) {}
	    } else {
		boolean done = ol.spr.tick(dt);
		if((!ol.delign || (ol.spr instanceof Sprite.CDel)) && done) {
		    ol.remove0();
		    i.remove();
		}
	    }
	}
	synchronized (dols) {
		for (Iterator<Overlay> i = dols.iterator(); i.hasNext(); ) {
			Overlay ol = i.next();
			addol(ol);
			i.remove();
		}
	}
	updstate();
	if(virtual && ols.isEmpty() && (getattr(Drawable.class) == null))
	    glob.oc.remove(this);
	if (itsLoftar == null)
		checkIfItsLoftar();
	if(!playerAlarmPlayed) {
		if (isMe == null)
			isMe();
		initPlayerName();
		if(isMe != null && itsLoftar != null) {
			if (!itsLoftar){
				playPlayerAlarm();
				playerAlarmPlayed = true;
			} else {
//				playLoftarAlarm();
				playerAlarmPlayed = true;
			}
		}
	}
	updateState();
	if (getattr(Moving.class) instanceof Following){
		Following following = (Following) getattr(Moving.class);
		occupiedGobID = following.tgt;
		if (occupiedGobID != null) {
			Gob OccupiedGob = glob.oc.getgob(occupiedGobID);
			if (OccupiedGob != null) {
				synchronized (OccupiedGob.occupants) {
					if (!OccupiedGob.occupants.contains(this)) {
						OccupiedGob.occupants.add(this);
					}
				}
			}
		}
	}
    }

    public void gtick(Render g) {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.gtick(g);
	for(Overlay ol : ols) {
	    if(ol.spr != null)
		ol.spr.gtick(g);
	}
    }

    void removed() {
	removed = true;
    }

    private void deferred() {
	while(true) {
	    Runnable task;
	    synchronized(deferred) {
		task = deferred.peek();
		if(task == null) {
		    deferral = null;
		    return;
		}
	    }
	    synchronized(this) {
		if(!removed)
		    task.run();
	    }
	    if(task instanceof Disposable)
		((Disposable)task).dispose();
	    synchronized(deferred) {
		if(deferred.poll() != task)
		    throw(new RuntimeException());
	    }
	}
    }

    public void defer(Runnable task) {
	synchronized(deferred) {
	    deferred.add(task);
	    if(deferral == null)
		deferral = glob.loader.defer(this::deferred, null);
	}
    }

    public void addol(Overlay ol, boolean async) {
		if(disableScentSmoke && getres() != null && getres().name.equals("gfx/terobjs/clue")){
			if(ol.res != null && ol.res.get() != null && ol.res.get().name.contains("ismoke")){
				return;
			}
		}
		if(disableIndustrialSmoke && getres() != null && !getres().name.equals("gfx/terobjs/clue")){
			if(ol.res != null && ol.res.get() != null && ol.res.get().name.contains("ismoke")){
				return;
			}
		}
	if(async) {
	    defer(() -> addol(ol, false));
	    return;
	}
	ol.init();
	ol.add0();
	ols.add(ol);
	overlayAdded(ol);
    }
    public void addol(Overlay ol) {
	addol(ol, true);
    }
    public void addol(Sprite ol) {
	addol(new Overlay(this, ol));
    }
    public void addol(Indir<Resource> res, Message sdt) {
	addol(new Overlay(this, -1, res, sdt));
    }

    public Overlay findol(int id) {
	for(Overlay ol : ols) {
	    if(ol.id == id)
		return(ol);
	}
	synchronized (dols) {
		for(Overlay ol : dols) {
			if(ol.id == id)
				return ol;
		}
	}
	return(null);
    }

	private void overlayAdded(Overlay item) {
		try {
			Indir<Resource> indir = item.res;
			if(indir != null) {
				Resource res = indir.get();
				if(res != null) {
					if(res.name.equals("gfx/fx/floatimg"))
						processDmg(item.sdt.clone());
//		    System.out.printf("overlayAdded: '%s'%n", res.name);
				}
				updateOverlayDependantHighlights();
			}
		} catch (Loading e) {CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}
	}

	private void olRemoved() {
		updateOverlayDependantHighlights();
	}

	private void processDmg(MessageBuf msg) {
		try {
			msg.rewind();
			int v = msg.int32();
			msg.uint8();
			int c = msg.uint16();

			if(damage == null) {
				addDmg();
			}
			damage.update(c, v);
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
	}

	private void addDmg() {
		damage = new GobDamageInfo(this);
		setattr(GobDamageInfo.class, damage);
	}

	public void clearDmg() {
		setattr(GobDamageInfo.class, null);
		damage = null;
	}

    public void dispose() {
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values())
	    a.dispose();
    }

    public void move(Coord2d c, double a) {
	Moving m = getattr(Moving.class);
		if (m != null) {
			m.move(c);
			this.gobSpeed = m.getv();
		} else {
			this.gobSpeed = 0;
		}
		if(Boolean.TRUE.equals(isMe()) && OptWnd.trackingEnableBoolean && MappingClient.getInstance() != null) {
			MappingClient.getInstance().CheckGridCoord(c);
			MappingClient.getInstance().Track(id, c);
		}
	this.rc = c;
	this.a = a;
    }

    public Placer placer() {
	Drawable d = getattr(Drawable.class);
	if(d != null) {
	    Placer ret = d.placer();
	    if(ret != null)
		return(ret);
	}
	return(glob.map.mapplace);
    }

    public Coord3f getc() {
	Moving m = getattr(Moving.class);
	Coord3f ret = (m != null) ? m.getc() : getrc();
	DrawOffset df = getattr(DrawOffset.class);
	if(df != null)
	    ret = ret.add(df.off);
	return(ret);
    }

    public Coord3f getrc() {
	return(placer().getc(rc, a));
    }

    protected Pipe.Op getmapstate(Coord3f pc) {
	Tiler tile = glob.map.tiler(glob.map.gettile(new Coord2d(pc).floor(MCache.tilesz)));
	return(tile.drawstate(glob, pc));
    }

    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
	while(true) {
	    Class<?> p = cl.getSuperclass();
	    if(p == GAttrib.class)
		return(cl);
	    cl = p.asSubclass(GAttrib.class);
	}
    }

    public <C extends GAttrib> C getattr(Class<C> c) {
	GAttrib attr = this.attr.get(attrclass(c));
	if(!c.isInstance(attr))
	    return(null);
	return(c.cast(attr));
    }

    private void setattr(Class<? extends GAttrib> ac, GAttrib a) {
		GAttrib prev = attr.remove(ac);
		if (prev != null) {
			if ((prev instanceof RenderTree.Node) && (prev.slots != null))
				RUtils.multirem(new ArrayList<>(prev.slots));
			if (prev instanceof SetupMod)
				setupmods.remove(prev);
		}
		if (a != null) {
			if (a instanceof RenderTree.Node && !a.skipRender) {
				try {
					RUtils.multiadd(this.slots, (RenderTree.Node) a);
				} catch (Loading l) {
					if (prev instanceof RenderTree.Node && !prev.skipRender) {
						RUtils.multiadd(this.slots, (RenderTree.Node) prev);
						attr.put(ac, prev);
					}
					if (prev instanceof SetupMod)
						setupmods.add((SetupMod) prev);
					throw (l);
				}
			}
			if (a instanceof SetupMod)
				setupmods.add((SetupMod) a);
			attr.put(ac, a);
		}
		if (ac == Drawable.class) {
			if (a != prev) drawableUpdated();
		}
		if (prev != null)
			prev.dispose();

		if(ac == Moving.class && a == null) {
			if (occupiedGobID != null){
				Gob OccupiedGob = glob.oc.getgob(occupiedGobID);
				if (OccupiedGob != null){
					synchronized (OccupiedGob.occupants) {
						OccupiedGob.occupants.remove(this);
					}
					occupiedGobID = null;
				}
			}
		}
		if (a instanceof Moving) {
			if (gobChaseVector != null) {
				gobChaseVector.remove();
				gobChaseVector = null;
			}
		}
		if (a instanceof Homing) {
			Homing homing = (Homing) a;
				if (gobChaseVector == null && homing != null) {
					gobChaseVector = new Overlay(this, new ChaseVectorSprite(this, homing));
					addol(gobChaseVector);
				} else if (gobChaseVector != null && homing != null) {
					gobChaseVector.remove();
					gobChaseVector = new Overlay(this, new ChaseVectorSprite(this, homing));
					addol(gobChaseVector);
				} else if (gobChaseVector != null) {
					gobChaseVector.remove();
					gobChaseVector = null;
				}
		}
	}


    public void setattr(GAttrib a) {
	setattr(attrclass(a.getClass()), a);
    }

    public void delattr(Class<? extends GAttrib> c) {
	setattr(attrclass(c), null);
    }

    public Supplier<? extends Pipe.Op> eqpoint(String nm, Message dat) {
	for(GAttrib attr : this.attr.values()) {
	    if(attr instanceof EquipTarget) {
		Supplier<? extends Pipe.Op> ret = ((EquipTarget)attr).eqpoint(nm, dat);
		if(ret != null)
		    return(ret);
	    }
	}
	return(null);
    }

    public static class GobClick extends Clickable {
	public final Gob gob;

	public GobClick(Gob gob) {
	    this.gob = gob;
	}

	public Object[] clickargs(ClickData cd) {
	    Object[] ret = {0, (int)gob.id, gob.rc.floor(OCache.posres), 0, -1};
	    for(Object node : cd.array()) {
		if(node instanceof Gob.Overlay) {
		    ret[0] = 1;
		    ret[3] = ((Gob.Overlay)node).id;
		}
		if(node instanceof FastMesh.ResourceMesh)
		    ret[4] = ((FastMesh.ResourceMesh)node).id;
	    }
	    return(ret);
	}

	public String toString() {
	    return(String.format("#<gob-click %d %s>", gob.id, gob.getres()));
	}
    }

    protected void obstate(Pipe buf) {
    }

    private class GobState implements Pipe.Op {
	final Pipe.Op mods;

	private GobState() {
	    if(setupmods.isEmpty()) {
		this.mods = null;
	    } else {
		Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
		int n = 0;
		for(SetupMod mod : setupmods) {
		    if((mods[n] = mod.gobstate()) != null)
			n++;
		}
		this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
	    }
	}

	public void apply(Pipe buf) {
	    if(!virtual)
		buf.prep(new GobClick(Gob.this));
	    buf.prep(new TickList.Monitor(Gob.this));
	    obstate(buf);
	    if(mods != null)
		buf.prep(mods);
	}

	public boolean equals(GobState that) {
	    return(Utils.eq(this.mods, that.mods));
	}
	public boolean equals(Object o) {
	    return((o instanceof GobState) && equals((GobState)o));
	}
    }
    private GobState curstate = null;
    private GobState curstate() {
	if(curstate == null)
	    curstate = new GobState();
	return(curstate);
    }

    private void updstate() {
	GobState nst;
	try {
	    nst = new GobState();
	} catch(Loading l) {
	    return;
	}
	if(!Utils.eq(nst, curstate)) {
	    try {
		for(RenderTree.Slot slot : slots)
		    slot.ostate(nst);
		this.curstate = nst;
	    } catch(Loading l) {
	    }
	}
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(curstate());
	for(Overlay ol : ols) {
	    if(ol.slots != null)
		slot.add(ol);
	}
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values()) {
	    if(a instanceof RenderTree.Node && !a.skipRender)
			try {
				slot.add((RenderTree.Node) a);
			} catch (GLObject.UseAfterFreeException e) {
				// ND: I have no clue what causes this, and what happens if we just ignore it?
				// >> Meeku said he crashed on this when he got out of the minehole
//				e.printStackTrace();
				// ND: I got this stacktrace once and nothing seems to have happened, so I commented it, the try catch seems to work fine.
			}
	}
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    private Waitable.Queue updwait = null;
    void updated() {
	synchronized(this) {
	    updateseq++;
	    if(updwait != null)
		updwait.wnotify();
	}
    }

    public void updwait(Runnable callback, Consumer<Waitable.Waiting> reg) {
	/* Caller should probably synchronize on this already for a
	 * call like this to even be meaningful, but just in case. */
	synchronized(this) {
	    if(updwait == null)
		updwait = new Waitable.Queue();
	    reg.accept(updwait.add(callback));
	}
    }

    public static class DataLoading extends Loading {
	public final transient Gob gob;
	public final int updseq;

	/* It would be assumed that the caller has synchronized on gob
	 * while creating this exception. */
	public DataLoading(Gob gob, String message) {
	    super(message);
	    this.gob = gob;
	    this.updseq = gob.updateseq;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(gob) {
		if(gob.updateseq != this.updseq) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    gob.updwait(callback, reg);
		}
	    }
	}
    }

    public Random mkrandoom() {
	return(Utils.mkrandoom(id));
    }

    public Resource getres() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getres());
	return(null);
    }

    public Skeleton.Pose getpose() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getpose());
	return(null);
    }

	public boolean isplayer(GameUI gui) {
		try {
			return gui.map.plgob == id;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isMoving() {
		return (getattr(Moving.class) != null);
	}

	public LinMove getLinMove() {
		LinMove lm = getattr(LinMove.class);
		if (lm != null)
			return lm;

		Following follow = getattr(Following.class);
		if (follow != null)
			return follow.tgt().getattr(LinMove.class);

		return null;
	}

    private static final ClassResolver<Gob> ctxr = new ClassResolver<Gob>()
	.add(Gob.class, g -> g)
	.add(Glob.class, g -> g.glob)
	.add(Session.class, g -> g.glob.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
	Moving m = getattr(Moving.class);
	if(m == null)
	    return(0);
	return(m.getv());
    }

    public Collection<Location.Chain> getloc() {
	Collection<Location.Chain> ret = new ArrayList<>(slots.size());
	for(RenderTree.Slot slot : slots) {
	    Location.Chain loc = slot.state().get(Homo3D.loc);
	    if(loc != null)
		ret.add(loc);
	}
	return(ret);
    }

    public class Placed implements RenderTree.Node, TickList.Ticking, TickList.TickNode {
	/* XXX: Using a COW list is far from an ideal solution. It
	 * should work for the specific case of flavobjs (which are
	 * added asynchronously in a way that makes it difficult to
	 * lock on each individually), but it's certainly not a
	 * general solution, and it would be nice with something that
	 * is in fact more general. */
	private final Collection<RenderTree.Slot> slots = new java.util.concurrent.CopyOnWriteArrayList<>();
	private Placement cur;

	private Placed() {}

	private class Placement implements Pipe.Op {
	    final Pipe.Op flw, tilestate, mods;
	    final Coord3f oc, rc;
	    final Matrix4f rot;

	    Placement() {
		try {
		    Following flw = Gob.this.getattr(Following.class);
		    Pipe.Op flwxf = (flw == null) ? null : flw.xf();
		    Pipe.Op tilestate = null;
		    if(flwxf == null) {
			Coord3f oc = Gob.this.getc();
			Coord3f rc = new Coord3f(oc);
			rc.y = -rc.y;
			this.flw = null;
			this.oc = oc;
			this.rc = rc;
			this.rot = Gob.this.placer().getr(Coord2d.of(oc), Gob.this.a);
			tilestate = Gob.this.getmapstate(oc);
		    } else {
			this.flw = flwxf;
			this.oc = this.rc = null;
			this.rot = null;
		    }
		    this.tilestate = tilestate;
		    if(setupmods.isEmpty()) {
			this.mods = null;
		    } else {
			Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
			int n = 0;
			for(SetupMod mod : setupmods) {
			    if((mods[n] = mod.placestate()) != null)
				n++;
			}
			this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
		    }
		} catch(Loading bl) {
		    throw(new Loading(bl) {
			    public String getMessage() {return(bl.getMessage());}

			    public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
				Waitable.or(callback, reg, bl, Gob.this::updwait);
			    }
			});
		}
	    }

	    public boolean equals(Placement that) {
		if(this.flw != null) {
		    if(!Utils.eq(this.flw, that.flw))
			return(false);
		} else {
		    if(!(Utils.eq(this.oc, that.oc) && Utils.eq(this.rot, that.rot)))
			return(false);
		}
		if(!Utils.eq(this.tilestate, that.tilestate))
		    return(false);
		if(!Utils.eq(this.mods, that.mods))
		    return(false);
		return(true);
	    }

	    public boolean equals(Object o) {
		return((o instanceof Placement) && equals((Placement)o));
	    }

	    Pipe.Op gndst = null;
	    public void apply(Pipe buf) {
		if(this.flw != null) {
		    this.flw.apply(buf);
		} else {
		    if(gndst == null)
			gndst = Pipe.Op.compose(new Location(Transform.makexlate(new Matrix4f(), this.rc), "gobx"),
						new Location(rot, "gob"));
		    gndst.apply(buf);
		}
		if(tilestate != null)
		    tilestate.apply(buf);
		if(mods != null)
		    mods.apply(buf);
	    }
	}

	public Pipe.Op placement() {
	    return(new Placement());
	}

	public void autotick(double dt) {
	    synchronized(Gob.this) {
		Placement np;
		try {
		    np = new Placement();
		} catch(Loading l) {
		    return;
		}
		if(!Utils.eq(this.cur, np))
		    update(np);
	    }
	}

	private void update(Placement np) {
	    for(RenderTree.Slot slot : slots)
		slot.ostate(np);
	    this.cur = np;
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(curplace());
	    slot.add(Gob.this);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public Pipe.Op curplace() {
	    if(cur == null)
		cur = new Placement();
	    return(cur);
	}

	public Coord3f getc() {
	    return((this.cur != null) ? this.cur.oc : null);
	}

	public TickList.Ticking ticker() {return(this);}
    }

	public void highlight(Color c) {
		GobHighlight h = getattr(GobHighlight.class);
		if (h != null) {
			delattr(h.getClass());
		}
		h = new GobHighlight(this, c);
		setattr(h);
		h.start();
	}

    public final Placed placed = new Placed();

	public String resid() {
		Drawable d = getattr(Drawable.class);
		if(d != null)
			return d.resId();
		return null;
	}

	//Useful for getting stage information or model type
	public int sdt() {
		Drawable d = getattr(Drawable.class);
		if(d instanceof ResDrawable) {
			ResDrawable dw = (ResDrawable) d;
			return dw.sdtnum();
		}
		return 0;
	}

	private static class StatusUpdates {
		private final Set<StatusType> updated = new HashSet<>();

		private void update(StatusType type) {
			synchronized (updated) {
				updated.add(type);
			}
		}

		private boolean updated(StatusType... types) {
			synchronized (updated) {
				for (StatusType type : types) {
					if(updated.contains(type)) {return true;}
				}
			}
			return false;
		}

		private boolean updated() {
			return !updated.isEmpty();
		}
	}

	private enum StatusType {
		collisionBox, drawable, icon, hidingBox, growthInfo, qualityInfo
	}

	public void setQuality(int q) {
		qualityInfo.setQ(q);
		status.update(StatusType.qualityInfo);
	}
	public void qualityInfoUpdated() {
		status.update(StatusType.qualityInfo);
	}
	public void growthInfoUpdated() {
		status.update(StatusType.growthInfo);
	}
	public void collisionBoxUpdated() {
		status.update(StatusType.collisionBox);
	}
	public void hidingBoxUpdated() {
		status.update(StatusType.hidingBox);
	}
	public void drawableUpdated() { status.update(StatusType.drawable); }
	public void iconUpdated() { status.update(StatusType.icon);}
	private void updateState() {
		if(updateseq == 0 || !status.updated()) {return;}
		StatusUpdates status = this.status;
		this.status = new StatusUpdates();

		if(status.updated(StatusType.collisionBox, StatusType.drawable)) {
			updateCollisionBox();
		}
		if(status.updated(StatusType.hidingBox, StatusType.drawable)) {
			updateHidingBox();
		}
		if(status.updated(StatusType.drawable, StatusType.icon)) {
			updateIcon();
		}
		if(status.updated(StatusType.growthInfo)) {
			growthInfo.clean();
		}
		if(status.updated(StatusType.qualityInfo)) {
			qualityInfo.clean();
		}
		if(status.updated(StatusType.drawable)) {
			if (virtual){
				for (int i = 0; i < ols.size(); i++) {
					Overlay ol = (Overlay) ols.toArray()[i];
					if (ol.res != null && ol.res.get().name.equals("gfx/fx/death")){
						setSomethingJustDiedStatus();
					}
				}
				updateSupportOverlays();
			}
			try {
				if (getres().name.equals("gfx/borka/body")){
					for (GAttrib g : attr.values()) {
						if (g instanceof Drawable) {
							if (g instanceof Composite) {
								Composite c = (Composite) g;
								if (c.comp.cmod.size() > 0) {
									for (Composited.MD item : c.comp.cmod) {
										if (item.mod.get().basename().equals("male")) {
											malePlayer = true;
										} else if (item.mod.get().basename().equals("female")) {
											femalePlayer = true;
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception ignored){}
		}
	}

	public static void setSomethingJustDiedStatus(){
		if (gobDeathFuture != null)
			gobDeathFuture.cancel(true);
		somethingJustDied = true;
		gobDeathFuture = gobDeathExecutor.scheduleWithFixedDelay(Gob::resetSomethingJustDiedStatus, 1, 5, TimeUnit.SECONDS);
	}
	public static void resetSomethingJustDiedStatus() {
		somethingJustDied = false;
		gobDeathFuture.cancel(true);
	}

	private void updateCollisionBox() {
		if(updateseq == 0) {return;}
		if(OptWnd.toggleGobCollisionBoxesDisplayCheckBox.a) {
			if(collisionBox != null) {
				if(!collisionBox.show(true)) {
					collisionBox.fx.updateState();
				}
			} else if(!virtual || this instanceof MapView.Plob) {
				CollisionBox collisionBox = CollisionBox.forGob(this);
				if(collisionBox != null) {
					this.collisionBox = new CollisionBoxGobSprite<>(this, collisionBox);
					addol(this.collisionBox);
				}
			}
		} else if(collisionBox != null) {
			collisionBox.show(false);
		}
	}

	private static final String[] HIDINGHOUSES = {
			"gfx/terobjs/arch/logcabin",
			"gfx/terobjs/arch/timberhouse",
			"gfx/terobjs/arch/stonestead",
			"gfx/terobjs/arch/stonemansion",
			"gfx/terobjs/arch/greathall",
			"gfx/terobjs/arch/stonetower",
			"gfx/terobjs/arch/windmill",
			"gfx/terobjs/arch/primitivetent",
			"gfx/terobjs/arch/greenhouse",
	};

	private void updateHidingBox() {
		if (updateseq == 0) {
			return;
		}
		boolean doHide = false;
		boolean doShowHidingBox = false;
		boolean isGate = false;
		Resource res = Gob.this.getres();
		if (res != null) {
			if (OptWnd.hideTreesCheckbox.a && res.name.startsWith("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideBushesCheckbox.a && res.name.startsWith("gfx/terobjs/bushes")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideBouldersCheckbox.a && res.name.startsWith("gfx/terobjs/bumlings")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideTreeLogsCheckbox.a && res.name.startsWith("gfx/terobjs/trees") && (res.name.endsWith("log") || res.name.endsWith("oldtrunk"))) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideWallsCheckbox.a && (res.name.startsWith("gfx/terobjs/arch/palisade") || res.name.startsWith("gfx/terobjs/arch/brickwall")) && !res.name.endsWith("gate")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideHousesCheckbox.a && Arrays.asList(HIDINGHOUSES).contains(res.name)) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideCropsCheckbox.a && res.name.startsWith("gfx/terobjs/plants") && !res.name.endsWith("trellis")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideStockpilesCheckbox.a && res.name.startsWith("gfx/terobjs/stockpile")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (res.name.startsWith("gfx/terobjs/arch") && res.name.endsWith("gate")) {//gates
				isGate = true;
			}
			Drawable d = getattr(Drawable.class);
			if (d != null && d.skipRender != doHide) {
				d.skipRender = doHide;
				if (doHide) {
					if (d.slots != null) {
						ArrayList<RenderTree.Slot> tmpSlots = new ArrayList<>(d.slots);
						try {
							glob.loader.defer(() -> RUtils.multiremSafe(tmpSlots), null);
						} catch (Exception ignored) {
						}
					}
				} else {
					ArrayList<RenderTree.Slot> tmpSlots = new ArrayList<>(slots);
					try {
						glob.loader.defer(() -> RUtils.multiadd(tmpSlots, d), null);
					} catch (Exception ignored) {
					}
				}
			}
			if ((OptWnd.toggleGobHidingCheckBox.a && doShowHidingBox) || (isGate && OptWnd.displayGatePassabilityBoxesCheckBox.a)) {
				if (hidingBox != null) {
					if (!hidingBox.show(true)) {
						hidingBox.fx.updateState();
					}
				} else if (!virtual || this instanceof MapView.Plob) {
					HidingBoxFilled hidingboxfilled = HidingBoxFilled.forGob(this);
					if (hidingboxfilled != null) {
						this.hidingBox = new CollisionBoxGobSprite<>(this, hidingboxfilled);
						addol(this.hidingBox);
					}
				}
				if(collisionBox2 != null) {
					if(!collisionBox2.show(true)) {
						collisionBox2.fx.updateState();
					}
				} else if(!virtual || this instanceof MapView.Plob) {
					HidingBox hidingbox = HidingBox.forGob(this);
					if(hidingbox != null) {
						this.collisionBox2 = new CollisionBoxGobSprite<>(this, hidingbox);
						addol(this.collisionBox2);
					}
				}
			} else if (hidingBox != null) {
				hidingBox.show(false);
				if(collisionBox2 != null) {
					collisionBox2.show(false);
				}
			}
		}
	}

	public Overlay daddol(final Overlay ol) {
		synchronized (dols) {
			dols.add(ol);
		}
		return ol;
	}

	public Overlay daddol(int id, Sprite spr) {
		final Overlay ol = new Overlay(this, id, spr);
		daddol(ol);
		return ol;
	}

	private void initCustomGAttrs() {
		updateOverlayDependantHighlights();
		Drawable dr = getattr(Drawable.class);
		ResDrawable d = (dr instanceof ResDrawable)?(ResDrawable)dr:null;
		if (d != null) {
			updateResPeekDependantHighlights(d.sdt);
		}
	}

	public void setGobSearchOverlay() {
		if (getres() != null) {
			String resourceName = getres().basename().replace("stockpile", "");
			resourceName = resourceName.toLowerCase();
			String searchKeyword = GobSearcher.gobHighlighted.toLowerCase();
			boolean result;
			if (searchKeyword.contains("||")) {
				String[] keywords = searchKeyword.split("\\|\\|"); // Updated split regex
				String finalResourceName = resourceName;
				result = Arrays.stream(keywords)
						.anyMatch(keyword -> finalResourceName.contains(keyword) && keyword.length() > 2);
			} else {
				result = resourceName.contains(searchKeyword) && searchKeyword.length() > 2;
			}

			setSearchOl(result);
		}
	}

	public void updateResPeekDependantHighlights(MessageBuf sdt) {
		updateContainerHighlight(sdt);
		updateLeathertubsHighlight(sdt);
		updateGardenPotHighlight(sdt);
	}

	private void updateOverlayDependantHighlights() {
		updateDryingFramesHighlight();
		updateCheeseRacksHighlight();
	}

	public void settingUpdateWorkstationStage() { // ND: Used to enable/disable showing the color stage through options window.
		updateDryingFramesHighlight();
		updateCheeseRacksHighlight();
		updateGardenPotHighlight();
		updateLeathertubsHighlight();
	}

	public void settingUpdateMiningSupports() { // ND: Used to enable/disable showing the color stage through options window.
		updateSupportOverlays();
	}

	private static final String[] CONTAINER_PATHS = {
			// ND: Each container might have different peekrbufs for each state. This needs to be checked for each new container, in each state (Empty & Closed || Empty & Open, Full & Closed || Full & Open).
			"gfx/terobjs/cupboard",
			"gfx/terobjs/chest",
			"gfx/terobjs/crate",
			"gfx/terobjs/largechest",
			"gfx/terobjs/coffer",
			"gfx/terobjs/exquisitechest",
			//"gfx/terobjs/wbasket", // ND: This one only has open/closed peekrbufs, no other fullness indicators lmao
			"gfx/terobjs/birchbasket",
			"gfx/terobjs/metalcabinet",
			"gfx/terobjs/stonecasket",
			"gfx/terobjs/bonechest",
			"gfx/terobjs/leatherbasket",
			"gfx/terobjs/woodbox",
			"gfx/terobjs/linencrate",
	};

	private void updateContainerHighlight(MessageBuf sdt) {
		if (getres() != null) {
			String resName = getres().name;
			if (Arrays.stream(CONTAINER_PATHS).anyMatch(resName::matches)) {
				setContainerHighlight(resName, sdt);
			}
		}
	}
	public void updateContainerHighlight() {
		if (getres() != null) {
			String resName = getres().name;
			if (Arrays.stream(CONTAINER_PATHS).anyMatch(resName::matches)) {
				Drawable dr = getattr(Drawable.class);
				ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
				if (d != null) {
					setContainerHighlight(resName, d.sdt);
				}
			}
		}
	}
	private void setContainerHighlight(String resName, MessageBuf sdt){
		int peekrbuf = sdt.peekrbuf(0);
		if (OptWnd.showContainerFullnessCheckBox.a) {
			switch (resName) {
				case "gfx/terobjs/cupboard":
				case "gfx/terobjs/chest":
				case "gfx/terobjs/exquisitechest":
					if (peekrbuf == 30 || peekrbuf == 29) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/crate":
				case "gfx/terobjs/linencrate":
					if (peekrbuf == 16) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/leatherbasket":
					if (peekrbuf == 4) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/woodbox":
					if (peekrbuf == 8) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/largechest":
				case "gfx/terobjs/birchbasket":
				case "gfx/terobjs/stonecasket":
				case "gfx/terobjs/bonechest":
					if (peekrbuf == 17 || peekrbuf == 18) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/coffer":
				case "gfx/terobjs/metalcabinet":
					if (peekrbuf == 65 || peekrbuf == 66) {
						if (OptWnd.showContainerFullnessRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
						else delattr(GobStateHighlight.class);
					}
					break;
				default:
					break;
			}
		} else {
			delattr(GobStateHighlight.class);
		}
	}

	private void updateLeathertubsHighlight(MessageBuf sdt) {
		if (getres() != null && Pattern.matches("gfx/terobjs/ttub", getres().name)) {
			if (OptWnd.showWorkstationStageCheckBox.a){
				setLeathertubsHighlight(sdt);
			} else {
				delattr(GobStateHighlight.class);
			}
		}
	}
	private void updateLeathertubsHighlight() {
		if (getres() != null && Pattern.matches("gfx/terobjs/ttub", getres().name)) {
			Drawable dr = getattr(Drawable.class);
			ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
			if (d != null) {
				if (OptWnd.showWorkstationStageCheckBox.a) {
					setLeathertubsHighlight(d.sdt);
				} else {
					delattr(GobStateHighlight.class);
				}
			}
		}
	}
	private void setLeathertubsHighlight(MessageBuf sdt){
			int peekrbuf = sdt.peekrbuf(0);
			if (peekrbuf == 0 || peekrbuf == 1 || peekrbuf == 4 || peekrbuf == 5) {
				if (OptWnd.showWorkstationStageGrayCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GRAY);
				else delattr(GobStateHighlight.class);
			} else if (peekrbuf == 10 || peekrbuf == 9 || peekrbuf == 8) {
				if (OptWnd.showWorkstationStageRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
				else delattr(GobStateHighlight.class);
			} else if (peekrbuf != 6) {
				if (OptWnd.showWorkstationStageGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
				else delattr(GobStateHighlight.class);
			} else {
				if (OptWnd.showWorkstationStageYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
				else delattr(GobStateHighlight.class);
			}
	}

	private void updateDryingFramesHighlight() {
		if (getres() != null && Pattern.matches("gfx/terobjs/dframe", getres().name)) {
			if (OptWnd.showWorkstationStageCheckBox.a){
				boolean done = true;
				boolean empty = true;
				for (Overlay ol : ols) {
					try {
						Indir<Resource> olires = ol.res;
						if (olires != null) {
							empty = false;
							Resource olres = olires.get();
							if (olres != null) {
								if (olres.name.endsWith("-blood") || olres.name.endsWith("-windweed") || olres.name.endsWith("-fishraw")) {
									done = false;
									break;
								}
							}
						}
					} catch (Loading l) {
					}
				}
				if (OptWnd.showWorkstationStageYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
				else delattr(GobStateHighlight.class);
				if (done && !empty) {
					if (OptWnd.showWorkstationStageRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
					else delattr(GobStateHighlight.class);
				} else if (empty) {
					if (OptWnd.showWorkstationStageGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
					else delattr(GobStateHighlight.class);
				}
			} else {
				delattr(GobStateHighlight.class);
			}
		}
	}

	private void updateCheeseRacksHighlight() {
		if (getres() != null && Pattern.matches("gfx/terobjs/cheeserack", getres().name)) {
			if (OptWnd.showWorkstationStageCheckBox.a) {
				if (ols.size() == 3) {
					if (OptWnd.showWorkstationStageRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
					else delattr(GobStateHighlight.class);
				} else if (ols.size() == 0) {
					if (OptWnd.showWorkstationStageGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
					else delattr(GobStateHighlight.class);
				} else {
					if (OptWnd.showWorkstationStageYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
					else delattr(GobStateHighlight.class);
				}
			} else {
				delattr(GobStateHighlight.class);
			}
		}
	}

	private void updateGardenPotHighlight(MessageBuf sdt) {
		if (getres() != null && Pattern.matches("gfx/terobjs/gardenpot", getres().name)){
			if (OptWnd.showWorkstationStageCheckBox.a) {
				setGardenPotHighlight(sdt);
			} else {
				delattr(GobStateHighlight.class);
			}
		}
	}
	private void updateGardenPotHighlight() {
		if (getres() != null && Pattern.matches("gfx/terobjs/gardenpot", getres().name)){
			Drawable dr = getattr(Drawable.class);
			ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
			if (d != null) {
				if (OptWnd.showWorkstationStageCheckBox.a) {
					setGardenPotHighlight(d.sdt);
				} else {
					delattr(GobStateHighlight.class);
				}
			}
		}
	}
	private void setGardenPotHighlight(MessageBuf sdt){
		int peekrbuf = sdt.peekrbuf(0);
		if (ols.size() == 2) {
			if (OptWnd.showWorkstationStageRedCheckBox.a) setGobStateHighlight(GobStateHighlight.State.RED);
			else delattr(GobStateHighlight.class);
		} else if (ols.size() == 1) {
			if (OptWnd.showWorkstationStageYellowCheckBox.a) setGobStateHighlight(GobStateHighlight.State.YELLOW);
			else delattr(GobStateHighlight.class);
		} else if (ols.size() == 0) {
			if (peekrbuf == 3) {
				if (OptWnd.showWorkstationStageGreenCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GREEN);
				else delattr(GobStateHighlight.class);
			} else { // (peekrbuf == 0 || peekrbuf == 1 || peekrbuf == 2)
				if (OptWnd.showWorkstationStageGrayCheckBox.a) setGobStateHighlight(GobStateHighlight.State.GRAY);
				else delattr(GobStateHighlight.class);
			}
		} else {
			delattr(GobStateHighlight.class);
		}
	}

	public void setHighlightedObjects(){
		if (listHighlighted.contains(id)) {
			setattr(new GobPermanentHighlight(this, GobPermanentHighlight.State.PURPLE));
		}
	}

	private void setGobStateHighlight(GobStateHighlight.State state) {
		GobStateHighlight current = getattr(GobStateHighlight.class);
		if (current != null) {
			current.state = state;
		} else  {
			if (GobStateHighlight.State.RED == state)
				setattr(new GobStateHighlight(this, GobStateHighlight.State.RED));
			else if (GobStateHighlight.State.GREEN == state)
				setattr(new GobStateHighlight(this, GobStateHighlight.State.GREEN));
			else if (GobStateHighlight.State.GRAY == state)
				setattr(new GobStateHighlight(this, GobStateHighlight.State.GRAY));
			else if (GobStateHighlight.State.YELLOW == state)
				setattr(new GobStateHighlight(this, GobStateHighlight.State.YELLOW));
			else {
				delattr(GobStateHighlight.class);
			}
		}
	}

	public boolean isFriend() {
		synchronized (glob.party.memb) {
			for (Party.Member m : glob.party.memb.values()) {
				if (m.gobid == id)
					return true;
			}
		}
		KinInfo kininfo = getattr(KinInfo.class);
		if (kininfo == null || !kininfo.isVillager() || kininfo.group == 2 /*red*/)
			return false;

		return true;
	}

	public boolean isPartyMember() {
		synchronized (glob.party.memb) {
			for (Party.Member m : glob.party.memb.values()) {
				if (m.gobid == id)
					return true;
			}
		}
		return false;
	}
	public boolean isPartyLeader (){
		synchronized (glob.party.memb) {
			for (Party.Member m : glob.party.memb.values()) {
				if (m == glob.party.leader)
					return true;
			}
		}
		return false;
	}

	public void playPlayerAlarm() {
		if (getres() != null) {
			if (isMannequin != null && isMannequin == false){
				if (getres().name.equals("gfx/borka/body")) {
					KinInfo kininfo = getattr(KinInfo.class);
					if (!isMe() && !alarmPlayed.contains(id)) {
						if (kininfo == null || (kininfo.unknown && !kininfo.isVillager()) || (kininfo.group == 0 && !kininfo.isVillager())) {
							playPlayerColorAlarm(OptWnd.whitePlayerAlarmEnabledCheckbox.a, OptWnd.whitePlayerAlarmFilename.buf.line(), OptWnd.whitePlayerAlarmVolumeSlider.val);
						} else if ((kininfo.unknown && kininfo.isVillager()) || (kininfo.group == 0 && kininfo.isVillager())) {
							playPlayerColorAlarm(OptWnd.whiteVillageOrRealmPlayerAlarmEnabledCheckbox.a, OptWnd.whiteVillageOrRealmPlayerAlarmFilename.buf.line(), OptWnd.whiteVillageOrRealmPlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 1) {
							playPlayerColorAlarm(OptWnd.greenPlayerAlarmEnabledCheckbox.a, OptWnd.greenPlayerAlarmFilename.buf.line(), OptWnd.greenPlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 2) {
							playPlayerColorAlarm(OptWnd.redPlayerAlarmEnabledCheckbox.a, OptWnd.redPlayerAlarmFilename.buf.line(), OptWnd.redPlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 3) {
							playPlayerColorAlarm(OptWnd.bluePlayerAlarmEnabledCheckbox.a, OptWnd.bluePlayerAlarmFilename.buf.line(), OptWnd.bluePlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 4) {
							playPlayerColorAlarm(OptWnd.tealPlayerAlarmEnabledCheckbox.a, OptWnd.tealPlayerAlarmFilename.buf.line(), OptWnd.tealPlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 5) {
							playPlayerColorAlarm(OptWnd.yellowPlayerAlarmEnabledCheckbox.a, OptWnd.yellowPlayerAlarmFilename.buf.line(), OptWnd.yellowPlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 6) {
							playPlayerColorAlarm(OptWnd.purplePlayerAlarmEnabledCheckbox.a, OptWnd.purplePlayerAlarmFilename.buf.line(), OptWnd.purplePlayerAlarmVolumeSlider.val);
						} else if (kininfo.group == 7) {
							playPlayerColorAlarm(OptWnd.orangePlayerAlarmEnabledCheckbox.a, OptWnd.orangePlayerAlarmFilename.buf.line(), OptWnd.orangePlayerAlarmVolumeSlider.val);
						}
					}
				}
			}
		}
	}

	public void playLoftarAlarm() {
		if (getres() != null) {
			if (isMannequin != null && isMannequin == false){
				if (getres().name.equals("gfx/borka/body")) {
					KinInfo kininfo = getattr(KinInfo.class);
					if (kininfo.name.equals("Big Daddy Loftar")){
						try {
							File file = new File("res/sfx/loftarAlarm.wav");
							if (file.exists()) {
								AudioInputStream in = AudioSystem.getAudioInputStream(file);
								AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
								AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
								Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
								((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
								alarmPlayed.add(id);
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
		}
	}

	private void playPlayerColorAlarm(Boolean enabled, String line, int val) {
		if (enabled) {
			try {
				File file = new File("Alarms/" + line + ".wav");
				if (file.exists()) {
					AudioInputStream in = AudioSystem.getAudioInputStream(file);
					AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
					AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
					Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
					((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, val / 50.0));
					alarmPlayed.add(id);
				}
			} catch (Exception ignored) {
			}
		}
	}

	public Boolean isMe() {
		if(isMe == null) {
			if(glob.sess.ui.gui == null || glob.sess.ui.gui.map == null || glob.sess.ui.gui.map.plgob < 0) {
				return null;
			} else {
				isMe = id == glob.sess.ui.gui.map.plgob;
			}
		}
		return isMe;
	}

	public void toggleMineLadderRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/ladder")){
				setRadiusOl(100F, AuraCircleSprite.darkgreen, MSRad.show);
			}
		}
	}

	public void toggleBeeSkepRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/beehive")){
				setRadiusOl(150F, AuraCircleSprite.yellow, OptWnd.showBeeSkepsRadiiCheckBox.a);
			}
		}
	}

	public void toggleTroughsRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/trough")){
				setRadiusOl(200F, AuraCircleSprite.orange, OptWnd.showFoodTroughsRadiiCheckBox.a);
			}
		}
	}

	public static final String[] CRITTERAURA_PATHS = {
			"gfx/kritter/bayshrimp/bayshrimp",
			"gfx/kritter/bogturtle/bogturtle",
			"gfx/kritter/brimstonebutterfly/brimstonebutterfly",
			"gfx/kritter/cavecentipede/cavecentipede",
			"gfx/kritter/cavemoth/cavemoth",
			"gfx/kritter/chicken/chick",
			"gfx/kritter/chicken/chicken", // ND: This seems to be the model for wild chickens, both hens and roosters.
			"gfx/kritter/chicken/hen", // ND: This might be pointless?
			"gfx/kritter/chicken/rooster", // ND: This might be pointless?
			"gfx/kritter/crab/crab",
			"gfx/kritter/dragonfly/dragonfly",
			"gfx/kritter/earthworm/earthworm",
			"gfx/kritter/firefly/firefly",
			"gfx/kritter/forestlizard/forestlizard",
			"gfx/kritter/forestsnail/forestsnail",
			"gfx/kritter/frog/frog",
			"gfx/kritter/grasshopper/grasshopper",
			"gfx/kritter/hedgehog/hedgehog",
			"gfx/kritter/irrbloss/irrbloss",
			"gfx/kritter/jellyfish/jellyfish",
			"gfx/kritter/ladybug/ladybug",
			"gfx/kritter/lobster/lobster",
			"gfx/kritter/magpie/magpie",
			"gfx/kritter/mallard/mallard", // ND: I haven't checked yet, but I assume it could be the same case as with the chickens
			"gfx/kritter/mallard/mallard-f", // ND: This might be pointless?
			"gfx/kritter/mallard/mallard-m", // ND: This might be pointless?
			"gfx/kritter/mole/mole",
			"gfx/kritter/monarchbutterfly/monarchbutterfly",
			"gfx/kritter/moonmoth/moonmoth",
			"gfx/kritter/opiumdragon/opiumdragon",
			"gfx/kritter/ptarmigan/ptarmigan",
			"gfx/kritter/quail/quail",
			"gfx/kritter/rat/rat",
			"gfx/kritter/rockdove/rockdove",
			"gfx/kritter/sandflea/sandflea",
			"gfx/kritter/seagull/seagull",
			"gfx/kritter/silkmoth/silkmoth",
			"gfx/kritter/springbumblebee/springbumblebee",
			"gfx/kritter/squirrel/squirrel",
			"gfx/kritter/stagbeetle/stagbeetle",
			"gfx/kritter/stalagoomba/stalagoomba",
			"gfx/kritter/toad/toad",
			"gfx/kritter/waterstrider/waterstrider",
			"gfx/kritter/woodgrouse/woodgrouse-f", // ND: Only female can be chased, males will fight you
			"gfx/kritter/woodworm/woodworm",
			"gfx/kritter/whirlingsnowflake/whirlingsnowflake",


			"gfx/terobjs/items/grub", // ND: lmao
			"gfx/terobjs/items/hoppedcow",
			"gfx/terobjs/items/mandrakespirited",
	};

	public static final String[] BEASTDANGER_PATHS = {
			"/bear",
			"/lynx",
			"/walrus",
			"/mammoth",
			"/troll",
			"/spermwhale",
			"/orca",
			"/moose",
			"/wolf",
			"/bat",
			"/goldeneagle",
			"/caveangler",
			"/boar",
			"/badger",
			"/wolverine",
			"/boreworn",
			"/greenooze",
			"/adder",
			"/caverat",
			"/wildgoat",
			"/rat/caverat",
			"/cavelouse/cavelouse"
	};


	private void initiateAnimalOverlays() {
			toggleBeastDangerRadii();
			toggleCritterAuras();
	}

	private void initiateSupportOverlays(){
		if (getres() != null && OptWnd.showMineSupportSafeTilesCheckBox.a) {
			String resourceName = getres().name;
			if (getres().name.equals("gfx/terobjs/map/naturalminesupport") ){
				setMiningOl(true, (float) a, 0);
			} else if (resourceName.equals("gfx/terobjs/ladder") || resourceName.equals("gfx/terobjs/minesupport") ){
				setMiningOl(true, (float) a, 1);
			} else if (resourceName.equals("gfx/terobjs/column")){
				setMiningOl(true, (float) a, 2);
			} else if (resourceName.equals("gfx/terobjs/minebeam")){
				setMiningOl(true, (float) a, 3);
			}
		}
	}

	private void updateSupportOverlays(){
		if (getres() != null) {
			if (getres().name.equals("gfx/terobjs/map/naturalminesupport") ){
				setMiningOl(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 0);
			} else if (getres().name.equals("gfx/terobjs/ladder") || getres().name.equals("gfx/terobjs/minesupport") ){
				setMiningOl(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 1);
			} else if (getres().name.equals("gfx/terobjs/column")){
				setMiningOl(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 2);
			} else if (getres().name.equals("gfx/terobjs/minebeam")){
				setMiningOl(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 3);
			}
		}
	}

	public void toggleBeastDangerRadii() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.startsWith("gfx/kritter")){
				if (knocked != null && knocked == false) {
					if (Arrays.stream(BEASTDANGER_PATHS).anyMatch(resourceName::endsWith)) {
						if (resourceName.endsWith("/bat")) {
							if (batsFearMe || batsLeaveMeAlone) {
								setDangerRadii(false);
							} else {
								setDangerRadii(OptWnd.toggleBeastDangerRadiiCheckBox.a);
							}
						} else {
							setDangerRadii(OptWnd.toggleBeastDangerRadiiCheckBox.a);
						}
					}
				} else if (knocked != null && knocked == true) {
					if (Arrays.stream(BEASTDANGER_PATHS).anyMatch(resourceName::endsWith)) {
						setDangerRadii(false);
					}
				}
				else if (isComposite) { // ND: Retarded workaround. Some of these stupid animals have no animation when STANDING STILL. They're not loading their fucking knocked status??? HOW? It's like they're not an instance of composite, ONLY when standing still.
					if (Arrays.stream(BEASTDANGER_PATHS).anyMatch(resourceName::endsWith)) {
						if (resourceName.endsWith("/bat")) {
							if (batsFearMe || batsLeaveMeAlone) {
								setDangerRadii(false);
							} else {
								setDangerRadii(OptWnd.toggleBeastDangerRadiiCheckBox.a);
							}
						} else {
							setDangerRadii(OptWnd.toggleBeastDangerRadiiCheckBox.a);
						}
					}
				}
			}
		}
	}

	public void toggleCritterAuras() {
		if (getres() != null) {
			String resourceName = getres().name;
				if (knocked != null && knocked == false) {
					if (Arrays.stream(CRITTERAURA_PATHS).anyMatch(resourceName::matches)) {
						setCritterAura(OptWnd.toggleCritterAurasCheckBox.a, false);
					} else if (resourceName.matches(".*(rabbit|bunny)$")) {
						setCritterAura(OptWnd.toggleCritterAurasCheckBox.a, true);
					}
				} else if (knocked != null && knocked == true) {
				if (Arrays.stream(CRITTERAURA_PATHS).anyMatch(resourceName::matches)) {
					setCritterAura(false, false);
				} else if (resourceName.matches(".*(rabbit|bunny)$")) {
					setCritterAura(false, true);
				}
			}
				else if (!isComposite) { //ND: This also works for critters that can't have a knocked status, like insects.
					if (Arrays.stream(CRITTERAURA_PATHS).anyMatch(resourceName::matches)) {
						setCritterAura(OptWnd.toggleCritterAurasCheckBox.a, false);
					} else if (resourceName.matches(".*(rabbit|bunny)$")) {
						setCritterAura(OptWnd.toggleCritterAurasCheckBox.a, true);
					}
				}
		}
	}

	public void toggleSpeedBuffAuras() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/boostspeed"))
				setCircleOl(AuraCircleSprite.speedbuffAuraColor, OptWnd.toggleSpeedBoostAurasCheckBox.a, 6f);
		}
	}


	public void setCritterAura(boolean on, boolean rabbit) {
		if (rabbit) {
			setCircleOl(AuraCircleSprite.rabbitAuraColor, on);
		} else {
			setCircleOl(AuraCircleSprite.genericCritterAuraColor, on);
		}
	}

	public void setDangerRadii(boolean on) {
		setRadiusOl(120F, AuraCircleSprite.redr, on);
	}

	private void setRadiusOl(float radius, Color col, boolean on) {
		if (on) {
			for (Overlay ol : ols) {
				if (ol.spr instanceof AnimalDangerRadiiSprite) {
					return;
				}
			}
			customRadiusOverlay = new Overlay(this, new AnimalDangerRadiiSprite(this, null, radius, col));
			synchronized (ols) {
				addol(customRadiusOverlay);
			}
		} else if (customRadiusOverlay != null) {
			removeOl(customRadiusOverlay);
			customRadiusOverlay = null;
		}
	}

	private void setSearchOl(boolean on) {
		if (on) {
			for (Overlay ol : ols) {
				if (ol.spr instanceof GobSearchHighlight) {
					return;
				}
			}
			customSearchOverlay = new Overlay(this, new GobSearchHighlight(this, null));
			synchronized (ols) {
				addol(customSearchOverlay);
			}
		} else if (customSearchOverlay != null) {
			removeOl(customSearchOverlay);
			customSearchOverlay = null;
		}
	}

	private void setCircleOl(Color col, boolean on) {
		if (on) {
			if (customRadiusOverlay != null) {
				removeOl(customRadiusOverlay);
				customRadiusOverlay = null;
			}
			customRadiusOverlay = new Overlay(this, new AuraCircleSprite(this, col));
			synchronized (ols) {
				addol(customRadiusOverlay);
			}
		} else if (customRadiusOverlay != null) {
			removeOl(customRadiusOverlay);
			customRadiusOverlay = null;
		}
	}
	private void setCircleOl(Color col, boolean on, float size) {
		if (on) {
//			for (Overlay ol : ols) {
//				if (ol.spr instanceof AuraCircleSprite) {
//					return;
//				}
//			}
			if (customRadiusOverlay != null) {
				removeOl(customRadiusOverlay);
				customRadiusOverlay = null;
			}
			customRadiusOverlay = new Overlay(this, new AuraCircleSprite(this, col, size));
			synchronized (ols) {
				addol(customRadiusOverlay);
			}
		} else if (customRadiusOverlay != null) {
			removeOl(customRadiusOverlay);
			customRadiusOverlay = null;
		}
	}

	public void setMiningOl(boolean on, float angle, int size) {
		if (on) {
			for (Overlay ol : ols) {
				if (ol.spr instanceof SupportSprite) {
					return;
				}
			}
			customOverlay = new Overlay(this, new SupportSprite(this, angle, size));
			synchronized (ols) {
				addol(customOverlay);
			}
		} else if (customOverlay != null) {
			removeOl(customOverlay);
			customOverlay = null;
		}
	}

	private void updateIcon() {
		if(getattr(GobIcon.class) == null) {
			GobIcon icon = CustomMapIcons.getIcon(this);
			if(icon != null) {
				setattr(icon);
			}
		}
	}

	private void archeryIndicator(int range, boolean imOnLand) {
		if (OptWnd.flatWorldCheckBox.a && imOnLand){
			if (this.archeryVector == null) {
				archeryVector = new Overlay(this, new ArcheryVectorSprite(this, range));
				synchronized (ols) {
					addol(archeryVector);
				}
			}
		}
		if (this.archeryRadius == null) {
			archeryRadius = new Overlay(this, new ArcheryRadiusSprite(this, range));
			synchronized (ols) {
				addol(archeryRadius);
			}
		}
	}

	public void knockedOrDeadPlayerSoundEfect(HashSet<String> poses){
		Gob hearthling = this;
		final Timer timer = new Timer(); // ND: Need to do this with a timer cause the knocked out birds get loaded a few miliseconds later. I hope 100 is enough to prevent any issues.
		timer.schedule(new TimerTask(){
			@Override
			public void run(){
				long now = System.currentTimeMillis();
				// ND: Should only allow this sound to play again after 45 seconds. If you loot someone, their body sometimes does the KO animation again.
				// So check if at least 45 seconds passed. Tt takes about 50ish seconds for a hearthling to get up after being knocked anyway. They can port or log out after 25-30ish seconds.
				if ((now - lastKnockSoundtime) > 45000) {
					boolean imDead = true;
					ArrayList<Map.Entry<Class<? extends GAttrib>, GAttrib>> gAttribs = new ArrayList<>(hearthling.attr.entrySet());
					for (int i = 0; i < gAttribs.size(); i++) {
						Map.Entry<Class<? extends GAttrib>, GAttrib> entry = gAttribs.get(i);
						GAttrib g = entry.getValue();
						if (g instanceof Drawable) {
							if (g instanceof Composite) {
								Composite c = (Composite) g;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) {
										if (item.res.res.get().basename().equals("knockchirp")) {
											imDead = false;
											break;
										}
									}
								}
							}
						}
					}
					if (poses.contains("knock") || poses.contains("drowned")) {
						if (!imDead) {
							File file = new File("res/sfx/PlayerKnockedOut.wav");
							if (file.exists()) {
								try {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} else {
							isDeadPlayer = true;
							File file = null;
							if (malePlayer) {
								file = new File("res/sfx/MalePlayerKilled.wav");
							} else if (femalePlayer) {
								file = new File("res/sfx/FemalePlayerKilled.wav");
							}
							if (file != null && file.exists() && somethingJustDied) {
								try {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						lastKnockSoundtime = now;
					}
				}
				timer.cancel();
			}
		}, 100);
	}

	public Set<String> getPoses() {
		Set<String> poses = new HashSet<>();
		if (this.isComposite) {
			try {
				if (this.getattr(Drawable.class) != null) {
					poses = new HashSet<>(((Composite) this.getattr(Drawable.class)).poses);

				}
			} catch (Exception ignored) { }
		}
		return poses;
	}

	public void checkIfPlayerIsDead(HashSet<String> poses){
		Gob hearthling = this;
		final Timer timer = new Timer(); // ND: Need to do this with a timer cause the knocked out birds get loaded a few miliseconds later. I hope 100 is enough to prevent any issues.
		timer.schedule(new TimerTask(){
			@Override
			public void run() {
				if (poses.contains("rigormortis")) {
					isDeadPlayer = true;
					return;
				}
				if (poses.contains("knock") || poses.contains("drowned")) {
					isDeadPlayer = true;
					for (GAttrib g : hearthling.attr.values()) {
						if (g instanceof Drawable) {
							if (g instanceof Composite) {
								Composite c = (Composite) g;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) {
										if (item.res.res.get().basename().equals("knockchirp")) {
											isDeadPlayer = false;
											break;
										}
									}
								}
							}
						}
					}
				}
				timer.cancel();
			}
		}, 100);
	}

	public void removePermanentHighlight(){
		if (Gob.listHighlighted.contains(id)) {
			Gob.listHighlighted.remove(id);
			delattr(GobPermanentHighlight.class);
		}
	}

	public void reloadTreeScale(){
		TreeScale treeScale = null;
		if (getres() != null) {
			if ((getres().name.startsWith("gfx/terobjs/trees") && !getres().name.endsWith("log") && !getres().name.endsWith("oldtrunk")) || getres().name.startsWith("gfx/terobjs/bushes")) {
				treeScale = getattr(TreeScale.class);
				if (treeScale != null) {
					float scale = treeScale.originalScale;
					delattr(TreeScale.class);
					setattr(new TreeScale(this, (OptWnd.treesAndBushesScaleSlider.val/100f) * scale, scale));
					drawableUpdated();
				}
			}
		}
	}

	public void reloadTreeSwaying(){
		GobSvaj gobSvaj = null;
		if (getres() != null) {
			if ((getres().name.startsWith("gfx/terobjs/trees") && !getres().name.endsWith("log") && !getres().name.endsWith("oldtrunk") && !getres().name.endsWith("trombonechantrelle") && !getres().name.endsWith("towercap")) || getres().name.startsWith("gfx/terobjs/bushes")) {
				gobSvaj = getattr(GobSvaj.class);
				if (gobSvaj != null && (OptWnd.disableTreeAndBushSwayingCheckBox.a)) {
					delattr(GobSvaj.class);
				} else if (!OptWnd.disableTreeAndBushSwayingCheckBox.a) {
					setattr(new GobSvaj(this));
				}
			}
		}
	}
}
