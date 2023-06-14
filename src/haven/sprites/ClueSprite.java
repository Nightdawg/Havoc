package haven.sprites;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;
import haven.map.MapSprite;

import java.awt.*;

public class ClueSprite extends MapSprite {
    private static final int ARM_LENGTH = 500;
    private static final Color CLUE_COLOR = new Color(255, 255, 255);
    private final double a1;
    private final double a2;
    private double timetolive;

    public ClueSprite(Coord2d rc, double a1, double a2) {
        this.rc = rc;
        this.a1 = -a1;
        this.a2 = -a2;
        timetolive = 100;
    }
    
    @Override
    public void draw(GOut g, Coord pos, double zoomlevel) {
        Coord arm1pos = new Coord((int) (Math.cos(a1)*ARM_LENGTH), (int) (Math.sin(a1)*ARM_LENGTH)).add(pos);
        Coord arm2pos = new Coord((int) (Math.cos(a2)*ARM_LENGTH), (int) (Math.sin(a2)*ARM_LENGTH)).add(pos);
        g.chcolor(CLUE_COLOR);
        g.clippedLine(pos, arm1pos, 2);
        g.clippedLine(pos, arm2pos, 2);
        g.clippedLine(arm1pos, arm2pos, 2);
    }

    @Override
    public boolean tick(double dt) {
        timetolive -= dt*1.;
        return timetolive <= 0;
    }
}
