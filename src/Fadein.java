/* Preprocessed source code */
import haven.*;
import haven.render.*;
import java.awt.Color;
import java.awt.image.WritableRaster;

/* >wdg: Fadein */
@haven.FromResource(name = "ui/fadein", version = 5)
public class Fadein extends Widget {
    public static final Text.Furnace loadf = new PUtils.BlurFurn(new PUtils.TexFurn(new Text.Foundry(Text.fraktur, 50).aa(true),
										    Loading.waitfor(Resource.classres(Fadein.class).pool.load("gfx/hud/chantex", 2)).layer(Resource.imgc).img),
								 5, 4, new Color(96, 48, 0));
    public static final Tex loading = loadf.render("Loading...").tex();
    public static final Coord hsz = new Coord(128, 128);
    public static final double ir = 10, or = 60;
    public static final Tex peephole;
    public final double len;

    static {
	Coord cc = hsz.div(2);
	int B = 4;
	WritableRaster buf = PUtils.imgraster(hsz);
	Coord c = new Coord();
	for(c.y = 0; c.y < hsz.y; c.y++) {
	    for(c.x = 0; c.x < hsz.x; c.x++) {
		buf.setSample(c.x, c.y, 0, 0);
		buf.setSample(c.x, c.y, 1, 0);
		buf.setSample(c.x, c.y, 2, 0);
		double d = c.dist(cc);
		if(d < ir)
		    buf.setSample(c.x, c.y, 3, 0);
		else if(d > or)
		    buf.setSample(c.x, c.y, 3, 255);
		else
		    buf.setSample(c.x, c.y, 3, Utils.clip((int)(((d - ir) / (or - ir)) * 255), 0, 255));
	    }
	}
	TexI tex = new TexI(PUtils.rasterimg(buf));;
	tex.magfilter(Texture.Filter.LINEAR);
	tex.wrapmode(Texture.Wrapping.CLAMP);
	peephole = tex;
    }

    public Fadein(double len) {
	super(Coord.z);
	this.len = len;
    }

    public static Fadein mkwidget(UI ui, Object... args) {
	double len = ((Number)args[0]).doubleValue();
	return(new Fadein(len));
    }

    private double start = 0;
    private void drawfx(GOut g) {
	GameUI gui = getparent(GameUI.class);
	MapView mv = (gui == null) ? null : gui.map;
	Coord sz = g.sz();
	Loading loading = (mv == null) ? new Loading("Loading...") : mv.terrain.loading();
	if(loading != null) {
	    g.chcolor(0, 0, 0, 255);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    g.aimage(this.loading, sz.div(2), 0.5, 0.5);
	    String reason = loading.getMessage();
	    if(reason != null)
		g.atext(reason, sz.div(2).add(0, this.loading.sz().y / 2 + 15), 0.5, 0.5);
	    return;
	}
	double now = Utils.rtime();
	if(start == 0)
	    start = now;
	double a = (now - start) / (len/1.5);
	if(a > 1) {
	    ui.destroy(Fadein.this);
	    return;
	}
	double ra = (a * 0.25) + (Math.pow(a, 3) * 0.25) + (Math.pow(a, 6) * 0.5);
	double mr = Coord.z.dist(sz) / 2;
	double f = Math.max(ra * mr, 1) / ir;
	Coord ul = new Coord((int)((hsz.x - (sz.x / f)) / 2),
			     (int)((hsz.y - (sz.y / f)) / 2));
	Coord br = new Coord((int)((hsz.x + (sz.x / f)) / 2),
			     (int)((hsz.y + (sz.y / f)) / 2));
	peephole.render(g, g.ul, g.ul.add(sz), ul, br);
	g.chcolor(Utils.clipcol(0, 0, 0, 255 - (int)(255 * a)));
	g.frect(Coord.z, sz);
	g.chcolor();
    }

    public void draw(GOut g) {
	ui.drawafter(this::drawfx);
    }
}
