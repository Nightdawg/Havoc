package haven.sprites;

import haven.*;
import haven.sprites.baseSprite.ColoredCircleSprite;

import java.awt.*;

public class AuraCircleSprite extends ColoredCircleSprite {
    public static final Color redr = new Color(192, 0, 0, 140);
    public static final Color bluer = new Color(22, 67, 219, 140);
    public static Color rabbitAuraColor = OptWnd.rabbitAuraColorOptionWidget.currentColor;
    public static Color speedbuffAuraColor = OptWnd.speedbuffAuraColorOptionWidget.currentColor;
    public static final Color darkgreen = new Color(0, 121, 12, 128);
    public static final Color orange = new Color(255, 136, 0, 128);
    public static final Color yellow = new Color(255, 242, 0, 128);
    public static Color genericCritterAuraColor = OptWnd.genericCritterAuraColorOptionWidget.currentColor;

    public AuraCircleSprite(final Gob g, final Color col) {
        super(g, col, 0f, 10f, 0.45f);
    }
    public AuraCircleSprite(final Gob g, final Color col, float size) {
        super(g, col, 0f, size, 0.45f);
    }
}