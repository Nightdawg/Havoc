package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobHighlight extends GAttrib implements Gob.SetupMod {
    private final Color c;
    private static final long cycle = 1200;
    private static final long duration = 7200;
    private long start = 0;

    public GobHighlight(Gob g, Color c) {
        super(g);
        this.c = c;
    }

    public void start() {
        start = System.currentTimeMillis();
    }

    public Pipe.Op gobstate() {
        long active = System.currentTimeMillis() - start;
        if(active > duration) {
            return null;
        } else {
            float k = (float) Math.abs(Math.sin(Math.PI * active / cycle));
            return new MixColor(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * k));
        }
    }
}
