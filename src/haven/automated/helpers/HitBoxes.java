package haven.automated.helpers;

import haven.*;

import java.util.*;

public class HitBoxes {
    public static Map<String, CollisionBox[]> collisionBoxMap = new HashMap<>();

    private static Set<String> passableGobs = new HashSet<>(Arrays.asList(
            "gfx/terobjs/herbs", "gfx/terobjs/items", "gfx/terobjs/plants", "gfx/terobjs/clue", "gfx/terobjs/boostspeed",
            "gfx/kritter/jellyfish/jellyfish"
    ));

    static {
        //Add missing animals...
        collisionBoxMap.put("gfx/kritter/horse", new CollisionBox[]{new CollisionBox(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/mare", new CollisionBox[]{new CollisionBox(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/stallion", new CollisionBox[]{new CollisionBox(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/foal", new CollisionBox[]{new CollisionBox(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/cattle/calf", new CollisionBox[]{new CollisionBox(
                new Coord(-9, -3), new Coord(9, 3), new Coord(9, -3), new Coord(-9, 3))});
        collisionBoxMap.put("gfx/kritter/cattle/cattle", new CollisionBox[]{new CollisionBox(
                new Coord(-9, -3), new Coord(9, 3), new Coord(9, -3), new Coord(-9, 3))});
        collisionBoxMap.put("gfx/kritter/pig/piglet", new CollisionBox[]{new CollisionBox(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/pig/sow", new CollisionBox[]{new CollisionBox(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/pig/hog", new CollisionBox[]{new CollisionBox(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/goat/nanny", new CollisionBox[]{new CollisionBox(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/goat/billy", new CollisionBox[]{new CollisionBox(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/goat/kid", new CollisionBox[]{new CollisionBox(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/sheep/lamb", new CollisionBox[]{new CollisionBox(
                new Coord(-4, -2), new Coord(5, 2), new Coord(5, -2), new Coord(-4, 2))});
        collisionBoxMap.put("gfx/kritter/sheep/sheep", new CollisionBox[]{new CollisionBox(
                new Coord(-4, -2), new Coord(5, 2), new Coord(5, -2), new Coord(-4, 2))});
    }



    public static void addHitBox(Gob gob){
        Resource res = gob.getres();
        if(collisionBoxMap.get(res.name) == null){
            for (String gobResName : passableGobs) {
                if (res.name.contains(gobResName) && !res.name.contains("trellis")) {
                    collisionBoxMap.put(res.name, new CollisionBox[]{new CollisionBox(false)});
                    return;
                }
            }
            try {
                extractCollisionBoxesFromResource(gob);
            } catch (Loading ignored){}

        }

    }



    private static CollisionBox[] extractCollisionBoxesFromResource(Gob gob) {
        Resource res = gob.getres();
        if (res.name.endsWith("/consobj")) {
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null && rd.sdt.rbuf.length >= 4) {
                MessageBuf buf = rd.sdt.clone();
                return (new CollisionBox[]{new CollisionBox(new Coord(buf.rbuf[0], buf.rbuf[1]), new Coord(buf.rbuf[2], buf.rbuf[3]))});
            }
        }
        try {
            List<Resource.Neg> negs = new ArrayList<>(res.layers(Resource.Neg.class));
            List<Resource.Obstacle> obstacles = new ArrayList<>(res.layers(Resource.Obstacle.class));
            for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
                RenderLink l = link.l;
                if (l instanceof RenderLink.MeshMat) {
                    RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                    addIf(negs, getLayer(Resource.Neg.class, mm.srcres.indir(), mm.mesh));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, mm.srcres.indir(), mm.mesh));
                }
                if (l instanceof RenderLink.AmbientLink) {
                    RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
                    addIf(negs, getLayer(Resource.Neg.class, al.res));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, al.res));
                }
                if (l instanceof RenderLink.Collect) {
                    RenderLink.Collect cl = (RenderLink.Collect) l;
                    addIf(negs, getLayer(Resource.Neg.class, cl.from));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, cl.from));
                }
                if (l instanceof RenderLink.Parameters) {
                    RenderLink.Parameters pl = (RenderLink.Parameters) l;
                    addIf(negs, getLayer(Resource.Neg.class, pl.res));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, pl.res));
                }
            }

            final List<CollisionBox> collisionBoxesList = new ArrayList<>();
            for (Resource.Obstacle o : obstacles) {
                for (int i = 0; i < o.p.length; i++) {
                    boolean hitAble = checkHitAble(gob, o);
                    CollisionBox collisionBox = new CollisionBox(o.p[i], hitAble);
                    collisionBoxesList.add(collisionBox);
                }
            }
            for (Resource.Neg o : negs) {
                boolean hitAble = checkHitAble(gob);
                CollisionBox collisionBox = new CollisionBox(o.bc, o.ac, hitAble, false);
                collisionBoxesList.add(collisionBox);
            }
            boolean isAdd = true;
            if(res.name.endsWith("gate") || res.name.endsWith("/pow")){
                isAdd = false;
            }
            CollisionBox[] collisionBoxes = collisionBoxesList.toArray(new CollisionBox[0]);
            if (isAdd) {
                collisionBoxMap.put(res.name, collisionBoxes);
            }
            return (collisionBoxes);
        } catch (Exception ignore) {
        }
        return null;
    }

