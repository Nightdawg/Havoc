package haven.sprites;

import haven.Gob;
import haven.sprites.baseSprite.ColoredCircleSprite;

import java.awt.*;

public class ArcheryRadiusSprite extends ColoredCircleSprite {

    private static final Color col = new Color(255, 0, 0, 140);

    public ArcheryRadiusSprite(final Gob g, final float range) {
        super(g, col, range-2, range, 1.1F);
    }
}