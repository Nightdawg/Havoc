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

import haven.MCache.OverlayInfo;
import haven.automated.MiningSafetyAssistant;
import haven.automated.helpers.AreaSelectCallback;
import haven.automated.pathfinder.PFListener;
import haven.automated.pathfinder.Pathfinder;
import haven.render.*;
import haven.render.sl.Type;
import haven.render.sl.Uniform;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class MapView extends PView implements DTarget, Console.Directory, PFListener {
    public static boolean clickdb = false;
    public long plgob = -1;
    public Coord2d cc;
    public final Glob glob;
    private int view = 2;
    private Collection<Delayed> delayed = new LinkedList<Delayed>();
	private Collection<DelayedB> delayedB = new LinkedList<DelayedB>();
    private Collection<Delayed> delayed2 = new LinkedList<Delayed>();
    public Camera camera = restorecam();
    private Loader.Future<Plob> placing = null;
    private Grabber grab;
    private Selector selection;
    private Coord3f camoff = new Coord3f(Coord3f.o);
    public double shake = 0.0;
    public static double plobpgran = Utils.getprefd("plobpgran", 8);
    public static double plobagran = Utils.getprefd("plobagran", 12);
    private static final Map<String, Class<? extends Camera>> camtypes = new HashMap<String, Class<? extends Camera>>();
	private static final int MAX_TILE_RANGE = 40;

	private long lastmmhittest = System.currentTimeMillis();
	private Coord lasthittestc = Coord.z;

	private AreaSelectCallback areaSelectCallback;
	public boolean areaSelect = false;

	public final PartyHighlight partyHighlight;
	public final PartyCircles partyCircles;

	public Pathfinder pf;
	public Thread pfthread;
	public CheckpointManager checkpointManager;
	public Thread checkpointManagerThread;


	public interface Delayed {
	public void run(GOut g);
    }

	public interface DelayedB {
		public void run();
	}

    public interface Grabber {
	boolean mmousedown(Coord mc, int button);
	boolean mmouseup(Coord mc, int button);
	boolean mmousewheel(Coord mc, int amount);
	void mmousemove(Coord mc);
    }

	private enum Direction {
		WEST, EAST, NORTH, SOUTH
	}

    public abstract class Camera implements Pipe.Op {
	protected haven.render.Camera view = new haven.render.Camera(Matrix4f.identity());
	protected Projection proj = new Projection(Matrix4f.identity());
	
	public Camera() {
	    resized();
	}

	public boolean keydown(KeyEvent ev) {
	    return(false);
	}

	public boolean click(Coord sc) {
	    return(false);
	}
	public void drag(Coord sc) {}
	public void release() {}
	public boolean wheel(Coord sc, int amount) {
	    return(false);
	}

	public void snap(Direction dir) {}
	
	public void resized() {
	    float field = 0.5f;
	    float aspect = ((float)sz.y) / ((float)sz.x);
	    proj = Projection.frustum(-field, field, -aspect * field, aspect * field, 1, 5000); // ND: Changed this from 2000 to 5000. This is the max free cam distance, before it turns black.
	}

	public void apply(Pipe p) {
	    proj.apply(p);
	    view.apply(p);
	}
	
	public abstract float angle();
	public abstract void tick(double dt);

	public String stats() {return("N/A");}
    }
    
    public class FollowCam extends Camera {
	private final float fr = 0.0f, h = 10.0f;
	private float ca, cd;
	private Coord3f curc = null;
	private float elev, telev;
	private float angl, tangl;
	private Coord dragorig = null;
	private float anglorig;
	
	public FollowCam() {
	    elev = telev = (float)Math.PI / 6.0f;
	    angl = tangl = 0.0f;
	}
	
	public void resized() {
	    ca = (float)sz.y / (float)sz.x;
	    cd = 400.0f * ca;
	}
	
	public boolean click(Coord c) {
	    anglorig = tangl;
	    dragorig = c;
	    return(true);
	}
	
	public void drag(Coord c) {
	    tangl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    tangl = tangl % ((float)Math.PI * 2.0f);
	}

	private double f0 = 0.2, f1 = 0.5, f2 = 0.9;
	private double fl = Math.sqrt(2);
	private double fa = ((fl * (f1 - f0)) - (f2 - f0)) / (fl - 2);
	private double fb = ((f2 - f0) - (2 * (f1 - f0))) / (fl - 2);
	private float field(float elev) {
	    double a = elev / (Math.PI / 4);
	    return((float)(f0 + (fa * a) + (fb * Math.sqrt(a))));
	}

	private float dist(float elev) {
	    float da = (float)Math.atan(ca * field(elev));
	    return((float)(((cd - (h / Math.tan(elev))) * Math.sin(elev - da) / Math.sin(da)) - (h / Math.sin(elev))));
	}

	public void tick(double dt) {
	    elev += (telev - elev) * (float)(1.0 - Math.pow(500, -dt));
	    if(Math.abs(telev - elev) < 0.0001)
		elev = telev;
	    
	    float dangl = tangl - angl;
	    while(dangl >  Math.PI) dangl -= (float)(2 * Math.PI);
	    while(dangl < -Math.PI) dangl += (float)(2 * Math.PI);
	    angl += dangl * (float)(1.0 - Math.pow(500, -dt));
	    if(Math.abs(tangl - angl) < 0.0001)
		angl = tangl;
	    
	    Coord3f cc = getcc().invy();
	    if(curc == null)
		curc = cc;
	    float dx = cc.x - curc.x, dy = cc.y - curc.y;
	    float dist = (float)Math.sqrt((dx * dx) + (dy * dy));
	    if(dist > 250) {
		curc = cc;
	    } else if(dist > fr) {
		Coord3f oc = curc;
		float pd = (float)Math.cos(elev) * dist(elev);
		Coord3f cambase = new Coord3f(curc.x + ((float)Math.cos(tangl) * pd), curc.y + ((float)Math.sin(tangl) * pd), 0.0f);
		float a = cc.xyangle(curc);
		float nx = cc.x + ((float)Math.cos(a) * fr), ny = cc.y + ((float)Math.sin(a) * fr);
		Coord3f tgtc = new Coord3f(nx, ny, cc.z);
		curc = curc.add(tgtc.sub(curc).mul((float)(1.0 - Math.pow(500, -dt))));
		if(curc.dist(tgtc) < 0.01)
		    curc = tgtc;
		tangl = curc.xyangle(cambase);
	    }
	    
	    float field = field(elev);
	    view = haven.render.Camera.pointed(curc.add(camoff).add(0.0f, 0.0f, h), dist(elev), elev, angl);
	    proj = Projection.frustum(-field, field, -ca * field, ca * field, 1, 2000);
	}

	public float angle() {
	    return(angl);
	}
	
	private static final float maxang = (float)(Math.PI / 2 - 0.1);
	private static final float mindist = 50.0f;
	public boolean wheel(Coord c, int amount) {
	    float fe = telev;
	    telev += amount * telev * 0.02f;
	    if(telev > maxang)
		telev = maxang;
	    if(dist(telev) < mindist)
		telev = fe;
	    return(true);
	}

	public String stats() {
	    return(String.format("%f %f %f", elev, dist(elev), field(elev)));
	}
    }
    static {camtypes.put("follow", FollowCam.class);}

    public class SimpleCam extends Camera {
	private float dist = 50.0f;
	private float elev = (float)Math.PI / 4.0f;
	private float angl = 0.0f;
	private Coord dragorig = null;
	private float elevorig, anglorig;

	public void tick(double dt) {
	    Coord3f cc = getcc().invy();
	    view = haven.render.Camera.pointed(cc.add(camoff).add(0.0f, 0.0f, 15f), dist, elev, angl);
	}
	
	public float angle() {
	    return(angl);
	}
	
	public boolean click(Coord c) {
	    elevorig = elev;
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}
	
	public void drag(Coord c) {
	    elev = elevorig - ((float)(c.y - dragorig.y) / 100.0f);
	    if(elev < 0.0f) elev = 0.0f;
	    if(elev > (Math.PI / 2.0)) elev = (float)Math.PI / 2.0f;
	    angl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    angl = angl % ((float)Math.PI * 2.0f);
	}

	public boolean wheel(Coord c, int amount) {
	    float d = dist + (amount * 25);
	    if(d < 5)
		d = 5;
	    dist = d;
	    return(true);
	}
    }
    static {camtypes.put("worse", SimpleCam.class);}

    public class FreeCam extends Camera {
	private float dist = 50.0f, tdist = dist;
	private float elev = (float)Math.PI / 4.0f, telev = elev;
	private float angl = 0.0f, tangl = angl;
	private Coord dragorig = null;
	private float elevorig, anglorig;
	private final float pi2 = (float)(Math.PI * 2);
	private Coord3f cc = null;

	public void tick(double dt) {
	    float cf = (1f - (float)Math.pow(500, -dt * 3));
	    angl = angl + ((tangl - angl) * cf);
	    while(angl > pi2) {angl -= pi2; tangl -= pi2; anglorig -= pi2;}
	    while(angl < 0)   {angl += pi2; tangl += pi2; anglorig += pi2;}
	    if(Math.abs(tangl - angl) < 0.0001) angl = tangl;

	    elev = elev + ((telev - elev) * cf);
	    if(Math.abs(telev - elev) < 0.0001) elev = telev;

	    dist = dist + ((tdist - dist) * cf);
	    if(Math.abs(tdist - dist) < 0.0001) dist = tdist;

	    Coord3f mc = getcc().invy();
	    if((cc == null) || (Math.hypot(mc.x - cc.x, mc.y - cc.y) > 250))
		cc = mc;
	    else
		cc = cc.add(mc.sub(cc).mul(cf));
	    view = haven.render.Camera.pointed(cc.add(0.0f, 0.0f, 15f), dist, elev, angl);
	}

	public float angle() {
	    return(angl);
	}

	public boolean click(Coord c) {
	    elevorig = elev;
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}

	public void drag(Coord c) {
	    telev = elevorig - ((float)(c.y - dragorig.y) / 100.0f);
	    if(telev < 0.0f) telev = 0.0f;
	    if(telev > (Math.PI / 2.0)) telev = (float)Math.PI / 2.0f;
	    tangl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	}

	public boolean wheel(Coord c, int amount) {
	    float d = tdist + (amount * 25);
	    if(d < 5)
		d = 5;
	    tdist = d;
	    return(true);
	}
    }
    static {camtypes.put("bad", FreeCam.class);}
    
    public class OrthoCam extends Camera {
	public boolean exact = true;
	protected float dist = 500.0f;
	protected float elev = (float)Math.PI / 6.0f;
	protected float angl = (float) (3 * Math.PI / 2);
	protected float field = (float)(200 * Math.sqrt(2));
	private Coord dragorig = null;
	private float anglorig;
	protected Coord3f cc, jc;

	public void tick2(double dt) {
	    this.cc = getcc().invy();
	}

	public void tick(double dt) {
	    tick2(dt);
	    float aspect = ((float)sz.y) / ((float)sz.x);
	    Matrix4f vm = haven.render.Camera.makepointed(new Matrix4f(), cc.add(camoff).add(0.0f, 0.0f, 15f), dist, elev, angl);
	    if(exact) {
		if(jc == null)
		    jc = cc;
		float pfac = rsz.x / (field * 2);
		Coord3f vjc = vm.mul4(jc).mul(pfac);
		Coord3f corr = new Coord3f(Math.round(vjc.x) - vjc.x, Math.round(vjc.y) - vjc.y, 0).div(pfac);
		if((Math.abs(vjc.x) > 500) || (Math.abs(vjc.y) > 500))
		    jc = null;
		vm = Location.makexlate(new Matrix4f(), corr).mul1(vm);
	    }
	    view = new haven.render.Camera(vm);
	    proj = Projection.ortho(-field, field, -field * aspect, field * aspect, 1, 5000);
	}

	public float angle() {
	    return(angl);
	}

	public boolean click(Coord c) {
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}

	public void drag(Coord c) {
	    angl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	    angl = angl % ((float)Math.PI * 2.0f);
	}

	public String stats() {
	    return(String.format("%.1f %.2f %.2f %.1f", dist, elev / Math.PI, angl / Math.PI, field));
	}
    }

    public static KeyBinding kb_camleft  = KeyBinding.get("cam-left",  KeyMatch.nil);
    public static KeyBinding kb_camright = KeyBinding.get("cam-right", KeyMatch.nil);
    public static KeyBinding kb_camin    = KeyBinding.get("cam-in",    KeyMatch.nil);
    public static KeyBinding kb_camout   = KeyBinding.get("cam-out",   KeyMatch.nil);
    public static KeyBinding kb_camreset = KeyBinding.get("cam-reset", KeyMatch.nil);
	public static KeyBinding kb_camSnapNorth = KeyBinding.get("cam-SnapNorth", KeyMatch.forcode(KeyEvent.VK_UP, 0));
	public static KeyBinding kb_camSnapSouth = KeyBinding.get("cam-SnapSouth", KeyMatch.forcode(KeyEvent.VK_DOWN, 0));
	public static KeyBinding kb_camSnapEast = KeyBinding.get("cam-SnapEast", KeyMatch.forcode(KeyEvent.VK_RIGHT, 0));
	public static KeyBinding kb_camSnapWest = KeyBinding.get("cam-SnapWest", KeyMatch.forcode(KeyEvent.VK_LEFT, 0));
    public class SOrthoCam extends OrthoCam {
	private Coord dragorig = null;
	private float anglorig;
	private float tangl = angl;
	private float tfield = field;
	private boolean isometric = true;
	private final float pi2 = (float)(Math.PI * 2);
	private double tf = 1.0;

	public SOrthoCam(String... args) {
	    PosixArgs opt = PosixArgs.getopt(args, "enift:Z:");
	    for(char c : opt.parsed()) {
		switch(c) {
		case 'e':
		    exact = true;
		    break;
		case 'n':
		    exact = false;
		    break;
		case 'i':
		    isometric = true;
		    break;
		case 'f':
		    isometric = false;
		    break;
		case 't':
		    tf = Double.parseDouble(opt.arg);
		    break;
		case 'Z':
		    field = tfield = Float.parseFloat(opt.arg);
		    break;
		}
	    }
	}

	public void tick2(double dt) {
	    dt *= tf;
	    float cf = 1f - (float)Math.pow(500, -dt);
	    Coord3f mc = getcc().invy();
	    if((cc == null) || (Math.hypot(mc.x - cc.x, mc.y - cc.y) > 250))
		cc = mc;
	    else if(!exact || (mc.dist(cc) > 2))
		cc = cc.add(mc.sub(cc).mul(cf));

	    angl = angl + ((tangl - angl) * cf);
	    while(angl > pi2) {angl -= pi2; tangl -= pi2; anglorig -= pi2;}
	    while(angl < 0)   {angl += pi2; tangl += pi2; anglorig += pi2;}
	    if(Math.abs(tangl - angl) < 0.001)
		angl = tangl;
	    else
		jc = cc;

	    field = field + ((tfield - field) * cf);
	    if(Math.abs(tfield - field) < 0.1)
		field = tfield;
	    else
		jc = cc;
	}

	public boolean click(Coord c) {
	    anglorig = angl;
	    dragorig = c;
	    return(true);
	}

	public void drag(Coord c) {
	    tangl = anglorig + ((float)(c.x - dragorig.x) / 100.0f);
	}

	public void release() {
	    if(isometric && (tfield > 100))
		tangl = (float)(Math.PI * 0.5 * (Math.floor(tangl / (Math.PI * 0.5)) + 0.5));
	}

	private void chfield(float nf) {
	    tfield = nf;
	    tfield = Math.max(Math.min(tfield, sz.x * (float)Math.sqrt(2) / 8f), 50);
	    if(tfield > 100)
		release();
	}

	public boolean wheel(Coord c, int amount) {
	    chfield(tfield + amount * 10);
	    return(true);
	}

	public boolean keydown(KeyEvent ev) {
	    if(kb_camleft.key().match(ev)) {
		tangl = (float)(Math.PI * 0.5 * (Math.floor((tangl / (Math.PI * 0.5)) - 0.51) + 0.5));
		return(true);
	    } else if(kb_camright.key().match(ev)) {
		tangl = (float)(Math.PI * 0.5 * (Math.floor((tangl / (Math.PI * 0.5)) + 0.51) + 0.5));
		return(true);
	    } else if(kb_camin.key().match(ev)) {
		chfield(tfield - 50);
		return(true);
	    } else if(kb_camout.key().match(ev)) {
		chfield(tfield + 50);
		return(true);
	    } else if(kb_camreset.key().match(ev)) {
		tangl = angl + (float)Utils.cangle(-(float)Math.PI * 0.25f - angl);
		chfield((float)(100 * Math.sqrt(2)));
		return(true);
	    }
	    return(false);
	}
    }
    static {camtypes.put("ortho", SOrthoCam.class);}

	public static int orthoCameraAxisReverter = 1;
	public static void NDrevertOrthoAxis(boolean reverted){
		if (reverted) orthoCameraAxisReverter = -1;
		else orthoCameraAxisReverter = 1;
	}
	public static int freeCamXAxisReverter = 1;
	public static void NDrevertfreeCamXAxis(boolean reverted){
		if (reverted) freeCamXAxisReverter = -1;
		else freeCamXAxisReverter = 1;
	}
	public static int freeCamYAxisReverter = 1;
	public static void NDrevertfreeCamYAxis(boolean reverted){
		if (reverted) freeCamYAxisReverter = -1;
		else freeCamYAxisReverter = 1;
	}
	public static boolean freeCamTiltBool = false;
	public static float cameraHeightDistance = 15f;
	public static int freeCameraZoomSpeed = 25;
	public static float publicFreeCamDist = 500.0f;
	public static int publicCurrentCameraName = 1;
	public static float freeCamAngle = (float)Math.PI / 4.0f;
	public class NDFreeCam extends FreeCam {
		private float dist = 500.0f, tdist = dist;
		private float elev = (float)Math.PI / 4.0f, telev = elev;
		private float angl = (float) (3 * Math.PI / 2), tangl = angl;
		private Coord dragorig = null;
		private float elevorig, anglorig;
		private final float pi2 = (float)(Math.PI * 2);
		private Coord3f cc = null;

		public void tick(double dt) {
			float cf = (1f - (float)Math.pow(500, -dt * 3));
			angl = angl + ((tangl - angl) * cf);
			while(angl > pi2) {angl -= pi2; tangl -= pi2; anglorig -= pi2;}
			while(angl < 0)   {angl += pi2; tangl += pi2; anglorig += pi2;}
			if(Math.abs(tangl - angl) < 0.0001) angl = tangl;

			elev = elev + ((telev - elev) * cf);
			if(Math.abs(telev - elev) < 0.0001) elev = telev;

			dist = dist + ((tdist - dist) * cf);
			if (dist > 4000) tdist = dist = 4000;
			if(Math.abs(tdist - dist) < 0.0001) dist = tdist;

			Coord3f mc = getcc();
			mc.y = -mc.y;
			if((cc == null) || (Math.hypot(mc.x - cc.x, mc.y - cc.y) > 250))
				cc = mc;
			else
				cc = cc.add(mc.sub(cc).mul(cf));
			view = haven.render.Camera.pointed(cc.add(0.0f, 0.0f, cameraHeightDistance), dist, elev, angl);
		}

		public float angle() {
			return(angl);
		}

		public boolean click(Coord c) {
			elevorig = elev;
			anglorig = angl;
			dragorig = c;
			return(true);
		}

		public void drag(Coord c) {
			telev = elevorig - freeCamYAxisReverter * ((float)(c.y - dragorig.y) / 100.0f);
			if (freeCamTiltBool){
				if(telev < -0.5f) telev = -0.5f;
			}
			else {
				if(telev < 0.0f) telev = 0.0f;
			}
			if(telev > (Math.PI / 2.0)) telev = (float)Math.PI / 2.0f;
			freeCamAngle = telev;
			tangl = anglorig + freeCamXAxisReverter * ((float)(c.x - dragorig.x) / 100.0f);
		}

		public boolean wheel(Coord c, int amount) {
			float d = tdist + (amount * freeCameraZoomSpeed);
			if(d < 10)
				d = 10;
			tdist = d;
			publicFreeCamDist = d;
			return(true);
		}

		@Override
		public void snap(Direction dir) {
			switch (dir) {
				case WEST:
					tangl = (float) (2 * Math.PI);
					break;
				case EAST:
					tangl = (float) Math.PI;
					break;
				case NORTH:
					tangl = (float) (3 * Math.PI / 2);
					break;
				case SOUTH:
					tangl = (float) (Math.PI / 2);
					break;
			}
		}

		public boolean keydown(KeyEvent ev) {
			if(kb_camSnapNorth.key().match(ev)) {
				snapCameraNorth();
				return(true);
			} else if(kb_camSnapSouth.key().match(ev)) {
				snapCameraSouth();
				return(true);
			} else if(kb_camSnapEast.key().match(ev)) {
				snapCameraEast();
				return(true);
			} else if(kb_camSnapWest.key().match(ev)) {
				snapCameraWest();
				return (true);
			}
			return(false);
		}
	}
	static {camtypes.put("NDFree", NDFreeCam.class);}

	public static Boolean isometricNDOrtho = true;
	public static int orthoCameraZoomSpeed = 10;
	public static float publicOrthoCamDist = 150f;

	public class NDSOrthoCam extends SOrthoCam {
		private Coord dragorig = null;
		private float anglorig;
		private float tangl = angl;
		private float tfield = field;
		//private boolean isometric = false;
		private final float pi2 = (float)(Math.PI * 2);
		private double tf = 2.0;

		public NDSOrthoCam(String... args) {
			PosixArgs opt = PosixArgs.getopt(args, "enift:Z:");
			for(char c : opt.parsed()) {
				switch(c) {
					case 'e':
						exact = true;
						break;
					case 'n':
						exact = false;
						break;
					case 'i':
						//isometric = true;
						break;
					case 'f':
						//isometric = false;
						break;
					case 't':
						tf = Double.parseDouble(opt.arg);
						break;
					case 'Z':
						field = tfield = Float.parseFloat(opt.arg);
						break;
				}
			}
		}

		public void tick2(double dt) {
			dt *= tf;
			float cf = 1f - (float)Math.pow(500, -dt);
			Coord3f mc = getcc();
			mc.y = -mc.y;
			if((cc == null) || (Math.hypot(mc.x - cc.x, mc.y - cc.y) > 250))
				cc = mc;
			else if(!exact || (mc.dist(cc) > 2))
				cc = cc.add(mc.sub(cc).mul(cf));

			angl = angl + ((tangl - angl) * cf);
			while(angl > pi2) {angl -= pi2; tangl -= pi2; anglorig -= pi2;}
			while(angl < 0)   {angl += pi2; tangl += pi2; anglorig += pi2;}
			if(Math.abs(tangl - angl) < 0.001)
				angl = tangl;
			else
				jc = cc;

			field = field + ((tfield - field) * cf);
			if(Math.abs(tfield - field) < 0.1)
				field = tfield;
			else
				jc = cc;
		}

		public boolean click(Coord c) {
			anglorig = angl;
			dragorig = c;
			return(true);
		}

		public void drag(Coord c) {
			tangl = anglorig + orthoCameraAxisReverter * ((float)(c.x - dragorig.x) / 100.0f);
		}

		public void release() {
			if(isometricNDOrtho && (tfield > 100))
				tangl = (float)(Math.PI * 0.5 * (Math.floor(tangl / (Math.PI * 0.5)) + 0.5));
		}

		private void chfield(float nf) {
			tfield = nf;
			tfield = Math.max(Math.min(tfield, sz.x * (float)Math.sqrt(2) / 2f), 50); // ND: changed "8f" to "2f" to increase the zoom out distance limit
			if(tfield > 100)
				release();
			publicOrthoCamDist = tfield;
		}

		public boolean wheel(Coord c, int amount) {
			chfield(tfield + amount * orthoCameraZoomSpeed);
			return(true);
		}

		@Override
		public void snap(Direction dir) {
			switch (dir) {
				case WEST:
					tangl = (float) (2 * Math.PI);
					break;
				case EAST:
					tangl = (float) Math.PI;
					break;
				case NORTH:
					tangl = (float) (3 * Math.PI / 2);
					break;
				case SOUTH:
					tangl = (float) (Math.PI / 2);
					break;
			}
		}

		public boolean keydown(KeyEvent ev) {
			if(kb_camSnapNorth.key().match(ev)) {
				snapCameraNorth();
				return(true);
			} else if(kb_camSnapSouth.key().match(ev)) {
				snapCameraSouth();
				return(true);
			} else if(kb_camSnapEast.key().match(ev)) {
				snapCameraEast();
				return(true);
			} else if(kb_camSnapWest.key().match(ev)) {
				snapCameraWest();
				return (true);
			}
			return(false);
		}
	}
	static {camtypes.put("NDOrtho", NDSOrthoCam.class);}

    @RName("mapview")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Coord sz = UI.scale((Coord)args[0]);
	    Coord2d mc = ((Coord)args[1]).mul(posres);
	    long pgob = -1;
	    if(args.length > 2)
		pgob = Utils.uint32((Integer)args[2]);
	    return(new MapView(sz, ui.sess.glob, mc, pgob));
	}
    }
    
    public MapView(Coord sz, Glob glob, Coord2d cc, long plgob) {
	super(sz);
	this.glob = glob;
	this.cc = cc;
	this.plgob = plgob;
	glob.oc.getgob(plgob).delattr(KinInfo.class); // ND: This is only needed for Valhalla. That's the only time plgob seems to change while you're playing. I guess it will work anytime anyway.
	basic.add(new Outlines(false));
	basic.add(this.gobs = new Gobs());
	basic.add(this.terrain = new Terrain());
	this.clickmap = new ClickMap();
	clmaptree.add(clickmap);
	setcanfocus(true);
	if (Gob.showCollisionBoxes) updatePlobDrawable();
	if (Gob.hideObjects) updatePlobDrawable();
	this.partyHighlight = new PartyHighlight(glob.party, plgob);
	this.partyCircles = new PartyCircles(glob.party, plgob);
    }

	public void updatePlobDrawable() {
		if(placing != null && placing.done()) {
			placing.get().drawableUpdated();
		}
	}
    
    protected void envdispose() {
	if(smap != null) {
	    smap.dispose(); smap = null;
	    slist.dispose(); slist = null;
	}
	super.envdispose();
    }

    public void dispose() {
	gobs.slot.remove();
	clmaplist.dispose();
	clobjlist.dispose();
	super.dispose();
    }

    public boolean visol(String tag) {
	synchronized(oltags) {
	    return(oltags.containsKey(tag));
	}
    }

    public void enol(String tag) {
	synchronized(oltags) {
	    oltags.put(tag, oltags.getOrDefault(tag, 0) + 1);
	}
    }

    public void disol(String tag) {
	synchronized(oltags) {
	    Integer rc = oltags.get(tag);
	    if((rc != null) && (--rc > 0))
		oltags.put(tag, rc);
	    else
		oltags.remove(tag);;
	}
    }

    private final Gobs gobs;
    private class Gobs implements RenderTree.Node, OCache.ChangeCallback {
	final OCache oc = glob.oc;
	final Map<Gob, Loader.Future<?>> adding = new HashMap<>();
	final Map<Gob, RenderTree.Slot> current = new HashMap<>();
	RenderTree.Slot slot;

	private void addgob(Gob ob) {
	    RenderTree.Slot slot = this.slot;
	    if(slot == null)
		return;
	    synchronized(ob) {
		synchronized(this) {
		    if(!adding.containsKey(ob))
			return;
		}
		RenderTree.Slot nslot;
		try {
		    nslot = slot.add(ob.placed);
		} catch(RenderTree.SlotRemoved e) {
		    /* Ignore here as there is a harmless remove-race
		     * on disposal. */
		    return;
		}
		synchronized(this) {
		    if(adding.remove(ob) != null)
			current.put(ob, nslot);
		    else
			nslot.remove();
		}
	    }
	}

	public void added(RenderTree.Slot slot) {
	    synchronized(this) {
		if(this.slot != null)
		    throw(new RuntimeException());
		this.slot = slot;
		synchronized(oc) {
		    for(Gob ob : oc)
			adding.put(ob, glob.loader.defer(() -> addgob(ob), null));
		    oc.callback(this);
		}
	    }
	}

	public void removed(RenderTree.Slot slot) {
	    synchronized(this) {
		if(this.slot != slot)
		    throw(new RuntimeException());
		this.slot = null;
		oc.uncallback(this);
		Collection<Loader.Future<?>> tasks = new ArrayList<>(adding.values());
		adding.clear();
		for(Loader.Future<?> task : tasks)
		    task.restart();
		current.clear();
	    }
	}

	public void added(Gob ob) {
	    synchronized(this) {
		if(current.containsKey(ob))
		    throw(new RuntimeException());
		adding.put(ob, glob.loader.defer(() -> addgob(ob), null));
	    }
	}

	public void removed(Gob ob) {
	    RenderTree.Slot slot;
	    synchronized(this) {
		slot = current.remove(ob);
		if(slot == null) {
		    Loader.Future<?> t = adding.remove(ob);
		    if(t != null)
			t.restart();
		}
	    }
	    if(slot != null) {
		try {
		    slot.remove();
		} catch(RenderTree.SlotRemoved e) {
		    /* Ignore here as there is a harmless remove-race
		     * on disposal. */
		}
	    }
	}

	public Loading loading() {
	    synchronized(this) {
		if(adding.isEmpty())
		    return(null);
		for(Loader.Future<?> t : adding.values()) {
		    Loading l = t.lastload();
		    if(l != null)
			return(l);
		}
	    }
	    return(new Loading("Loading objects..."));
	}
    }

    private class MapRaster extends RenderTree.Node.Track1 {
	final MCache map = glob.map;
	Area area;
	Loading lastload = new Loading("Initializing map...");

	abstract class Grid<T> extends RenderTree.Node.Track1 {
	    final Map<Coord, Pair<T, RenderTree.Slot>> cuts = new HashMap<>();
	    final boolean position;
	    Loading lastload = new Loading("Initializing map...");

	    Grid(boolean position) {
		this.position = position;
	    }

	    Grid() {this(true);}

	    abstract T getcut(Coord cc);
	    RenderTree.Node produce(T cut) {return((RenderTree.Node)cut);}

	    void tick() {
		if(slot == null)
		    return;
		Loading curload = null;
		for(Coord cc : area) {
		    try {
			T cut = getcut(cc);
			Pair<T, RenderTree.Slot> cur = cuts.get(cc);
			if((cur == null) || (cur.a != cut)) {
			    Coord2d pc = cc.mul(MCache.cutsz).mul(tilesz);
			    RenderTree.Node draw = produce(cut);
			    Pipe.Op cs = null;
			    if(position)
				cs = Location.xlate(new Coord3f((float)pc.x, -(float)pc.y, 0));
			    cuts.put(cc, new Pair<>(cut, slot.add(draw, cs)));
			    if(cur != null)
				cur.b.remove();
			}
		    } catch(Loading l) {
			l.boostprio(5);
			curload = l;
		    }
		}
		this.lastload = curload;
		for(Iterator<Map.Entry<Coord, Pair<T, RenderTree.Slot>>> i = cuts.entrySet().iterator(); i.hasNext();) {
		    Map.Entry<Coord, Pair<T, RenderTree.Slot>> ent = i.next();
		    if(!area.contains(ent.getKey())) {
			ent.getValue().b.remove();
			i.remove();
		    }
		}
	    }

	    public void removed(RenderTree.Slot slot) {
		super.removed(slot);
		cuts.clear();
	    }
	}

	void tick() {
	    /* XXX: Should be taken out of the main rendering
	     * loop. Probably not a big deal, but still. */
	    try {
		Coord cc = new Coord2d(getcc()).floor(tilesz).div(MCache.cutsz);
		area = new Area(cc.sub(view, view), cc.add(view, view).add(1, 1));
		lastload = null;
	    } catch(Loading l) {
		l.boostprio(5);
		lastload = l;
	    }
	}

	public Loading loading() {
	    if(this.lastload != null)
		return(this.lastload);
	    return(null);
	}
    }

    public final Terrain terrain;
    public class Terrain extends MapRaster {
	final Grid main = new Grid<MapMesh>() {
		MapMesh getcut(Coord cc) {
		    return(map.getcut(cc));
		}
	    };
	final Grid flavobjs = new Grid<RenderTree.Node>(false) {
		RenderTree.Node getcut(Coord cc) {
		    return(map.getfo(cc));
		}
	    };

	private Terrain() {
	}

	void tick() {
	    super.tick();
	    if(area != null) {
		main.tick();
		flavobjs.tick();
	    }
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(main);
	    slot.add(flavobjs);
	    super.added(slot);
	}

	public Loading loading() {
	    Loading ret = super.loading();
	    if(ret != null)
		return(ret);
	    if((ret = main.lastload) != null)
		return(ret);
	    if((ret = flavobjs.lastload) != null)
		return(ret);
	    return(null);
	}
    }

    public class Overlay extends MapRaster {
	final OverlayInfo id;
	int rc = 0;
	boolean used;

	final Grid base = new Grid<RenderTree.Node>() {
		RenderTree.Node getcut(Coord cc) {
		    return(map.getolcut(id, cc));
		}
	    };
	final Grid outl = new Grid<RenderTree.Node>() {
		RenderTree.Node getcut(Coord cc) {
		    return(map.getololcut(id, cc));
		}
	    };

	private Overlay(OverlayInfo id) {
	    this.id = id;
	}

	void tick() {
	    super.tick();
	    if(area != null) {
		base.tick();
		outl.tick();
	    }
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(base, id.mat());
	    Material omat = id.omat();
	    if(omat != null)
		slot.add(outl, omat);
	    super.added(slot);
	}

	public Loading loading() {
	    Loading ret = super.loading();
	    if(ret != null)
		return(ret);
	    if((ret = base.lastload) != null)
		return(ret);
	    return(null);
	}

	public void remove() {
	    slot.remove();
	}
    }

    private final Map<String, Integer> oltags = new HashMap<>();
    private final Map<OverlayInfo, Overlay> ols = new HashMap<>();
    {oltags.put("show", 1);}
    private void oltick() {
	try {
	    for(Overlay ol : ols.values())
		ol.used = false;
	    if(terrain.area != null) {
		for(OverlayInfo id : glob.map.getols(terrain.area.mul(MCache.cutsz))) {
		    boolean vis = false;
		    synchronized(oltags) {
			for(String tag : id.tags()) {
			    if(oltags.containsKey(tag)) {
				vis = true;
				break;
			    }
			}
		    }
		    if(vis) {
			Overlay ol = ols.get(id);
			if(ol == null) {
			    try {
				basic.add(ol = new Overlay(id));
				ols.put(id, ol);
			    } catch(Loading l) {
				l.boostprio(2);
				continue;
			    }
			}
			ol.used = true;
		    }
		}
	    }
	    for(Iterator<Overlay> i = ols.values().iterator(); i.hasNext();) {
		Overlay ol = i.next();
		if(!ol.used) {
		    ol.remove();
		    i.remove();
		}
	    }
	} catch(Loading l) {
	    l.boostprio(2);
	}
	for(Overlay ol : ols.values())
	    ol.tick();
    }

    private static final Material gridmat = new Material(new BaseColor(255, 255, 255, 48), States.maskdepth, new MapMesh.OLOrder(null),
							 Location.xlate(new Coord3f(0, 0, 0.5f))   /* Apparently, there is no depth bias for lines. :P */
							 );
    private class GridLines extends MapRaster {
	final Grid grid = new Grid<RenderTree.Node>() {
		RenderTree.Node getcut(Coord cc) {
		    return(map.getcut(cc).grid());
		}
	    };

	private GridLines() {}

	void tick() {
	    super.tick();
	    if(area != null)
		grid.tick();
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(gridmat);
	    slot.add(grid);
	    super.added(slot);
	}

	public void remove() {
	    slot.remove();
	}
    }

    GridLines gridlines = null;
    public void showgrid(boolean show) {
	if((gridlines == null) && show) {
	    basic.add(gridlines = new GridLines());
	} else if((gridlines != null) && !show) {
	    gridlines.remove();
	    gridlines = null;
	}
    }

    static class MapClick extends Clickable {
	final MapMesh cut;

	MapClick(MapMesh cut) {
	    this.cut = cut;
	}

	public String toString() {
	    return(String.format("#<mapclick %s>", cut));
	}
    }

    private final ClickMap clickmap;
    private class ClickMap extends MapRaster {
	final Grid grid = new Grid<MapMesh>() {
		MapMesh getcut(Coord cc) {
		    return(map.getcut(cc));
		}
		RenderTree.Node produce(MapMesh cut) {
		    return(new MapClick(cut).apply(cut.flat));
		}
	    };

	void tick() {
	    super.tick();
	    if(area != null) {
		grid.tick();
	    }
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(grid);
	    super.added(slot);
	}

	public Loading loading() {
	    Loading ret = super.loading();
	    if(ret != null)
		return(ret);
	    if((ret = grid.lastload) != null)
		return(ret);
	    return(null);
	}
    }

    public String camstats() {
	String cc;
	try {
	    Coord3f c = getcc();
	    cc = String.format("(%.1f %.1f %.1f)", c.x / tilesz.x, c.y / tilesz.y, c.z / tilesz.x);
	} catch(Loading l) {
	    cc = "<nil>";
	}
	return(String.format("C: %s, Cam: %s", cc, camera.stats()));
    }

    public String stats() {
	String ret = String.format("Tree %s", tree.stats());
	if(back != null)
	    ret = String.format("%s, Inst %s, Draw %s", ret, instancer.stats(), back.stats());
	return(ret);
    }

    private Coord3f smapcc = null;
    private ShadowMap.ShadowList slist = null;
    private ShadowMap smap = null;
    private double lsmch = 0;
    private void updsmap(DirLight light) {
	boolean usesdw = ui.gprefs.lshadow.val;
	int sdwres = ui.gprefs.shadowres.val;
	sdwres = (sdwres < 0) ? (2048 >> -sdwres) : (2048 << sdwres);
	if(usesdw) {
	    Coord3f dir, cc;
	    try {
		dir = new Coord3f(-light.dir[0], -light.dir[1], -light.dir[2]);
		cc = getcc().invy();
	    } catch(Loading l) {
		return;
	    }
	    if(smap == null) {
		if(instancer == null)
		    return;
		slist = new ShadowMap.ShadowList(instancer);
		smap = new ShadowMap(new Coord(sdwres, sdwres), 750, 5000, 1);
	    } else if(smap.lbuf.w != sdwres) {
		smap.dispose();
		smap = new ShadowMap(new Coord(sdwres, sdwres), 750, 5000, 1);
		smapcc = null;
		basic(ShadowMap.class, null);
	    }
	    smap = smap.light(light);
	    boolean ch = false;
	    double now = Utils.rtime();
	    if((smapcc == null) || (smapcc.dist(cc) > 50)) {
		smapcc = cc;
		ch = true;
	    } else {
		if(now - lsmch > 0.1)
		    ch = true;
	    }
	    if(ch || !smap.haspos()) {
		smap = smap.setpos(smapcc.add(dir.neg().mul(1000f)), dir);
		lsmch = now;
	    }
	    basic(ShadowMap.class, smap);
	} else {
	    if(smap != null) {
		instancer.remove(slist);
		smap.dispose(); smap = null;
		slist.dispose(); slist = null;
		basic(ShadowMap.class, null);
	    }
	    smapcc = null;
	}
    }

    private void drawsmap(Render out) {
	if(smap != null)
	    smap.update(out, slist);
    }

    public DirLight amblight = null;
    private RenderTree.Slot s_amblight = null;
    private void amblight() {
	synchronized(glob) {
	    if(glob.lightamb != null) {
		//amblight = new DirLight(glob.lightamb, glob.lightdif, glob.lightspc, Coord3f.o.sadd((float)glob.lightelev, (float)glob.lightang, 1f));
		amblight = new DirLight(glob.blightamb, glob.blightdif, glob.blightspc, Coord3f.o.sadd((float)glob.lightelev, (float)glob.lightang, 1f));// ND: change for night mode
		amblight.prio(100);
	    } else {
		amblight = null;
	    }
	}
	if(s_amblight != null) {
	    s_amblight.remove();
	    s_amblight = null;
	}
	if(amblight != null)
	    s_amblight = basic.add(amblight);
    }

    public static class LightCompiler {
	public final GSettings gprefs;
	private final Lighting.LightGrid zgrid;
	private final int maxlights;

	public LightCompiler(GSettings gprefs) {
	    this.gprefs = gprefs;
	    if(gprefs == null) {
		zgrid = null;
		maxlights = 0;
	    } else {
		maxlights = gprefs.maxlights.val;
		if(gprefs.lightmode.val == GSettings.LightMode.ZONED) {
		    zgrid = new Lighting.LightGrid(64, 64, 64);
		    if(maxlights != 0)
			zgrid.maxlights = maxlights;
		} else {
		    zgrid = null;
		}
	    }
	}

	public boolean valid(GSettings prefs) {
	    return((prefs == gprefs) ||
		   (((prefs == null) == (gprefs == null)) &&
		    (prefs.lightmode.val == gprefs.lightmode.val) &&
		    (prefs.maxlights.val == gprefs.maxlights.val)));
	}

	public Pipe.Op compile(Object[][] params, Projection proj) {
	    if(zgrid == null) {
		Lighting.SimpleLights ret = new Lighting.SimpleLights(params);
		if(maxlights != 0)
		    ret.maxlights = maxlights;
		return(ret);
	    } else {
		return(zgrid.compile(params, proj));
	    }
	}
    }

    private LightCompiler lighting;
    protected void lights() {
	GSettings gprefs = basic.state().get(GSettings.slot);
	if((lighting == null) || !lighting.valid(gprefs)) {
	    basic(Light.class, null);
	    lighting = new LightCompiler(gprefs);
	}
	Projection proj = (camera == null) ? new Projection(Matrix4f.id) : camera.proj;
	basic(Light.class, Pipe.Op.compose(lights, lighting.compile(lights.params(), proj)));
    }

    public static final Uniform amblight_idx = new Uniform(Type.INT, p -> {
	    DirLight light = ((MapView)((WidgetContext)p.get(RenderContext.slot)).widget()).amblight;
	    Light.LightList lights = p.get(Light.lights);
	    int idx = -1;
	    if(light != null)
		idx = lights.index(light);
	    return(idx);
	}, RenderContext.slot, Light.lights);

    private final Map<RenderTree.Node, RenderTree.Slot> rweather = new HashMap<>();
    private void updweather() {
	Glob.Weather[] wls = glob.weather().toArray(new Glob.Weather[0]);
	Pipe.Op[] wst = new Pipe.Op[wls.length];
	for(int i = 0; i < wls.length; i++)
	    wst[i] = wls[i].state();
	try {
	    basic(Glob.Weather.class, Pipe.Op.compose(wst));
	} catch(Loading l) {
	}
	Collection<RenderTree.Node> old =new ArrayList<>(rweather.keySet());
	for(Glob.Weather w : wls) {
	    if(w instanceof RenderTree.Node) {
		RenderTree.Node n = (RenderTree.Node)w;
		old.remove(n);
		if(rweather.get(n) == null) {
		    try {
			rweather.put(n, basic.add(n));
		    } catch(Loading l) {
		    }
		}
	    }
	}
	for(RenderTree.Node rem : old)
	    rweather.remove(rem).remove();
    }

    public RenderTree.Slot drawadd(RenderTree.Node extra) {
	return(basic.add(extra));
    }

    public Gob player() {
	return((plgob < 0) ? null : glob.oc.getgob(plgob));
    }
    
    public Coord3f getcc() {
	Gob pl = player();
	if(pl != null)
	    return(pl.getc());
	else
	    return(glob.map.getzp(cc));
    }

    public static class Clicklist implements RenderList<Rendered>, RenderList.Adapter {
	public static final Pipe.Op clickbasic = Pipe.Op.compose(new States.Depthtest(States.Depthtest.Test.LE),
								 new States.Facecull(),
								 Homo3D.state);
	private static final int MAXID = 0xffffff;
	private final RenderList.Adapter master;
	private final boolean doinst;
	private final ProxyPipe basic = new ProxyPipe();
	private final Map<Slot<? extends Rendered>, Clickslot> slots = new HashMap<>();
	private final Map<Integer, Clickslot> idmap = new HashMap<>();
	private DefPipe curbasic = null;
	private RenderList<Rendered> back;
	private DrawList draw;
	private InstanceList instancer;
	private int nextid = 1;

	public class Clickslot implements Slot<Rendered> {
	    public final Slot<? extends Rendered> bk;
	    public final int id;
	    final Pipe idp;
	    private GroupPipe state;

	    public Clickslot(Slot<? extends Rendered> bk, int id) {
		this.bk = bk;
		this.id = id;
		this.idp = new SinglePipe<>(FragID.id, new FragID.ID(id));
	    }

	    public Rendered obj() {
		return(bk.obj());
	    }

	    public GroupPipe state() {
		if(state == null)
		    state = new IDState(bk.state());
		return(state);
	    }

	    private class IDState implements GroupPipe {
		static final int idx_bas = 0, idx_idp = 1, idx_back = 2;
		final GroupPipe back;

		IDState(GroupPipe back) {
		    this.back = back;
		}

		public Pipe group(int idx) {
		    switch(idx) {
		    case idx_bas: return(basic);
		    case idx_idp: return(idp);
		    default: return(back.group(idx - idx_back));
		    }
		}

		public int gstate(int id) {
		    if(id == FragID.id.id)
			return(idx_idp);
		    if(State.Slot.byid(id).type == State.Slot.Type.GEOM) {
			int ret = back.gstate(id);
			if(ret >= 0)
			    return(ret + idx_back);
		    }
		    if((id < curbasic.mask.length) && curbasic.mask[id])
			return(idx_bas);
		    return(-1);
		}

		public int nstates() {
		    return(Math.max(Math.max(back.nstates(), curbasic.mask.length), FragID.id.id + 1));
		}
	    }
	}

	public Clicklist(RenderList.Adapter master, boolean doinst) {
	    this.master = master;
	    this.doinst = doinst;
	    asyncadd(this.master, Rendered.class);
	}

	public void add(Slot<? extends Rendered> slot) {
	    if(slot.state().get(Clickable.slot) == null)
		return;
	    int id;
	    while(idmap.get(id = nextid) != null) {
		if(++nextid > MAXID)
		    nextid = 1;
	    }
	    Clickslot ns = new Clickslot(slot, id);
	    if(back != null)
		back.add(ns);
	    if(((slots.put(slot, ns)) != null) || (idmap.put(id, ns) != null))
		throw(new AssertionError());
	}

	public void remove(Slot<? extends Rendered> slot) {
	    Clickslot cs = slots.remove(slot);
	    if(cs != null) {
		if(idmap.remove(cs.id) != cs)
		    throw(new AssertionError());
		if(back != null)
		    back.remove(cs);
	    }
	}

	public void update(Slot<? extends Rendered> slot) {
	    if(back != null) {
		Clickslot cs = slots.get(slot);
		if(cs != null) {
		    cs.state = null;
		    back.update(cs);
		}
	    }
	}

	public void update(Pipe group, int[] statemask) {
	    if(back != null)
		back.update(group, statemask);
	}

	public Locked lock() {
	    return(master.lock());
	}

	public Iterable<? extends Slot<?>> slots() {
	    return(slots.values());
	}

	/* Shouldn't have to care. */
	public <R> void add(RenderList<R> list, Class<? extends R> type) {}
	public void remove(RenderList<?> list) {}

	public void basic(Pipe.Op st) {
	    try(Locked lk = lock()) {
		DefPipe buf = new DefPipe();
		buf.prep(st);
		if(curbasic != null) {
		    if(curbasic.maskdiff(buf).length != 0)
			throw(new RuntimeException("changing clickbasic definition mask is not supported"));
		}
		int[] mask = basic.dupdate(buf);
		curbasic = buf;
		if(back != null)
		    back.update(basic, mask);
	    }
	}

	public Coord sz() {
	    return(basic.get(States.viewport).area.sz());
	}

	public void draw(Render out) {
	    if((draw == null) || !out.env().compatible(draw)) {
		if(draw != null)
		    dispose();
		draw = out.env().drawlist().desc("click-list: " + this);
		if(doinst) {
		    instancer = new InstanceList(this);
		    instancer.add(draw, Rendered.class);
		    instancer.asyncadd(this, Rendered.class);
		    back = instancer;
		} else {
		    draw.asyncadd(this, Rendered.class);
		    back = draw;
		}
	    }
	    try(Locked lk = lock()) {
		if(instancer != null)
		    instancer.commit(out);
		draw.draw(out);
	    }
	}

	public void get(Render out, Coord c, Consumer<ClickData> cb) {
	    out.pget(basic, FragID.fragid, Area.sized(Coord.of(c.x, sz().y - c.y), new Coord(1, 1)), new VectorFormat(1, NumberFormat.SINT32), data -> {
		    int id = data.getInt(0);
		    if(id == 0) {
			cb.accept(null);
			return;
		    }
		    Clickslot cs = idmap.get(id);
		    if(cs == null) {
			cb.accept(null);
			return;
		    }
		    cb.accept(new ClickData(cs.bk.state().get(Clickable.slot), (RenderTree.Slot)cs.bk.cast(RenderTree.Node.class)));
		});
	}

	public void fuzzyget(Render out, Coord c, int rad, Consumer<ClickData> cb) {
	    Coord gc = Coord.of(c.x, sz().y - 1 - c.y);
	    Area area = new Area(gc.sub(rad, rad), gc.add(rad + 1, rad + 1)).overlap(Area.sized(Coord.z, this.sz()));
	    out.pget(basic, FragID.fragid, area, new VectorFormat(1, NumberFormat.SINT32), data -> {
		    Clickslot cs;
		    {
			int id = data.getInt(area.ridx(gc) * 4);
			if((id != 0) && ((cs = idmap.get(id)) != null)) {
			    cb.accept(new ClickData(cs.bk.state().get(Clickable.slot), (RenderTree.Slot)cs.bk.cast(RenderTree.Node.class)));
			    return;
			}
		    }
		    int maxr = Integer.MAX_VALUE;
		    Map<Clickslot, Integer> score = new HashMap<>();
		    for(Coord fc : area) {
			int id = data.getInt(area.ridx(fc) * 4);
			if((id == 0) || ((cs = idmap.get(id)) == null))
			    continue;
			int r = (int)Math.round(fc.dist(gc) * 10);
			if(r < maxr) {
			    score.clear();
			    maxr = r;
			} else if(r > maxr) {
			    continue;
			}
			score.put(cs, score.getOrDefault(cs, 0) + 1);
		    }
		    int maxscore = 0;
		    cs = null;
		    for(Map.Entry<Clickslot, Integer> ent : score.entrySet()) {
			if((cs == null) || (ent.getValue() > maxscore)) {
			    maxscore = ent.getValue();
			    cs = ent.getKey();
			}
		    }
		    if(cs == null) {
			cb.accept(null);
			return;
		    }
		    cb.accept(new ClickData(cs.bk.state().get(Clickable.slot), (RenderTree.Slot)cs.bk.cast(RenderTree.Node.class)));
		});
	}

	public void dispose() {
	    if(instancer != null) {
		instancer.dispose();
		instancer = null;
	    }
	    if(draw != null) {
		draw.dispose();
		draw = null;
	    }
	    back = null;
	}

	public String stats() {
	    if(back == null)
		return("");
	    return(String.format("Tree %s, Inst %s, Draw %s, Map %d", master.stats(), (instancer == null) ? null : instancer.stats(), draw.stats(), idmap.size()));
	}
    }

    private final RenderTree clmaptree = new RenderTree();
    private final Clicklist clmaplist = new Clicklist(clmaptree, false);
    private final Clicklist clobjlist = new Clicklist(tree, true);
    private FragID<Texture.Image<Texture2D>> clickid;
    private ClickLocation<Texture.Image<Texture2D>> clickloc;
    private DepthBuffer<Texture.Image<Texture2D>> clickdepth;
    private Pipe.Op curclickbasic;
    private Pipe.Op clickbasic(Coord sz) {
	if((curclickbasic == null) || !clickid.image.tex.sz().equals(sz)) {
	    if(clickid != null) {
		clickid.image.tex.dispose();
		clickloc.image.tex.dispose();
		clickdepth.image.tex.dispose();
	    }
	    clickid = new FragID<>(new Texture2D(sz, DataBuffer.Usage.STATIC, new VectorFormat(1, NumberFormat.SINT32), null).image(0));
	    clickloc = new ClickLocation<>(new Texture2D(sz, DataBuffer.Usage.STATIC, new VectorFormat(2, NumberFormat.UNORM16), null).image(0));
	    clickdepth = new DepthBuffer<>(new Texture2D(sz, DataBuffer.Usage.STATIC, Texture.DEPTH, new VectorFormat(1, NumberFormat.FLOAT32), null).image(0));
	    curclickbasic = Pipe.Op.compose(Clicklist.clickbasic, clickid, clickdepth, new States.Viewport(Area.sized(Coord.z, sz)));
	}
	/* XXX: FrameInfo shouldn't be treated specially. Is a new
	 * Slot.Type in order, perhaps? */
	return(Pipe.Op.compose(curclickbasic, camera, conf.state().get(FrameInfo.slot)));
    }

    private void checkmapclick(Render out, Pipe.Op basic, Coord c, Consumer<Coord2d> cb) {
	new Object() {
	    MapMesh cut;
	    Coord2d pos;

	    {
		clmaplist.basic(Pipe.Op.compose(basic, clickloc));
		clmaplist.draw(out);
		if(clickdb) {
		    GOut.debugimage(out, clmaplist.basic, FragID.fragid, Area.sized(Coord.z, clmaplist.sz()), new VectorFormat(1, NumberFormat.SINT32),
				    img -> Debug.dumpimage(img, Debug.somedir("click1.png")));
		    GOut.debugimage(out, clmaplist.basic, ClickLocation.fragloc, Area.sized(Coord.z, clmaplist.sz()), new VectorFormat(3, NumberFormat.UNORM16),
				    img -> Debug.dumpimage(img, Debug.somedir("click2.png")));
		}
		clmaplist.get(out, c, cd -> {
			if(clickdb)
			    Debug.log.printf("map-id: %s\n", cd);
			if(cd != null)
			    this.cut = ((MapClick)cd.ci).cut;
			ckdone(1);
		    });
		out.pget(clmaplist.basic, ClickLocation.fragloc, Area.sized(Coord.of(c.x, clmaplist.sz().y - c.y), new Coord(1, 1)), new VectorFormat(2, NumberFormat.FLOAT32), data -> {
			pos = new Coord2d(data.getFloat(0), data.getFloat(4));
			if(clickdb)
			    Debug.log.printf("map-pos: %s\n", pos);
			ckdone(2);
		    });
	    }

	    int dfl = 0;
	    void ckdone(int fl) {
		synchronized(this) {
		    if((dfl |= fl) == 3) {
			if(cut == null)
			    cb.accept(null);
			else
			    cb.accept(new Coord2d(cut.ul).add(pos.mul(new Coord2d(cut.sz))).mul(tilesz));
		    }
		}
	    }
	};
    }
    
    private static int gobclfuzz = 3;
    private void checkgobclick(Render out, Pipe.Op basic, Coord c, Consumer<ClickData> cb) {
	clobjlist.basic(basic);
	clobjlist.draw(out);
	if(clickdb) {
	    GOut.debugimage(out, clobjlist.basic, FragID.fragid, Area.sized(Coord.z, clobjlist.sz()), new VectorFormat(1, NumberFormat.SINT32),
			  img -> Debug.dumpimage(img, Debug.somedir("click3.png")));
	    Consumer<ClickData> ocb = cb;
	    cb = cl -> {
		Debug.log.printf("obj-id: %s\n", cl);
		ocb.accept(cl);
	    };
	}
	clobjlist.fuzzyget(out, c, gobclfuzz, cb);
    }
    
    public void delay(Delayed d) {
	synchronized(delayed) {
	    delayed.add(d);
	}
    }

    public void delay2(Delayed d) {
	synchronized(delayed2) {
	    delayed2.add(d);
	}
    }

    protected void undelay(Collection<Delayed> list, GOut g) {
	synchronized(list) {
	    for(Delayed d : list)
		d.run(g);
	    list.clear();
	}
    }

	public void delayB(DelayedB d) {
		synchronized(delayedB) {
			delayedB.add(d);
		}
	}
	protected void undelayB(Collection<DelayedB> list) {
		synchronized(list) {
			for(DelayedB d : list)
				d.run();
			list.clear();
		}
	}

    static class PolText {
	Text text; double tm;
	PolText(Text text, double tm) {this.text = text; this.tm = tm;}
    }

    private static final Text.Furnace polownertf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 30).aa(true), 3, 1, Color.BLACK);
    private final Map<Integer, PolText> polowners = new HashMap<Integer, PolText>();

    public void setpoltext(int id, String text) {
	synchronized(polowners) {
	    polowners.put(id, new PolText(polownertf.render(text), Utils.rtime()));
	}
    }

    private void poldraw(GOut g) {
	if(polowners.isEmpty())
	    return;
	double now = Utils.rtime();
	synchronized(polowners) {
	    int y = (sz.y / 3) - (polowners.values().stream().map(t -> t.text.sz().y).reduce(0, (a, b) -> a + b + 10) / 2);
	    for(Iterator<PolText> i = polowners.values().iterator(); i.hasNext();) {
		PolText t = i.next();
		double poldt = now - t.tm;
		if(poldt < 6.0) {
		    int a;
		    if(poldt < 1.0)
			a = (int)(255 * poldt);
		    else if(poldt < 4.0)
			a = 255;
		    else
			a = (int)((255 * (2.0 - (poldt - 4.0))) / 2.0);
		    g.chcolor(255, 255, 255, a);
		    g.aimage(t.text.tex(), new Coord((sz.x - t.text.sz().x) / 2, y), 0.0, 0.0);
		    y += t.text.sz().y + 10;
		    g.chcolor();
		} else {
		    i.remove();
		}
	    }
	}
    }
    
    private void drawarrow(GOut g, double a) {
	Coord hsz = sz.div(2);
	double ca = -Coord.z.angle(hsz);
	Coord ac;
	if((a > ca) && (a < -ca)) {
	    ac = new Coord(sz.x, hsz.y - (int)(Math.tan(a) * hsz.x));
	} else if((a > -ca) && (a < Math.PI + ca)) {
	    ac = new Coord(hsz.x - (int)(Math.tan(a - Math.PI / 2) * hsz.y), 0);
	} else if((a > -Math.PI - ca) && (a < ca)) {
	    ac = new Coord(hsz.x + (int)(Math.tan(a + Math.PI / 2) * hsz.y), sz.y);
	} else {
	    ac = new Coord(0, hsz.y + (int)(Math.tan(a) * hsz.x));
	}
	Coord bc = ac.add(Coord.sc(a, -10));
	g.line(bc, bc.add(Coord.sc(a, -40)), 2);
	g.line(bc, bc.add(Coord.sc(a + Math.PI / 4, -10)), 2);
	g.line(bc, bc.add(Coord.sc(a - Math.PI / 4, -10)), 2);
    }

    public HomoCoord4f clipxf(Coord3f mc, boolean doclip) {
	HomoCoord4f ret = Homo3D.obj2clip(new Coord3f(mc.x, -mc.y, mc.z), basic.state());
	if(doclip && ret.clipped()) {
	    Projection s_prj = basic.state().get(Homo3D.prj);
	    Matrix4f prj = (s_prj == null) ? Matrix4f.id : s_prj.fin(Matrix4f.id);
	    ret = HomoCoord4f.lineclip(HomoCoord4f.fromclip(prj, Coord3f.o), ret);
	}
	return(ret);
    }

    public Coord3f screenxf(Coord3f mc) {
	return(clipxf(mc, false).toview(Area.sized(this.sz)));
    }

    public Coord3f screenxf(Coord2d mc) {
	Coord3f cc;
	try {
	    cc = getcc();
	} catch(Loading e) {
	    return(null);
	}
	return(screenxf(new Coord3f((float)mc.x, (float)mc.y, cc.z)));
    }

    public double screenangle(Coord2d mc, boolean clip) {
	Coord3f cc;
	try {
	    cc = getcc();
	} catch(Loading e) {
	    return(Double.NaN);
	}
	Coord3f mloc = new Coord3f((float)mc.x, -(float)mc.y, cc.z);
	float[] sloc = camera.proj.toclip(camera.view.fin(Matrix4f.id).mul4(mloc));
	if(clip) {
	    float w = sloc[3];
	    if((sloc[0] > -w) && (sloc[0] < w) && (sloc[1] > -w) && (sloc[1] < w))
		return(Double.NaN);
	}
	float a = ((float)sz.y) / ((float)sz.x);
	return(Math.atan2(sloc[1] * a, sloc[0]));
    }

    private void partydraw(GOut g) {
	for(Party.Member m : ui.sess.glob.party.memb.values()) {
	    if(m.gobid == this.plgob)
		continue;
	    Coord2d mc = m.getc();
	    if(mc == null)
		continue;
	    double a = screenangle(mc, true);
	    if(Double.isNaN(a))
		continue;
	    g.chcolor(m.col);
	    drawarrow(g, a);
	}
	g.chcolor();
    }

    protected void maindraw(Render out) {
	drawsmap(out);
	super.maindraw(out);
    }

    private Loading camload = null, lastload = null;
    public void draw(GOut g) {
	Loader.Future<Plob> placing = this.placing;
	if((placing != null) && placing.done())
	    placing.get().gtick(g.out);
	glob.map.sendreqs();
	if((olftimer != 0) && (olftimer < Utils.rtime()))
	    unflashol();
	try {
	    if(camload != null)
		throw(new Loading(camload));
	    undelay(delayed, g);
		undelayB(delayedB);
	    super.draw(g);
	    undelay(delayed2, g);
	    poldraw(g);
	    partydraw(g);
	    glob.map.reqarea(cc.floor(tilesz).sub(MCache.cutsz.mul(view + 1)),
			     cc.floor(tilesz).add(MCache.cutsz.mul(view + 1)));
	} catch(Loading e) {
	    e.boostprio(6);
	    lastload = e;
	    String text = e.getMessage();
	    if(text == null)
		text = "Loading...";
	    g.chcolor(Color.BLACK);
	    g.frect(Coord.z, sz);
	    g.chcolor(Color.WHITE);
	    g.atext(text, sz.div(2), 0.5, 0.5);
	}
    }
    
    private double initload = -2;
    private boolean initdraw = false;
    private void checkload() {
	if(initload == -1)
	    return;
	double now = Utils.rtime();
	if(initload == -2) {
	    delay2(g -> initdraw = true);
	    initload = now;
	}
	if((terrain.loading() == null) && (gobs.loading() == null) && initdraw) {
	    wdgmsg("initload", now - initload);
	    initload = -1;
	}
    }

	public static boolean isWeatherDisabled = Utils.getprefb("isWeatherDisabled", false);
    public void tick(double dt) {
	super.tick(dt);
	checkload();
	camload = null;
	try {
	    if((shake = shake * Math.pow(100, -dt)) < 0.01)
		shake = 0;
	    camoff.x = (float)((Math.random() - 0.5) * shake);
	    camoff.y = (float)((Math.random() - 0.5) * shake);
	    camoff.z = (float)((Math.random() - 0.5) * shake);
	    camera.tick(dt);
	} catch(Loading e) {
	    e.boostprio(5);
	    camload = e;
	}
	basic(Camera.class, camera);
	amblight();
	updsmap(amblight);
	if (!isWeatherDisabled) updweather();
	synchronized(glob.map) {
	    terrain.tick();
	    oltick();
	    if(gridlines != null)
		gridlines.tick();
	    clickmap.tick();
	}
	partyHighlight.update();
	partyCircles.update();
	Loader.Future<Plob> placing = this.placing;
	if((placing != null) && placing.done()) {
		placing.get().ctick(dt);
	}
    }
    
    public void resize(Coord sz) {
	super.resize(sz);
	camera.resized();
    }

    public static interface PlobAdjust {
	public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags);
	public boolean rotate(Plob plob, int amount, int modflags);
    }

    public static class StdPlace implements PlobAdjust {
	boolean freerot = false;

	public void adjust(Plob plob, Coord pc, Coord2d mc, int modflags) {
	    Coord2d nc;
	    if((modflags & UI.MOD_CTRL) == 0)
		nc = mc.floor(tilesz).mul(tilesz).add(tilesz.div(2));
	    else if(plobpgran > 0)
		nc = mc.div(tilesz).mul(plobpgran).roundf().div(plobpgran).mul(tilesz);
	    else
		nc = mc;
	    Gob pl = plob.mv().player();
	    if((pl != null) && !freerot)
		plob.move(nc, Math.round(plob.rc.angle(pl.rc) / (Math.PI / 2)) * (Math.PI / 2));
	    else
		plob.move(nc);
	}

	public boolean rotate(Plob plob, int amount, int modflags) {
		if((modflags & UI.MOD_SHIFT) == 0)
		return(false);
	    freerot = true;
	    double na;
	    if((modflags & UI.MOD_CTRL) == 0)
		na = (Math.PI / 4) * Math.round((plob.a + (amount * Math.PI / 4)) / (Math.PI / 4));
	    else
		na = plob.a + amount * Math.PI / plobagran;
	    na = Utils.cangle(na);
	    plob.move(na);
	    return(true);
	}
    }

    public class Plob extends Gob {
	public PlobAdjust adjust = new StdPlace();
	Coord lastmc = null;
	RenderTree.Slot slot;

	private Plob(Indir<Resource> res, Message sdt) {
	    super(MapView.this.glob, MapView.this.cc);
	    setattr(new ResDrawable(this, res, sdt));
	}

	public MapView mv() {return(MapView.this);}

	public void move(Coord2d c, double a) {
	    super.move(c, a);
	    updated();
	}

	public void move(Coord2d c) {
	    move(c, this.a);
	}

	public void move(double a) {
	    move(this.rc, a);
	}

	void place() {
	    if(ui.mc.isect(rootpos(), sz))
		new Adjust(ui.mc.sub(rootpos()), 0).run();
	    this.slot = basic.add(this.placed);
	}

	private class Adjust extends Maptest {
	    int modflags;
	    
	    Adjust(Coord c, int modflags) {
		super(c);
		this.modflags = modflags;
	    }
	    
	    public void hit(Coord pc, Coord2d mc) {
		adjust.adjust(Plob.this, pc, mc, modflags);
		lastmc = pc;
	    }
	}
    }

    private Collection<String> olflash = null;
    private double olftimer;

    private void unflashol() {
	if(olflash != null) {
	    olflash.forEach(this::disol);
	}
	olflash = null;
	olftimer = 0;
    }

    private void flashol(Collection<String> ols, double tm) {
	unflashol();
	ols.forEach(this::enol);
	olflash = ols;
	olftimer = Utils.rtime() + tm;
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "place") {
	    Loader.Future<Plob> placing = this.placing;
	    if(placing != null) {
		if(!placing.cancel()) {
		    Plob ob = placing.get();
		    synchronized(ob) {
			ob.slot.remove();
		    }
		}
		this.placing = null;
	    }
	    int a = 0;
	    Indir<Resource> res = ui.sess.getres((Integer)args[a++]);
	    Message sdt;
	    if((args.length > a) && (args[a] instanceof byte[]))
		sdt = new MessageBuf((byte[])args[a++]);
	    else
		sdt = Message.nil;
	    int oa = a;
	    this.placing = glob.loader.defer(new Supplier<Plob>() {
		    int a = oa;
		    Plob ret = null;
		    public Plob get() {
			if(ret == null)
			    ret = new Plob(res, new MessageBuf(sdt));
			while(a < args.length) {
			    int a2 = a;
			    Indir<Resource> ores = ui.sess.getres((Integer)args[a2++]);
			    Message odt;
			    if((args.length > a2) && (args[a2] instanceof byte[]))
				odt = new MessageBuf((byte[])args[a2++]);
			    else
				odt = Message.nil;
			    ret.addol(ores, odt);
			    a = a2;
			}
			ret.place();
			return(ret);
		    }
		});
	} else if(msg == "unplace") {
	    Loader.Future<Plob> placing = this.placing;
	    if(placing != null) {
		if(!placing.cancel()) {
		    Plob ob = placing.get();
		    synchronized(ob) {
			ob.slot.remove();
		    }
		}
		this.placing = null;
	    }
	} else if(msg == "move") {
	    cc = ((Coord)args[0]).mul(posres);
	} else if(msg == "plob") {
	    if(args[0] == null)
		plgob = -1;
	    else
		plgob = Utils.uint32((Integer)args[0]);
	} else if(msg == "flashol2") {
	    Collection<String> ols = new LinkedList<>();
	    double tm = ((Number)args[0]).doubleValue() / 100.0;
	    for(int a = 1; a < args.length; a++)
		ols.add((String)args[a]);
	    flashol(ols, tm);
	} else if(msg == "sel") {
	    boolean sel = ((Integer)args[0]) != 0;
	    synchronized(this) {
		if(sel && (selection == null)) {
		    selection = new Selector();
		} else if(!sel && (selection != null)) {
		    selection.destroy();
		    selection = null;
		}
	    }
	} else if(msg == "shake") {
	    shake += ((Number)args[0]).doubleValue();
	} else {
	    super.uimsg(msg, args);
	}
    }

    private UI.Grab camdrag = null;
    
    public abstract class Maptest {
	private final Coord pc;

	public Maptest(Coord c) {
	    this.pc = c;
	}

	public void run() {
	    Environment env = ui.env;
	    Render out = env.render();
	    Pipe.Op basic = clickbasic(MapView.this.sz);
	    Pipe bstate = new BufPipe().prep(basic);
	    out.clear(bstate, FragID.fragid, FColor.BLACK);
	    out.clear(bstate, 1.0);
	    checkmapclick(out, basic, pc, mc -> {
		    synchronized(ui) {
			if(mc != null)
			    hit(pc, mc);
			else
			    nohit(pc);
		    }
		});
	    env.submit(out);
	}

	protected abstract void hit(Coord pc, Coord2d mc);
	protected void nohit(Coord pc) {}
    }

    public abstract class Hittest implements DelayedB {
	private final Coord pc;
	private Coord2d mapcl;
	private ClickData objcl;
	private int dfl = 0;
	
	public Hittest(Coord c) {
	    pc = c;
	}
	
	public void run() {
	    Environment env = ui.env;
	    Render out = env.render();
	    Pipe.Op basic = clickbasic(MapView.this.sz);
	    Pipe bstate = new BufPipe().prep(basic);
	    out.clear(bstate, FragID.fragid, FColor.BLACK);
	    out.clear(bstate, 1.0);
	    checkmapclick(out, basic, pc, mc -> {mapcl = mc; ckdone(1);});
	    out.clear(bstate, FragID.fragid, FColor.BLACK);
	    checkgobclick(out, basic, pc, cl -> {objcl = cl; ckdone(2);});
	    env.submit(out);
	}

	private void ckdone(int fl) {
	    boolean done = false;
	    synchronized(this) {
		    if((dfl |= fl) == 3)
			done = true;
	    }
	    if(done) {
		synchronized(ui) {
		    if(mapcl != null) {
			if(objcl == null)
			    hit(pc, mapcl, null);
			else
			    hit(pc, mapcl, objcl);
		    } else {
			nohit(pc);
		    }
		}
	    }
	}
	
	protected abstract void hit(Coord pc, Coord2d mc, ClickData inf);
	protected void nohit(Coord pc) {}
    }

    private class Click extends Hittest {
	int clickb;
	
	private Click(Coord c, int b) {
	    super(c);
	    clickb = b;
	}

	protected void hit(Coord pc, Coord2d mc, ClickData inf) {
		GameUI gui = gameui();
		// reset alt so we could walk with alt+lmb while having item on the cursor
		int modflags = ui.modflags();
		if (gui.vhand != null && clickb == 1)
			modflags = modflags & ~4;

	    Object[] args = {pc, mc.floor(posres), clickb, modflags};
	    if(inf != null) {
			args = Utils.extend(args, inf.clickargs());
			Long gobid = new Long((Integer) inf.clickargs()[1]);
			Gob gob = glob.oc.getgob(gobid);
			if(gob != null) {
				if(ui.isCursor("gfx/hud/curs/study") && clickb == 1) {
					gameui().setDetectGob(gob);
				}
				if (ui.modmeta && gui.vhand == null) {
					Map<String, ChatUI.MultiChat> chats = gui.chat.getMultiChannels();
					if (clickb == 1 && (!ui.modshift || !ui.modctrl)) {
						chats.get("Area Chat").send("@" + gob.id);
					} else if (clickb == 3 && (!ui.modshift || !ui.modctrl)) {
						if (chats.get("Party") != null)
							chats.get("Party").send("@" + gob.id);
					}
					if(OptWnd.objectPermanentHighlighting && clickb == 2 && (ui.modmeta && !(ui.modshift || ui.modctrl))){
						if (Gob.listHighlighted.contains(gob.id)) {
							Gob.listHighlighted.remove(gob.id);
							gob.delattr(GobPermanentHighlight.class);
						} else {
							Gob.listHighlighted.add(gob.id);
							gob.setattr(new GobPermanentHighlight(gob, GobPermanentHighlight.State.PURPLE));
						}
					}
					return;
				} else {
					if (clickb == 3) {
						if (OptWnd.autoswitchBunnyPlateBoots) {
							try {
								WItem eqboots = gui.getequipory().slots[Equipory.SLOTS.BOOTS.idx];
								List<WItem> invboots;
								if (gob.getres().name.contains("/rabbit/")) {
									invboots = gui.maininv.getItemsExact("Bunny Slippers");
									if (invboots.size() > 0) {
										if (eqboots != null && !eqboots.item.getname().equals("Bunny Slippers")) {
											eqboots.item.wdgmsg("transfer", new Coord(eqboots.sz.x / 2, eqboots.sz.y / 2));
										}
										WItem slipper = invboots.get(0);
										slipper.item.wdgmsg("transfer", new Coord(slipper.sz.x / 2, slipper.sz.y / 2));
									}
								} else {
									invboots = gui.maininv.getItemsExact("Plate Boots");
									if (eqboots != null && eqboots.item.getname().equals("Bunny Slippers")) {
										if (invboots.size() > 0) {
											eqboots.item.wdgmsg("transfer", new Coord(eqboots.sz.x / 2, eqboots.sz.y / 2));
											WItem boots = invboots.get(0);
											boots.item.wdgmsg("transfer", new Coord(boots.sz.x / 2, boots.sz.y / 2));
										}
									}
								}
							} catch (Exception ignored) {
							}
						}
						wdgmsg("click", args);
						if (OptWnd.instantFlowerMenuCTRL) {
							if (ui.modctrl) {
								gameui().ui.rcvr.rcvmsg(gameui().ui.lastid + 1, "cl", 0, gameui().ui.modflags());
							}
						}
						return;
					}
					if(!GameUI.walkWithPathfinder){
						wdgmsg("click", args);
					} else {
						pfLeftClick(mc.floor(), null);
					}
					return;
				}
			}
		} else {
			if (clickb == 1 && ui.modmeta && gui.vhand == null) {
				addCheckpoint(mc);
			} else if(clickb == 1) {
				if (OptWnd.autoswitchBunnyPlateBoots) {
					try {
						if (gui.getequipory() != null && gui.getequipory().slots != null) {
							WItem eqboots = gui.getequipory().slots[Equipory.SLOTS.BOOTS.idx];
							if (eqboots != null && eqboots.item.getname().equals("Bunny Slippers")) {
								List<WItem> invboots = gui.maininv.getItemsExact("Plate Boots");
								if (invboots.size() > 0) {
									eqboots.item.wdgmsg("transfer", new Coord(eqboots.sz.x / 2, eqboots.sz.y / 2));
									WItem boots = invboots.get(0);
									boots.item.wdgmsg("transfer", new Coord(boots.sz.x / 2, boots.sz.y / 2));
								}
							}
						}
					} catch (Exception e) {}
				}
			}
		}
		if(checkpointManager != null && checkpointManagerThread != null){
			checkpointManager.pauseIt();
		}
		if(!GameUI.walkWithPathfinder){
			wdgmsg("click", args);
		} else {
			pfLeftClick(mc.floor(), null);
		}
	}
    }
    
    public void grab(Grabber grab) {
	this.grab = grab;
    }
    
    public void release(Grabber grab) {
	if(this.grab == grab)
	    this.grab = null;
    }
    
    public boolean mousedown(Coord c, int button) {
	parent.setfocus(this);
	Loader.Future<Plob> placing_l = this.placing;
	if (button == 1 && areaSelect) {
		synchronized (this) {
			if (selection == null) {
				selection = new Selector();
			} else {
				selection.destroy();
				selection = null;
				areaSelect = false;
			}
		}
	}
	if(button == 2) {
		new Click(c, button).run();
	    if(((Camera)camera).click(c)) {
		camdrag = ui.grabmouse(this);
	    }
	} else if((placing_l != null) && placing_l.done()) {
	    Plob placing = placing_l.get();
	    if(placing.lastmc != null) {
			int modflags = ui.modflags(); // ND: These are the "mod" buttons you're holding down: CTRL, SHIFT, ALT, and god knows wtf "modsuper" means.
			// ND: Loftar checks on the serverside if we're holding CTRL while placing an item, which causes us to walk to that position if we do.
			// ND: I'm doing the following to switch that with ALT instead, by switching the information we send to the server.
			// ND: "2" is the value of "modctrl"
			if (ui.modctrl && !ui.modmeta) { // ND: Only extract this if we're holding CTRL while also not holding ALT
				modflags = modflags - 2;
			} else if (ui.modmeta && !ui.modctrl) { // ND: But also add it if we're holding ALT but not holding CTRL
				modflags = modflags + 2;
			}
			wdgmsg("place", placing.rc.floor(posres), (int) Math.round(placing.a * 32768 / Math.PI), button, modflags);
		}
	} else if((grab != null) && grab.mmousedown(c, button)) {
	} else {
	    new Click(c, button).run();
	}
	return(true);
    }
    
    public void mousemove(Coord c) {
	if(grab != null)
	    grab.mmousemove(c);
	Loader.Future<Plob> placing_l = this.placing;
	if(camdrag != null) {
	    ((Camera)camera).drag(c);
	} else if((placing_l != null) && placing_l.done()) {
	    Plob placing = placing_l.get();
	    if((placing.lastmc == null) || !placing.lastmc.equals(c)) {
		placing.new Adjust(c, ui.modflags()).run();
	    }
	}  else if (ui.modshift && ui.modctrl) {
		long now = System.currentTimeMillis();
		if ((now - lastmmhittest > 500 || lasthittestc.dist(c) > tilesz.x) && gameui().hand.isEmpty()) {
			lastmmhittest = now;
			lasthittestc = c;

			delayB(new Hittest(c) {
				@Override
				protected void hit(Coord pc, Coord2d mc, ClickData inf) {
					if (inf != null) {
						Long gobid = new Long((Integer) inf.clickargs()[1]);
						Gob gob = glob.oc.getgob(gobid);
						if (gob != null) {
							try {
								Resource res = gob.getres();

								String ols = null;
								String ols2 = null;
								String poses = null;
								String gattrs = null;
								String equip = null;
								String peekrbuf = null;
								String plantinfo = null;
								try {
									ols = gob.ols.stream().map(ol -> ol.res.get().name).collect(Collectors.joining(", "));
								} catch (Exception ignored) {}
								try {
									ols2 = gob.ols.stream().map(ol -> {
										try {
											return glob.sess.getres(haven.Utils.uint16d(ol.sdt.rbuf, 0)).get().basename()
													+ " State: " + ol.sdt.peekrbuf(0);
										} catch(IndexOutOfBoundsException e) {
											return "N/A";
										}
									}).collect(Collectors.joining(", "));
								} catch (Exception ignores) {}
								try {
									gattrs = gob.attr.keySet().stream().map(Class::getName).collect(Collectors.joining(", ")).replace("$", ".");
								} catch (Exception ignored) { }
								//Composites:
								if (gob.isComposite) {
									try {
										if (gob.getattr(Drawable.class) != null) {
											poses = ((Composite)gob.getattr(Drawable.class)).poses.stream().collect(Collectors.joining(", "));
										}
									} catch (Exception ignored) { }
								}

								for (GAttrib g : gob.attr.values()) {
									if (g instanceof Drawable) {
										if (g instanceof ResDrawable) {
											ResDrawable resDrawable = gob.getattr(ResDrawable.class);
												peekrbuf = "" + resDrawable.sdt.peekrbuf(0);

										} else if (g instanceof Composite) {
											Composite c = (Composite) g;
											StringBuilder sb = new StringBuilder();
											if (c.comp.cequ.size() > 0) {
												sb.append("$col[255,200,0]{Wearing:} \n");
												for (Composited.ED item : c.comp.cequ) {
													sb.append("   "+item.res.res.get().basename()+" \n");
												}
											}
											if (c.comp.cmod.size() > 0) {
												sb.append("$col[255,200,0]{Mod:} \n");
												for (Composited.MD item : c.comp.cmod) {
													sb.append("   "+item.mod.get().basename()+" \n");
												}
											}
											equip = sb.toString();
										}
									}
								}
								if (res != null) {
									String tt;
									if (OptWnd.advancedMouseInfo)
										tt = "Object Resource Path: " + "$col[255,200,0]{" + res.name + "}" +
												" \nID: " + gob.id +
												" \nRC: " + ui.sess.glob.map.getzp(gob.rc) +
												String.format(" \nAngle: %.2f (%.2f\u00B0)", gob.a, 360.*(gob.a/(2.*Math.PI))) +
												(ols != null ? " \nOls: $col[192,192,255]{" + ols + "}" : "") +
												(ols2 != null ? " \nOls exp: $col[192,192,255]{" + ols2 + "}" : "") +
												(poses != null ? " \nPoses: $col[192,192,255]{" + poses + "}" : "") +
												(gattrs != null ? " \nGattrs: $col[192,192,255]{" + gattrs + "}" : "") +
												(peekrbuf != null ? " \nPeekrbuf: $col[192,192,255]{" + peekrbuf + "}" : "") +
												(equip != null ? " \n$col[255,255,192]{" + equip + "}" : "");
									else
										tt = "Object Resource Path: " + "$col[255,200,0]{" + res.name + "}";
									tooltip = RichText.render(tt, 400);
									return;
								}
							} catch (Loading e) {
							}
						}
					} else {
						MCache map = ui.sess.glob.map;
						int t = map.gettile(mc.floor(tilesz));
						Resource res = map.tilesetr(t);
						if (res != null) {
							if (OptWnd.advancedMouseInfo)
								tooltip = RichText.render("Tile Resource Path: " + "$col[255,200,0]{" + res.name + "}" +
									" \nMC: " + mc.floor(), UI.scale(400));
							else
								tooltip = RichText.render("Tile Resource Path: " + "$col[255,200,0]{" + res.name + "}", UI.scale(400));
							return;
						}
					}
					tooltip = null;
				}

				protected void nohit(Coord pc) {
					if (OptWnd.advancedMouseInfo)
						System.out.println(pc);
					tooltip = null;
				}
			});
		}
	} else if (tooltip != null) {
		tooltip = null;
	}
    }
    
    public boolean mouseup(Coord c, int button) {
	if(button == 2) {
	    if(camdrag != null) {
		camera.release();
		camdrag.remove();
		camdrag = null;
	    }
	} else if(grab != null) {
	    grab.mmouseup(c, button);
	}
	return(true);
    }

    public boolean mousewheel(Coord c, int amount) {
	Loader.Future<Plob> placing_l = this.placing;
	if((grab != null) && grab.mmousewheel(c, amount))
	    return(true);
	if((placing_l != null) && placing_l.done()) {
	    Plob placing = placing_l.get();
	    if(placing.adjust.rotate(placing, amount, ui.modflags()))
		return(true);
	}
	return(((Camera)camera).wheel(c, amount));
    }
    
    public boolean drop(final Coord cc, Coord ul) {
	new Hittest(cc) {
	    public void hit(Coord pc, Coord2d mc, ClickData inf) {
			if((GameUI.preventDropAnywhere || GameUI.preventWaterDrop) && !ui.modctrl) {
				boolean nodropping = false;
				if (GameUI.preventDropAnywhere) {
					nodropping = true;
				} else {
					int t = glob.map.gettile(player().rc.floor(tilesz));
					Resource res = glob.map.tilesetr(t);
					if (res != null && (
							res.name.equals("gfx/tiles/water")
									|| res.name.equals("gfx/tiles/deep"))
							|| res.name.equals("gfx/tiles/owater")
							|| res.name.equals("gfx/tiles/odeep")
							|| res.name.equals("gfx/tiles/odeeper")) {
						nodropping = true;
					}
				}
				if (nodropping) {
					wdgmsg("click", pc, mc.floor(posres), 1, 0);
					return;
				}
			}
		wdgmsg("drop", pc, mc.floor(posres), ui.modflags());
	    }
	}.run();
	return(true);
    }
    
    public boolean iteminteract(Coord cc, Coord ul) {
	new Hittest(cc) {
	    public void hit(Coord pc, Coord2d mc, ClickData inf) {
		Object[] args = {pc, mc.floor(posres), ui.modflags()};
		if(inf != null)
		    args = Utils.extend(args, inf.clickargs());
		wdgmsg("itemact", args);
	    }
	}.run();
	return(true);
    }

    public boolean keydown(KeyEvent ev) {
	Loader.Future<Plob> placing_l = this.placing;
	if((placing_l != null) && placing_l.done()) {
	    Plob placing = placing_l.get();
	    if((ev.getKeyCode() == KeyEvent.VK_LEFT) && placing.adjust.rotate(placing, -1, ui.modflags()))
		return(true);
	    if((ev.getKeyCode() == KeyEvent.VK_RIGHT) && placing.adjust.rotate(placing, 1, ui.modflags()))
		return(true);
	}
	if(camera.keydown(ev))
	    return(true);
	return(super.keydown(ev));
    }

    public static final KeyBinding kb_grid = KeyBinding.get("grid", KeyMatch.forchar('G', KeyMatch.C));
    public boolean globtype(char c, KeyEvent ev) {
	if(kb_grid.key().match(ev)) {
	    showgrid(gridlines == null);
	    return(true);
	}
	return(false);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(selection != null) {
	    if(selection.tt != null)
		return(selection.tt);
	}
	return(super.tooltip(c, prev));
    }

    public class GrabXL implements Grabber {
	private final Grabber bk;
	public boolean mv = false;

	public GrabXL(Grabber bk) {
	    this.bk = bk;
	}

	public boolean mmousedown(Coord cc, final int button) {
	    new Maptest(cc) {
		public void hit(Coord pc, Coord2d mc) {
		    bk.mmousedown(mc.round(), button);
		}
	    }.run();
	    return(true);
	}

	public boolean mmouseup(Coord cc, final int button) {
	    new Maptest(cc) {
		public void hit(Coord pc, Coord2d mc) {
		    bk.mmouseup(mc.round(), button);
		}
	    }.run();
	    return(true);
	}

	public boolean mmousewheel(Coord cc, final int amount) {
	    new Maptest(cc) {
		public void hit(Coord pc, Coord2d mc) {
		    bk.mmousewheel(mc.round(), amount);
		}
	    }.run();
	    return(true);
	}

	public void mmousemove(Coord cc) {
	    if(mv) {
		new Maptest(cc) {
		    public void hit(Coord pc, Coord2d mc) {
			bk.mmousemove(mc.round());
		    }
		}.run();
	    }
	}
    }

    public static final OverlayInfo selol = new OverlayInfo() {
	    final Material mat = new Material(new BaseColor(255, 255, 0, 32), States.maskdepth);

	    public Collection<String> tags() {
		return(Arrays.asList("show"));
	    }

	    public Material mat() {return(mat);}
	};
    private class Selector implements Grabber {
	Coord sc;
	MCache.Overlay ol;
	UI.Grab mgrab;
	int modflags;
	Text tt;
	final GrabXL xl = new GrabXL(this) {
		public boolean mmousedown(Coord cc, int button) {
		    if(button != 1)
			return(false);
		    return(super.mmousedown(cc, button));
		}
		public boolean mmousewheel(Coord cc, int amount) {
		    return(false);
		}
	    };

	{
	    grab(xl);
	}

	public boolean mmousedown(Coord mc, int button) {
	    synchronized(MapView.this) {
		if(selection != this)
		    return(false);
		if(sc != null) {
		    ol.destroy();
		    mgrab.remove();
		}
		sc = mc.div(MCache.tilesz2);
		modflags = ui.modflags();
		xl.mv = true;
		mgrab = ui.grabmouse(MapView.this);
		ol = glob.map.new Overlay(Area.sized(sc, new Coord(1, 1)), selol);
		return(true);
	    }
	}

	public boolean mmouseup(Coord mc, int button) {
	    synchronized(MapView.this) {
		if(sc != null) {
		    Coord ec = mc.div(MCache.tilesz2);
		    xl.mv = false;
		    tt = null;
		    ol.destroy();
		    mgrab.remove();
			if (areaSelectCallback != null) {
				areaSelectCallback.areaselect(ol.a.ul, ol.a.br);
			}
		    wdgmsg("sel", sc, ec, modflags);
		    sc = null;
		}
		return(true);
	    }
	}

	public boolean mmousewheel(Coord mc, int amount) {
	    return(false);
	}

	public void mmousemove(Coord mc) {
	    synchronized(MapView.this) {
		if(sc != null) {
		    Coord tc = mc.div(MCache.tilesz2);
		    Coord c1 = new Coord(Math.min(tc.x, sc.x), Math.min(tc.y, sc.y));
		    Coord c2 = new Coord(Math.max(tc.x, sc.x), Math.max(tc.y, sc.y));
		    ol.update(new Area(c1, c2.add(1, 1)));
		    tt = Text.render(String.format("%d\u00d7%d", c2.x - c1.x + 1, c2.y - c1.y + 1));
		}
	    }
	}

	public void destroy() {
	    synchronized(MapView.this) {
		if(sc != null) {
		    ol.destroy();
		    mgrab.remove();
		}
		release(xl);
	    }
	}
    }

    private Camera makecam(Class<? extends Camera> ct, String... args) {
	try {
	    try {
		Constructor<? extends Camera> cons = ct.getConstructor(MapView.class, String[].class);
		return(cons.newInstance(new Object[] {this, args}));
	    } catch(IllegalAccessException e) {
	    } catch(NoSuchMethodException e) {
	    }
	    try {
		Constructor<? extends Camera> cons = ct.getConstructor(MapView.class);
		return(cons.newInstance(new Object[] {this}));
	    } catch(IllegalAccessException e) {
	    } catch(NoSuchMethodException e) {
	    }
	} catch(InstantiationException e) {
	    throw(new Error(e));
	} catch(InvocationTargetException e) {
	    if(e.getCause() instanceof RuntimeException)
		throw((RuntimeException)e.getCause());
	    throw(new RuntimeException(e));
	}
	throw(new RuntimeException("No valid constructor found for camera " + ct.getName()));
    }

    private Camera restorecam() {
	Class<? extends Camera> ct = camtypes.get(Utils.getpref("defcam", null));
	if(ct == null)
	    return(new SOrthoCam());
	String[] args = (String [])Utils.deserialize(Utils.getprefb("camargs", null));
	if(args == null) args = new String[0];
	try {
	    return(makecam(ct, args));
	} catch(Exception e) {
	    return(new SOrthoCam());
	}
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
		//ND: Change the "cam" console command. Console commands are stupid and not everyone knows of them, use the options menu instead.
//	cmdmap.put("cam", new Console.Command() {
//		public void run(Console cons, String[] args) throws Exception {
//		    if(args.length >= 2) {
//			Class<? extends Camera> ct = camtypes.get(args[1]);
//			String[] cargs = AUtils.splice(args, 2);
//			if(ct != null) {
//				camera = makecam(ct, cargs);
//				AUtils.setpref("defcam", args[1]);
//				AUtils.setprefb("camargs", AUtils.serialize(cargs));
//			} else {
//			    throw(new Exception("no such camera: " + args[1]));
//			}
//		    }
//		}
//	    });

		cmdmap.put("cam", new Console.Command() {
			public void run(Console cons, String[] args) throws Exception {
				if (OptWnd.cameraLmaoMessage == 1) {
					OptWnd.cameraLmaoMessage = 2;
					throw (new Exception("Please use the Options menu to change the camera instead."));
				}
				else if (OptWnd.cameraLmaoMessage == 2) {
					OptWnd.cameraLmaoMessage = 3;
					throw (new Exception("No. I said use the Options menu to change the camera!"));
				}
				else if (OptWnd.cameraLmaoMessage == 3) {
					OptWnd.cameraLmaoMessage = 4;
					throw (new Exception("USE THE FUCKING OPTIONS MENU TO CHANGE THE CAMERA!!!"));
				}
				else if (OptWnd.cameraLmaoMessage == 4) {
					OptWnd.cameraLmaoMessage = 1;
					throw (new Exception("I literally disabled this command in case you couldn't tell. Use the Options menu."));
				}
			}
		});


	cmdmap.put("whyload", new Console.Command() {
		public void run(Console cons, String[] args) throws Exception {
		    Loading l = lastload;
		    if(l == null)
			throw(new Exception("Not loading"));
		    l.printStackTrace(cons.out);
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }

    static {
	Console.setscmd("placegrid", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if((plobpgran = Double.parseDouble(args[1])) < 0)
			plobpgran = 0;
		    Utils.setprefd("plobpgran", plobpgran);
		}
	    });
	Console.setscmd("placeangle", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if((plobagran = Double.parseDouble(args[1])) < 2)
			plobagran = 2;
		    Utils.setprefd("plobagran", plobagran);
		}
	    });
	Console.setscmd("clickfuzz", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if((gobclfuzz = Integer.parseInt(args[1])) < 0)
			gobclfuzz = 0;
		}
	    });
	Console.setscmd("clickdb", new Console.Command() {
		public void run(Console cons, String[] args) {
		    clickdb = Utils.parsebool(args[1], false);
		}
	    });
    }
	//ND: Using this "setcam" to change the camera in OptWnd.java. This depends on gameui() inside Widget.java
	public void setcam(String name, String... opts) throws Exception {
		Class<? extends Camera> ct = camtypes.get(name);
		if(ct != null) {
			camera = makecam(ct, opts);
			Utils.setpref("defcam", name);
			Utils.setprefb("camargs", Utils.serialize(opts));
		} else {
			throw(new Exception("no such camera: " + name));
		}
	}
	// ND: These functions are used to snap the camera in NDFree and NDOrtho.
	public void snapCameraNorth() {
		camera.snap(Direction.NORTH);
	}
	public void snapCameraSouth() {
		camera.snap(Direction.SOUTH);
	}
	public void snapCameraWest() {
		camera.snap(Direction.WEST);
	}
	public void snapCameraEast() {
		camera.snap(Direction.EAST);
	}

	public void pfDone(final Pathfinder thread) {
		if (haven.automated.pathfinder.Map.DEBUG_TIMINGS)
			System.out.println("-= PF DONE =-");
	}

	public void registerAreaSelect(AreaSelectCallback callback) {
		this.areaSelectCallback = callback;
	}

	public void unregisterAreaSelect() {
		this.areaSelectCallback = null;
		areaSelect = false;
		selection.destroy();
		selection = null;
	}

	public void pfLeftClickPrecise(Coord mc, String action){
		haven.automated.pathfinder.Map.plbbox = 2;
		pfLeftClick(mc, action);
		haven.automated.pathfinder.Map.plbbox = 3;
	}

	public void pfRightClickPrecise(Gob gob, int meshid, int clickb, int modflags, String action){
		haven.automated.pathfinder.Map.plbbox = 2;
		pfRightClick(gob, meshid, clickb, modflags, action);
		haven.automated.pathfinder.Map.plbbox = 3;
	}

	public void pfLeftClick(Coord mc, String action) {
		try{
		Gob player = player();
		if (player == null)
			return;
		if (mc.dist(player.rc.floor()) > 11 * MAX_TILE_RANGE) {
			Coord between = mc.sub(player.rc.floor());
			double mul = 11 * MAX_TILE_RANGE / mc.dist(player.rc.floor());
			mc = player.rc.floor().add(between.mul(mul));
		}
		synchronized (Pathfinder.class) {
			if (pf != null) {
				pf.terminate = true;
				pfthread.interrupt();
				// cancel movement
				if (player.getattr(Moving.class) != null)
					wdgmsg("gk", 27);
			}

			Coord src = player.rc.floor();
			int gcx = haven.automated.pathfinder.Map.origin - (src.x - mc.x);
			int gcy = haven.automated.pathfinder.Map.origin - (src.y - mc.y);
			if (gcx < 0 || gcx >= haven.automated.pathfinder.Map.sz || gcy < 0 || gcy >= haven.automated.pathfinder.Map.sz)
				return;

			pf = new Pathfinder(this, new Coord(gcx, gcy), action);
			pf.addListener(this);
			pfthread = new Thread(pf, "Pathfinder");
			pfthread.start();
		}
		} catch (Exception e){
			e.getMessage();
		}
	}

	public void pfRightClick(Gob gob, int meshid, int clickb, int modflags, String action) {
		Gob player = player();
		if (player == null)
			return;
		if (gob.rc.dist(player.rc) > 11 * MAX_TILE_RANGE) {
			pfLeftClick(gob.rc.floor(), null);
			return;
		}
		synchronized (Pathfinder.class) {
			if (pf != null) {
				pf.terminate = true;
				pfthread.interrupt();
				// cancel movement
				if (player.getattr(Moving.class) != null)
					wdgmsg("gk", 27);
			}

			Coord src = player.rc.floor();
			int gcx = haven.automated.pathfinder.Map.origin - (src.x - gob.rc.floor().x);
			int gcy = haven.automated.pathfinder.Map.origin - (src.y - gob.rc.floor().y);
			if (gcx < 0 || gcx >= haven.automated.pathfinder.Map.sz || gcy < 0 || gcy >= haven.automated.pathfinder.Map.sz)
				return;

			pf = new Pathfinder(this, new Coord(gcx, gcy), gob, meshid, clickb, modflags, action);
			pf.addListener(this);
			pfthread = new Thread(pf, "Pathfinder");
			pfthread.start();
		}
	}

	public void addCheckpoint(Coord2d coord){
		if(checkpointManager != null && checkpointManagerThread != null){
			checkpointManager.addCoord(coord);
		} else {
			GameUI gameUI = gameui();
			checkpointManager = new CheckpointManager(gameUI);
			Window window = checkpointManager;
			gameUI.add(window, new Coord(gameUI.sz.x/2 - window.sz.x/2 + 100, gameUI.sz.y - window.sz.y));
			checkpointManagerThread = new Thread(checkpointManager, "CheckpointManager");
			checkpointManagerThread.start();
			checkpointManager.addCoord(coord);
		}
	}

	public List<Coord2d> getCheckPointList(){
		if(checkpointManager != null && checkpointManagerThread != null){
			synchronized (checkpointManager.checkpointList){
				return checkpointManager.getAllCoords();
			}
		}
		return null;
	}

	@Override
	public void wdgmsg(String msg, Object... args) {
		boolean safe = true;
		if(MiningSafetyAssistant.preventMiningOutsideSupport){
			Resource curs = ui.root.getcurs(Coord.z);
			if (curs != null && curs.name.equals("gfx/hud/curs/mine") && msg.equals("sel")) {
				safe = MiningSafetyAssistant.isAreaInSupportRange((Coord) args[0], (Coord) args[1], gameui());
			}
		}
		if(safe){
			super.wdgmsg(msg, args);
		} else {
			ui.error("Tile outside all (visible) support range. Preventing mining command");
		}
	}
}
