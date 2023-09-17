package haven;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GobHealthInfo extends GobInfo {
    public static Color BG = new Color(0, 0, 0, 0);

    private GobHealth health;

    public GobHealthInfo(Gob owner) {
        super(owner);
    }

    @Override
    protected boolean enabled() {
        return OptWnd.toggleGobHealthDisplayCheckBox.a;
    }

    @Override
    protected Tex render() {
        if(gob == null || gob.getres() == null) { return null;}

        BufferedImage health = health();

        if(health == null) {
            return null;
        }

        return new TexI(ItemInfo.catimgsh(3, 0, BG, health));
    }

    @Override
    public void dispose() {
        health = null;
        super.dispose();
    }

    private BufferedImage health() {
        health = gob.getattr(GobHealth.class);
        if(health != null) {
            return health.text();
        }

        return null;
    }

    @Override
    public String toString() {
        Resource res = gob.getres();
        return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }
}
