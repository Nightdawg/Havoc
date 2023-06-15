package haven.sprites;

import haven.*;
import haven.sprites.baseSprite.ColoredCircleSprite;

import java.awt.*;

public class AuraCircleSprite extends ColoredCircleSprite {
    public static final Color redr = new Color(192, 0, 0, 140);
    public static final Color bluer = new Color(22, 67, 219, 140);
    public static final Color yelr = new Color(248, 210, 0, 140);
    public static final Color gren = new Color(88, 255, 0, 140);
    public static final Color purp = new Color(193, 0, 255, 140);

    public AuraCircleSprite(final Gob g, final Color col) {
        super(g, col, 0f, 10f, 0.45f);
    }
}