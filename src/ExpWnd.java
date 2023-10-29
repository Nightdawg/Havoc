/* Preprocessed source code */
import haven.*;

import java.util.Arrays;

/* >wdg: ExpWnd */
@haven.FromResource(name = "ui/expwnd", version = 22)
public class ExpWnd extends Window {
    public static Resource sfx = Loading.waitfor(Resource.classres(ExpWnd.class).pool.load("sfx/exp", 1));
    public static final RichText.Foundry fnd = new RichText.Foundry();
    public final Indir<Resource> exp;
    public final int ep;
    private Button close;
    private Img img, text;

    public static Widget mkwidget(UI ui, Object... args) {
	Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	int ep = (args.length > 1)?((Integer)args[1]):0;
	return(new ExpWnd(res, ep));
    }

    public ExpWnd(Indir<Resource> exp, int ep) {
	super(UI.scale(300, 50), "Yo, check it out!", true);
	this.exp = exp;
	this.ep = ep;
    }

    protected void added() {
	if(c.equals(0, 0))
	    c = new Coord((parent.sz.x - sz.x) / 2, OptWnd.expWindowLocationIsTop ? 0 : (parent.sz.y - sz.y));
	Audio.play(sfx);
	super.added();
    }

    public void tick(double dt) {
	if(img == null) {
	    Tex img;
	    String cap, text;
	    try {
		img = exp.get().layer(Resource.imgc).tex();
		Resource.Tooltip tt = exp.get().layer(Resource.tooltip);
		cap = (tt == null)?null:(tt.t);
		text = exp.get().layer(Resource.pagina).text;
	    } catch(Loading e) {
			CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
		return;
	    }
	    if(cap != null)
		chcap(cap);
	    this.img = add(new Img(img), 0, UI.scale(10));
	    this.text = add(new Img(fnd.render(text, UI.scale(300)).tex()), img.sz().x + UI.scale(5), 10);
	    if(ep > 0)
		add(new Label("Experience points gained: " + ep), this.text.c.x, this.text.c.y + this.text.sz.y + UI.scale(10));
	    Coord csz = contentsz();
	    this.close = adda(new Button(UI.scale(100), "Aight"), csz.x / 2, csz.y + UI.scale(25), 0.5, 0);
	    resize(contentsz());
	    this.c = new Coord((parent.sz.x - sz.x) / 2, OptWnd.expWindowLocationIsTop ? 0 : (parent.sz.y - sz.y));
	}
	super.tick(dt);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == close)
	    wdgmsg("close");
	else
	    super.wdgmsg(sender, msg, args);
    }
}
