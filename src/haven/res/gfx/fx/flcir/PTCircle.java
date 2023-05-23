package haven.res.gfx.fx.flcir;

import haven.Gob;
import haven.Sprite;
import haven.render.RenderTree;

import java.awt.*;

public class PTCircle extends Sprite {

    public static final int id = -42141286;

    private ColoredCircleMesh mesh;
    public Color col;
    private boolean alive = true;

    public PTCircle(final Gob g, final Color col) {
        super(g, null);
        super.partyMemberColor = col;
        this.col = col;
        this.mesh = ColoredCircleMesh.getmesh(col, 3.5f, 5.6f, 0.5f);
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