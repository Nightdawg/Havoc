package haven.res.gfx.fx.flcir;

import haven.*;
import haven.render.*;

import java.awt.*;

public class FLCir extends Sprite {
    public static final Color redr = new Color(192, 0, 0, 140);
    public static final Color bluer = new Color(22, 67, 219, 140);
    public static final Color yelr = new Color(248, 210, 0, 140);
    public static final Color gren = new Color(88, 255, 0, 140);
    public static final Color purp = new Color(193, 0, 255, 140);

    public static final int id = -42141286;

    private ColoredCircleMesh mesh;
    private Color col;
    private boolean alive = true;

    public FLCir(final Gob g, final Color col) {
        super(g, null);
        this.col = col;
        this.mesh = ColoredCircleMesh.getmesh(col);
    }

    public void rem() {
        alive = false;
    }

    @Override
    public boolean tick(double ddt) {
        return !alive;
    }

    @Override
    public void added(RenderTree.Slot slot) {
        super.added(slot);
        slot.add(mesh);
    }
}