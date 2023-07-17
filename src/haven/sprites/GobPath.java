package haven.sprites;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GobPath extends Sprite {
    private final Model line;

    private static final VertexArray.Layout LAYOUT = new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex, new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12));
    public static final Color MAINCOLOR = new Color(233, 185, 110);
    public static final Color FOECOLOR = new Color(191, 7, 53, 120);
    public static final Color FRIENDCOLOR = new Color(7, 191, 38, 120);
    private final Color color;
    private final Moving mov;
    private static final float SOLID_WIDTH = UI.scale(10f);
    private static final Pipe.Op TOP = Pipe.Op.compose(Rendered.last, States.Depthtest.none, States.maskdepth);
    private final Pipe.Op solid;
    private final Pipe.Op solidtop;

    public GobPath(Gob gob, Homing mov, Color color) {
        super(gob, null);
        this.color = color;
        this.mov = mov;
        this.solid = Pipe.Op.compose(new BaseColor(color), new States.LineWidth(SOLID_WIDTH));
        this.solidtop = Pipe.Op.compose(solid, TOP);
        line = new Model(Model.Mode.LINES, new VertexArray(LAYOUT, new VertexArray.Buffer[]{new VertexArray.Buffer(this.line(), DataBuffer.Usage.STREAM)}), (Model.Indices) null);
    }

    private ByteBuffer line() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        byteBuffer.order(ByteOrder.nativeOrder());

        float xr = 0f;
        float yr = 0f;
        float z = 0f;
        
        try {
            float lcx = 0;
            float lcy = 0;
            Gob gob = (Gob) owner;

            Coord3f pc = gob.getc();
             if (mov instanceof Homing) {
                Coord2d c = ((Homing) mov).tc;
                lcx = (float) c.x;
                lcy = (float) c.y;
            } else if (mov instanceof Following) {
                Coord2d c = mov.gob.rc;
                lcx = (float) c.x;
                lcy = (float) c.y;
            }


            float x = lcx - pc.x;
            float y = -lcy + pc.y;
            z = OptWnd.flatWorldSetting || Math.sqrt(x * x + y * y) >= 44 * 11 ? 0 : gob.glob.map.getcz(lcx, lcy) - pc.z;

            //We need to add a line from 0,0,0 to where we clicked, but since the gob is rotated, we need to unrotate this sprite
            xr = (float) (x * Math.cos(gob.a) - y * Math.sin(gob.a));
            yr = (float) (x * Math.sin(gob.a) + y * Math.cos(gob.a));

            
        } catch (Exception ignored) {
        }

        byteBuffer.putFloat(0.0f).putFloat(0.0f).putFloat(0.0f);
        byteBuffer.putFloat(xr).putFloat(yr).putFloat(z);
        byteBuffer.flip();
        
        return byteBuffer;
    }

    public void added(final RenderTree.Slot slot) {
        slot.ostate(solidtop);
        slot.add((RenderTree.Node) this.line);
    }

    public void gtick(final Render render) {
        render.update((DataBuffer) this.line.va.bufs[0], DataBuffer.Filler.of(this.line()));
    }
}