    public static <T extends Resource.Layer> void addIf(final List<T> end, final List<T> start) {
        for (T l : start)
            if (!end.contains(l))
                end.add(l);
    }

    @SafeVarargs
    public static <T extends Resource.Layer> List<T> getLayer(final Class<T> layer, Indir<Resource>... reses) {
        final List<T> list = new ArrayList<>();
        for (Indir<Resource> ires : reses) {
            if (ires != null) {
                Resource res = ires.get();
                if (res != null) {
                    T l = res.layer(layer);
                    if (l != null)
                        list.add(layer.cast(l));
                }
            }
        }
        return (list);
    }

    public static boolean checkHitAble(Gob gob, Resource.Obstacle obst) {
        if (gob.getres() != null) {
            String id = obst.id;
            if (id != null)
                if (id.equals("build") || id.equals("ext")) {
                    return (false);
                }
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null)
                return !gob.getres().name.endsWith("gate") || rd.sdt.peekrbuf(0) != 1;
        }
        return (true);
    }

    public static boolean checkHitAble(Gob gob) {
        if (gob.getres() != null) {
            Resource res = gob.getres();
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null)
                if (res.name.endsWith("/pow") && (rd.sdt.peekrbuf(0) == 33 || rd.sdt.peekrbuf(0) == 17))
                    return (false);
            if (res.name.equals("gfx/terobjs/vflag"))
                return (false);
        }
        return (true);
    }




    public static class CollisionBox {
        private Coord2d[] coords;
        private boolean hitAble;

        public CollisionBox(Coord a, Coord b) {
            this.coords = new Coord2d[] {
                    new Coord2d(a.x, a.y),
                    new Coord2d(b.x, b.y)
            };
            this.hitAble = true;
        }

        public CollisionBox(Coord a, Coord b, Coord c, Coord d) {
            this.coords = new Coord2d[] {
                    new Coord2d(a.x, a.y),
                    new Coord2d(b.x, b.y),
                    new Coord2d(c.x, c.y),
                    new Coord2d(d.x, d.y)
            };
            this.hitAble = true;
        }

        public CollisionBox(boolean hitAble) {
            this.coords = new Coord2d[0];
            this.hitAble = hitAble;
        }

        public CollisionBox(Coord2d[] coords, boolean hitAble) {
            this.coords = coords;
            this.hitAble = hitAble;
        }

        public CollisionBox(final Coord off, final Coord br, boolean hitAble, boolean buffer) {
            Coord ac = !buffer ? off : off.add(2, 2);
            Coord bc = !buffer ? br : br.add(4, 4);
            this.coords = new Coord2d[]{
                    new Coord2d(ac.x, -ac.y), new Coord2d(bc.x, -ac.y), new Coord2d(bc.x, -bc.y), new Coord2d(ac.x, -bc.y)
            };
            this.hitAble = hitAble;
        }

        @Override
        public String toString() {
            StringBuilder coordsStr = new StringBuilder();
            for (Coord2d coord : coords) {
                coordsStr.append("(").append(coord.x).append(", ").append(coord.y).append("), ");
            }
            if (coordsStr.length() > 0) {
                coordsStr.setLength(coordsStr.length() - 2);
            }

            return "CollisionBox{" +
                    "coords=[" + coordsStr + "]" +
                    ", hitAble=" + hitAble + ", size: " + coords.length +
                    "}";
        }
    }
}
