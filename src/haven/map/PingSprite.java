package haven.map;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;

import java.awt.*;

public class PingSprite extends MapSprite {
    private static final double RADIUS = 14;
    private static final Color BACKGROUND = new Color(255, 255, 255, 210);

    private Color col;
    private double timetolive;

    public PingSprite(Coord2d rc, Color col, int timetolive) {
        this.rc = rc;
        this.col = col;
        this.timetolive = timetolive;
    }
    
    @Override
    public void draw(GOut g, Coord pos, double zoomlevel) {
        if (!pos.isect(Coord.z, g.sz())) {
            g.clipCoord(pos, Coord.z.add((int)RADIUS/2,(int)RADIUS/2), g.sz().sub((int)RADIUS/2, (int)RADIUS/2));
        }
        g.chcolor(BACKGROUND);
        g.fcircle(pos.x, pos.y, RADIUS, 10);
        g.chcolor(col);
        g.circle(pos.x, pos.y, (9*timetolive+10)%RADIUS, 10, 2);
        g.chcolor(col);
        g.circle(pos.x, pos.y, (9*timetolive+5)%RADIUS, 10, 2);
        g.chcolor(col);
        g.circle(pos.x, pos.y, (9*timetolive)%RADIUS, 10, 2);
    }

    @Override
    public boolean tick(double dt) {
        timetolive -= dt*1.;
        return timetolive <= 0;
    }
}
