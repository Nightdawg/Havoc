package haven.sprites;

import haven.sprites.baseSprite.ColoredCircleSprite;
import haven.Gob;
import haven.render.RenderTree;
import java.awt.*;


public class AggroCircleSprite extends ColoredCircleSprite {
    public static final int id = -4214129;
    private static final Color col = new Color(255, 0, 0, 140);
    private boolean alive = true;

    public AggroCircleSprite(final Gob g) {
        super(g, col, 4.6f, 6.1f, 0.5f);
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
    }
}
