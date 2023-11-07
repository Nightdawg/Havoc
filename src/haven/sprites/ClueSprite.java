package haven.sprites;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;
import haven.map.MapSprite;

import java.awt.*;

public class ClueSprite extends MapSprite {
    private final int length;
    private final int width;
    private static final Color CLUE_COLOR = new Color(255, 255, 255);
    private final double a1;
    private final double a2;
    private double timetolive;

    public ClueSprite(Coord2d rc, double a1, double a2, int width, int length) {
        this.rc = rc;
        this.a1 = -a1;
        this.a2 = -a2;
        this.width = width;
        this.length = length;
        timetolive = 100;
    }
    
    @Override
    public void draw(GOut g, Coord pos, double zoomlevel) {
        Coord arm1pos = new Coord((int) (Math.cos(a1)*length), (int) (Math.sin(a1)*length)).add(pos);
        Coord arm2pos = new Coord((int) (Math.cos(a2)*length), (int) (Math.sin(a2)*length)).add(pos);
        g.chcolor(CLUE_COLOR);
        g.clippedLine(pos, arm1pos, width);
        g.clippedLine(pos, arm2pos, width);
        g.clippedLine(arm1pos, arm2pos, width);
    }

    @Override
    public boolean tick(double dt) {
        timetolive -= dt*1.;
        return timetolive <= 0;
    }
}
