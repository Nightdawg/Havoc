package haven.sprites;

import haven.sprites.mesh.ColoredCircleMesh;
import haven.BuddyWnd;
import haven.Gob;
import haven.Sprite;
import haven.render.BaseColor;
import haven.render.RenderTree;

import java.awt.*;


public class AggroMark extends Sprite {
    public static final int id = -4214129;

    private static final Color col = new Color(255, 0, 0, 140);
    private final ColoredCircleMesh mesh;
    private boolean alive = true;

    public AggroMark(final Gob g) {
        super(g, null);
        this.mesh = ColoredCircleMesh.getmesh(col, 3.5f, 5.6f, 0.55f);
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
        slot.add(mesh, new BaseColor(Color.RED));
    }
}
