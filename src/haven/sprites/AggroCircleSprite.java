package haven.sprites;

import haven.OptWnd;
import haven.sprites.baseSprite.ColoredCircleSprite;
import haven.Gob;
import haven.render.RenderTree;
import java.awt.*;


public class AggroCircleSprite extends ColoredCircleSprite {
    public static final int id = -4214129;
    public static Color col = OptWnd.aggroedEnemiesColorOptionWidget.currentColor;
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
