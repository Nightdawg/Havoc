/* Preprocessed source code */
package haven.res.ui.tt.level;

import haven.*;
import java.awt.Color;

/* >tt: Level */
@haven.FromResource(name = "ui/tt/level", version = 21)
public class Level extends ItemInfo implements GItem.OverlayInfo<Double> {
    public static final Color defcolor = Color.WHITE;
    public static final int h = UI.scale(2);
    public static final int m = UI.scale(1);
    public final double max, cur;
    public final Color color;

    public Level(Owner owner, double max, double cur, Color color) {
	super(owner);
	this.max = max;
	this.cur = cur;
	this.color = color;
    }

    public static void drawmeter(GOut g, double l, Color color, Color ocolor) {
	Coord sz = g.sz();
	int h = (int) (sz.y * l);
	g.chcolor(ocolor);
	g.frect(new Coord(1, 0), new Coord(5, sz.y));
	g.chcolor(color);
	g.frect(new Coord(2, sz.y - h), new Coord(3, h - 1));
	g.chcolor();
    }

    public Double overlay() {
	return(cur / max);
    }

    public void drawoverlay(GOut g, Double l) {
	drawmeter(g, l, color, Color.BLACK);
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	double max = ((Number)args[1]).doubleValue();
	double cur = ((Number)args[2]).doubleValue();
	Color color = (args.length > 3) ? (Color)args[3] : null;
	if(color == null)
	    color = defcolor;
	return(new Level(owner, max, cur, color));
    }
}
