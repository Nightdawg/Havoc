package haven.map;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;

public abstract class MapSprite {
    public Coord2d rc;
    
    public abstract void draw(GOut g, Coord div, double zoomlevel);

    public abstract boolean tick(double dt);
}
