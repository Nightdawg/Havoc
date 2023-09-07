/* Preprocessed source code */
import haven.*;

import java.awt.image.WritableRaster;

/* >wdg: Fadeout */
@haven.FromResource(name = "ui/fadeout", version = 5)
public class Fadeout extends AWidget {
    public MapView mv;
    public final double len, dur;

    public Fadeout(double len, double dur) {
	this.len = len;
	this.dur = dur;
    }

    protected void attached() {
	super.attached();
	this.mv = getparent(GameUI.class).map;
    }

    public static Fadeout mkwidget(UI ui, Object... args) {
	double len = ((Number)args[0]).doubleValue();
	double dur = ((Number)args[1]).doubleValue();
	return(new Fadeout(len, dur));
    }

    double start = 0;
    private void drawfx(GOut g) {
	double now = Utils.rtime();
	if(start == 0)
	    start = now;
	if((dur >= 0) && (now - start > dur)) {
	    ui.destroy(Fadeout.this);
	    return;
	}
	double a = (now - start) / len;
	if(a > 1)
	    a = 1;
	g.chcolor(Utils.clipcol(0, 0, 0, (int)(255 * a)));
	g.frect(Coord.z, g.sz());
	mv.shake = a * 100;
    }

    public void tick(double dt) {
	mv.delay2(this::drawfx);
	super.tick(dt);
    }
}
