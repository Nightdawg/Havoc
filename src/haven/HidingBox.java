package haven;

import haven.render.*;
import haven.res.lib.tree.TreeScale;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class HidingBox extends SlottedNode implements Rendered {
	private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
	private Model model;
	private final Gob gob;
	private static final Map<Resource, Model> MODEL_CACHE = new HashMap<>();
	private static final float Z = 0.1f;
	public static final float WIDTH = 2f;
	public static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
	public static Pipe.Op SOLID_TOP = Pipe.Op.compose(Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.hiddenObjectsColorSetting[0]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[1]), Integer.parseInt(OptWnd.hiddenObjectsColorSetting[2]), 140)), new States.LineWidth(WIDTH)), TOP);
	public static Pipe.Op CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(OptWnd.closedGateColorOptionWidget.currentColor), new States.LineWidth(WIDTH), TOP);
	public static Pipe.Op OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(OptWnd.openVisitorGateNoCombatColorOptionWidget.currentColor), new States.LineWidth(WIDTH), TOP);
	public static Pipe.Op OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(OptWnd.openVisitorGateInCombatColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last, TOP);
	public static Pipe.Op PASSABLE_TOP = Pipe.Op.compose(new BaseColor(OptWnd.passableGateCombatColorOptionWidget.currentColor), new States.LineWidth(WIDTH), TOP);
	private Pipe.Op state = SOLID_TOP;
	private boolean issaGate = false;
	private boolean issaVisitorGate = false;

	private HidingBox(Gob gob) {
		model = getModel(gob);
		this.gob = gob;
		updateState();
	}

	public static HidingBox forGob(Gob gob) {
		try {
			return new HidingBox(gob);
		} catch (Loading ignored) { }
		return null;
	}

	@Override
	public void added(RenderTree.Slot slot) {
		super.added(slot);
		slot.ostate(state);
		updateState();
	}

	@Override
	public void draw(Pipe context, Render out) {
		if(model != null) {
			out.draw(context, model);
		}
	}

	public void updateState() {
		if(model != null && slots != null) {
			Pipe.Op newState;
			boolean inCombat = gob.glob.sess.ui.gui.fv != null && gob.glob.sess.ui.gui.fv.current != null;
			if (passable()){
				if (issaVisitorGate) newState = (inCombat) ? OPENVISITORGATE_TOP_InCombat : OPENVISITORGATE_TOP_NoCombat; else newState = PASSABLE_TOP;
			} else {
				if (issaGate) newState = CLOSEDGATE_TOP; else newState = SOLID_TOP;
			}
			try {
				Model m = getModel(gob);
				if(m != null && m != model) {
					model = m;
					slots.forEach(RenderTree.Slot::update);
				}
			}catch (Loading ignored) {}
			if(newState != state) {
				state = newState;
				for (RenderTree.Slot slot : slots) {
					slot.ostate(state);
				}
			}
		}
	}

	private boolean passable() {
		try {
			String name = gob.resid();
			ResDrawable rd = (gob.getattr(Drawable.class) instanceof ResDrawable) ? (ResDrawable) gob.getattr(Drawable.class) : null;

			if(rd != null) {
				int state = gob.sdt();
				if(name.endsWith("gate") && name.startsWith("gfx/terobjs/arch")) {//gates
					issaGate = true;
					try {
						for (Gob.Overlay ol : gob.ols.values()) {
							String oname = gob.glob.sess.getres(Utils.uint16d(ol.sdt.rbuf, 0)).get().basename();
							if (oname.equals("visflag"))
								issaVisitorGate = true;
						}
					} catch (NullPointerException ignored) {}
					if(state == 1) { // gate is open
						return true;
					}
				} else if(name.endsWith("/dng/antdoor")) {
					return state == 1 || state == 13;
				} else if(name.equals("gfx/terobjs/pow[hearth]")) {//hearth fire
					return true;
				} else if(name.equals("gfx/terobjs/arch/cellardoor") || name.equals("gfx/terobjs/fishingnet")) {
					return true;
				}
			}
		} catch (Loading ignored) {}
		return false;
	}

	private static Model getModel(Gob gob) {
		Model model = null;
		Resource res = getResource(gob);
		TreeScale treeScale = null;
		float boxScale = 1.0f;
		boolean growingTreeOrBush = false;
		if ((res.name.startsWith("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk")) || res.name.startsWith("gfx/terobjs/bushes")) {
			treeScale = gob.getattr(TreeScale.class);
			if (treeScale != null) {
				if (treeScale.scale != 1.0f) {
					boxScale = 1f / treeScale.scale;
					growingTreeOrBush = true;
				}
			}
		}
		synchronized (MODEL_CACHE) {
			if (!growingTreeOrBush)
				model = MODEL_CACHE.get(res);
			if(model == null) {
				List<List<Coord3f>> polygons = new LinkedList<>();

				Collection<Resource.Neg> negs = res.layers(Resource.Neg.class);
				if(negs != null) {
					for (Resource.Neg neg : negs) {
						List<Coord3f> box = new LinkedList<>();
						box.add(new Coord3f(neg.ac.x*boxScale, -neg.ac.y*boxScale, Z));
						box.add(new Coord3f(neg.bc.x*boxScale, -neg.ac.y*boxScale, Z));
						box.add(new Coord3f(neg.bc.x*boxScale, -neg.bc.y*boxScale, Z));
						box.add(new Coord3f(neg.ac.x*boxScale, -neg.bc.y*boxScale, Z));

						polygons.add(box);
					}
				}

				Collection<Resource.Obstacle> obstacles = res.layers(Resource.Obstacle.class);
				if(obstacles != null) {
					for (Resource.Obstacle obstacle : obstacles) {
						if("build".equals(obstacle.id)) {continue;}
						for (Coord2d[] polygon : obstacle.p) {
							polygons.add(Arrays.stream(polygon)
									.map(coord2d -> new Coord3f((float) coord2d.x, (float) -coord2d.y, Z))
									.collect(Collectors.toList()));
						}
					}
				}
				if(!polygons.isEmpty()) {
					List<Float> vertices = new LinkedList<>();

					for (List<Coord3f> polygon : polygons) {
						addLoopedVertices(vertices, polygon);
					}

					float[] data = convert(vertices);
					VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
					VertexArray va = new VertexArray(LAYOUT, vbo);

					model = new Model(Model.Mode.LINES, va, null);
					if (!growingTreeOrBush)
						MODEL_CACHE.put(res, model);
				}
			}
		}
		return model;
	}

	private static float[] convert(List<Float> list) {
		float[] ret = new float[list.size()];
		int i = 0;
		for (Float value : list) {
			ret[i++] = value;
		}
		return ret;
	}

	private static void addLoopedVertices(List<Float> target, List<Coord3f> vertices) {
		int n = vertices.size();
		for (int i = 0; i < n; i++) {
			Coord3f a = vertices.get(i);
			Coord3f b = vertices.get((i + 1) % n);
			Collections.addAll(target, a.x, a.y, a.z);
			Collections.addAll(target, b.x, b.y, b.z);
		}
	}

	private static Resource getResource(Gob gob) {
		Resource res = gob.getres();
		if(res == null) {throw new Loading();}
		Collection<RenderLink.Res> links = res.layers(RenderLink.Res.class);
		for (RenderLink.Res link : links) {
			if(link.l instanceof RenderLink.MeshMat) {
				RenderLink.MeshMat mesh = (RenderLink.MeshMat) link.l;
				return mesh.mesh.get();
			}
		}
		return res;
	}
}
