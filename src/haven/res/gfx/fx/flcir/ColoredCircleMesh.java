package haven.res.gfx.fx.flcir;

import haven.FastMesh;
import haven.MapMesh;
import haven.Utils;
import haven.VertexBuf;
import haven.render.Pipe;
import haven.render.RenderTree;
import haven.render.States;
import haven.render.VertexColor;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class ColoredCircleMesh extends FastMesh {
    //one global instance for everything to share off of
    private static final Map<Color, ColoredCircleMesh> sprmap = new HashMap<>();

    private ColoredCircleMesh(VertexBuf buf, ShortBuffer sa) {
        super(buf, sa);
    }

    public void added(RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(MapMesh.postmap,
                new VertexColor(),
                new States.Facecull(States.Facecull.Mode.NONE)));
    }


    public synchronized static ColoredCircleMesh getmesh(final Color col) {
        if(sprmap.containsKey(col))
            return sprmap.get(col);
        else {
            final float[] circlecol = Utils.c2fa(col);
            final float outerrad = 10f; //Outer distance of the circle
            final float innerrad = 0f;  //Inner distance of the circle
            final double step = Math.PI / 64;
            final int verts = 4 * 64;
            //Height of our path relative to gob z position, 17f should be above their head
            final float h = 0.5f;


            //Position buffer, outer - inner - outer - ...
            //ends with an inner, start with an outer
            final FloatBuffer pa = Utils.mkfbuf(verts * 3 * 2);
            final FloatBuffer na = Utils.mkfbuf(verts * 3 * 2);    //normal, don't care about
            final FloatBuffer cl = Utils.mkfbuf(verts * 4 * 2);    //color buffer
            //How to draw it. each tri will be 3 points. 2 from the outer, 1 form the inner.
            final ShortBuffer sa = Utils.mksbuf(verts * 3 * 2);


            double rad = 0;
            for (int start = 0; start < verts; ++start) {
                final float angx = (float) (Math.cos(rad)), angy = (float) (Math.sin(rad));
                final float ox = angx * outerrad, oy = angy * outerrad;
                final float ix = angx * innerrad, iy = angy * innerrad;
                pa.put(ox).put(oy).put(h);
                na.put(ox).put(oy).put(h);
                cl.put(circlecol[0]).put(circlecol[1]).put(circlecol[2]).put(circlecol[3]);
                pa.put(ix).put(iy).put(h);
                na.put(ix).put(iy).put(h);
                cl.put(circlecol[0]).put(circlecol[1]).put(circlecol[2]).put(circlecol[3]);
                rad += step;
            }

            for (int start = 0; start < verts; start += 2) {
                sa.put((short) start).put((short) (start + 1)).put((short) ((start + 2) % verts));
                sa.put((short) (start + 1)).put((short) ((start + 2) % verts)).put((short) ((start + 3) % verts));
            }

            sprmap.put(col, new ColoredCircleMesh(new VertexBuf(new VertexBuf.VertexData(pa),
                    new VertexBuf.NormalData(na),
                    new VertexBuf.ColorData(cl)),
                    sa));
            return sprmap.get(col);
        }
    }

    public synchronized static ColoredCircleMesh getmesh(final Color col, final float innerrad, final float outerrad, final float height) {
        if(sprmap.containsKey(col))
            return sprmap.get(col);
        else {
            final float[] circlecol = Utils.c2fa(col);
            //final float outerrad = 10f; //Outer distance of the circle
            //final float innerrad = 0f;  //Inner distance of the circle
            final double step = Math.PI / 64;
            final int verts = 4 * 64;
            //Height of our path relative to gob z position, 17f should be above their head
//            final float h = 0.5f;


            //Position buffer, outer - inner - outer - ...
            //ends with an inner, start with an outer
            final FloatBuffer pa = Utils.mkfbuf(verts * 3 * 2);
            final FloatBuffer na = Utils.mkfbuf(verts * 3 * 2);    //normal, don't care about
            final FloatBuffer cl = Utils.mkfbuf(verts * 4 * 2);    //color buffer
            //How to draw it. each tri will be 3 points. 2 from the outer, 1 form the inner.
            final ShortBuffer sa = Utils.mksbuf(verts * 3 * 2);


            double rad = 0;
            for (int start = 0; start < verts; ++start) {
                final float angx = (float) (Math.cos(rad)), angy = (float) (Math.sin(rad));
                final float ox = angx * outerrad, oy = angy * outerrad;
                final float ix = angx * innerrad, iy = angy * innerrad;
                pa.put(ox).put(oy).put(height);
                na.put(ox).put(oy).put(height);
                cl.put(circlecol[0]).put(circlecol[1]).put(circlecol[2]).put(circlecol[3]);
                pa.put(ix).put(iy).put(height);
                na.put(ix).put(iy).put(height);
                cl.put(circlecol[0]).put(circlecol[1]).put(circlecol[2]).put(circlecol[3]);
                rad += step;
            }

            for (int start = 0; start < verts; start += 2) {
                sa.put((short) start).put((short) (start + 1)).put((short) ((start + 2) % verts));
                sa.put((short) (start + 1)).put((short) ((start + 2) % verts)).put((short) ((start + 3) % verts));
            }

            sprmap.put(col, new ColoredCircleMesh(new VertexBuf(new VertexBuf.VertexData(pa),
                    new VertexBuf.NormalData(na),
                    new VertexBuf.ColorData(cl)),
                    sa));
            return sprmap.get(col);
        }
    }
}
