package haven;

import haven.render.*;
import haven.res.lib.tree.TreeScale;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class CollisionBox extends SlottedNode implements Rendered {
	private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
	public Model model;
	private final Gob gob;
	private static final Map<Resource, Model> MODEL_CACHE = new HashMap<>();
	private static final float Z = 0.1f;
	public static final float WIDTH = 2f;
	public static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
	public static Pipe.Op SOLID_TOP = Pipe.Op.compose(new BaseColor(OptWnd.collisionBoxesColorOptionWidget.currentColor), new States.LineWidth(WIDTH), TOP);
	public static Pipe.Op CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.closedGateColorSetting[0]), Integer.parseInt(OptWnd.closedGateColorSetting[1]),
			Integer.parseInt(OptWnd.closedGateColorSetting[2]), Integer.parseInt(OptWnd.collisionBoxesColorSetting[3]))), new States.LineWidth(WIDTH), TOP);
	public static Pipe.Op OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.openVisitorGateNoCombatColorSetting[0]), Integer.parseInt(OptWnd.openVisitorGateNoCombatColorSetting[1]),
			Integer.parseInt(OptWnd.openVisitorGateNoCombatColorSetting[2]), Integer.parseInt(OptWnd.collisionBoxesColorSetting[3]))), new States.LineWidth(WIDTH), TOP);
	public static Pipe.Op OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.openVisitorGateInCombatColorSetting[0]), Integer.parseInt(OptWnd.openVisitorGateInCombatColorSetting[1]),
			Integer.parseInt(OptWnd.openVisitorGateInCombatColorSetting[2]), Integer.parseInt(OptWnd.collisionBoxesColorSetting[3]))), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last, TOP);
	public static Pipe.Op PASSABLE_TOP = Pipe.Op.compose(new BaseColor(new Color(Integer.parseInt(OptWnd.passableGateCombatColorSetting[0]), Integer.parseInt(OptWnd.passableGateCombatColorSetting[1]),
			Integer.parseInt(OptWnd.passableGateCombatColorSetting[2]), Integer.parseInt(OptWnd.collisionBoxesColorSetting[3]))), new States.LineWidth(WIDTH), TOP);
	private Pipe.Op state = SOLID_TOP;
	private boolean issaGate = false;
	private boolean issaVisitorGate = false;

	private CollisionBox(Gob gob) {
		model = getModel(gob);
		this.gob = gob;
		updateState();
	}

	public static CollisionBox forGob(Gob gob) {
		try {
			return new CollisionBox(gob);
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
			//Pipe.Op newState = passable() ? (top ? PASSABLE_TOP : PASSABLE) : (top ? (issaGate ? CLOSEDGATE_TOP : SOLID_TOP) : (issaGate ? CLOSEDGATE : SOLID));
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
			}catch (Loading e) {CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}
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
						for (Gob.Overlay ol : gob.ols) {
							String oname = gob.glob.sess.getres(Utils.uint16d(ol.sdt.rbuf, 0)).get().basename();
							if (oname.equals("visflag"))
								issaVisitorGate = true;
						}
					} catch (NullPointerException e) {CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}
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
		} catch (Loading e) {CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}
		return false;
	}

	private static Model getModel(Gob gob) {
		Model model = null;
		Coord bboxa = new Coord(0,0);
		Coord bboxb = new Coord(0,0);
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

						bboxa.x = neg.ac.x;
						bboxa.y = neg.ac.y;
						bboxb.x = neg.bc.x;
						bboxb.y = neg.bc.y;
						polygons.add(box);
					}
				}

				Collection<Resource.Obstacle> obstacles = res.layers(Resource.Obstacle.class);
				if(obstacles != null) {
					Optional<Coord2d> minX;
					Optional<Coord2d> minY;
					Optional<Coord2d> maxX;
					Optional<Coord2d> maxY;
					for (Resource.Obstacle obstacle : obstacles) {
						if("build".equals(obstacle.id)) {continue;}
						for (Coord2d[] polygon : obstacle.p) {
							minX = Arrays.stream(polygon)
									.min(Comparator.comparingDouble(Coord2d::getX));
							minY = Arrays.stream(polygon)
									.min(Comparator.comparingDouble(Coord2d::getY));

							maxX = Arrays.stream(polygon)
									.max(Comparator.comparingDouble(Coord2d::getX));
							maxY = Arrays.stream(polygon)
									.max(Comparator.comparingDouble(Coord2d::getY));

							bboxa.x = (int) minX.get().getX();
							bboxa.y = (int) minY.get().getX();
							bboxb.x = (int) maxX.get().getX();
							bboxb.y = (int) maxY.get().getY();
							polygons.add(Arrays.stream(polygon)
									.map(coord2d -> new Coord3f((float) coord2d.x, (float) -coord2d.y, Z))
									.collect(Collectors.toList()));
						}
					}
				}
				if (polygons.isEmpty()){
					List<List<Coord3f>> polygons2 = new LinkedList<>();
					List<Coord3f> box = new LinkedList<>();
					float ax = 0, bx = 0, ay = 0, by = 0;
					if (res.name.startsWith("gfx/kritter/cattle/calf")) {
						ax = -9F; bx = 9F; ay = -3F; by = 3F;
					} else if (res.name.startsWith("gfx/kritter/sheep/lamb")) {
						ax = -4F; bx = 5F; ay = -2F; by = 2F;
					} else if (res.name.startsWith("gfx/kritter/goat/")) {
						ax = -3F; bx = 4F; ay = -2F; by = 2F;
					} else if (res.name.startsWith("gfx/kritter/pig/")) {
						ax = -6F; bx = 6F; ay = -3F; by = 3F;
					} else if (res.name.startsWith("gfx/kritter/horse/")) {
						ax = -8F; bx = 8F; ay = -4F; by = 4F;
					}
					if (ax != 0 && bx != 0 && ay != 0 && by != 0) {
						box.add(new Coord3f(ax, -ay, Z));
						box.add(new Coord3f(bx, -ay, Z));
						box.add(new Coord3f(bx, -by, Z));
						box.add(new Coord3f(ax, -by, Z));
						bboxa.x = (int)ax;
						bboxa.y = (int)ay;
						bboxb.x = (int)bx;
						bboxb.y = (int)by;
						polygons2.add(box);
						List<Float> vertices = new LinkedList<>();
						for (List<Coord3f> polygon : polygons2) {
							addLoopedVertices(vertices, polygon);
						}
						float[] data = convert(vertices);
						VertexArray.Buffer vbo = new VertexArray.Buffer(data.length * 4, DataBuffer.Usage.STATIC, DataBuffer.Filler.of(data));
						VertexArray va = new VertexArray(LAYOUT, vbo);
						model = new Model(Model.Mode.LINES, va, null);
						model.bbox = new Model.BoundingBox(bboxa, bboxb);
						if (!growingTreeOrBush)
							MODEL_CACHE.put(res, model);
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
					model.bbox = new Model.BoundingBox(bboxa, bboxb);
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
