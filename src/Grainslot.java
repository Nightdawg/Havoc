/* Preprocessed source code */
import haven.*;
import java.util.*;
import static haven.Inventory.invsq;

/* >wdg: Grainslot */
@haven.FromResource(name = "ui/grainslot", version = 20)
public class Grainslot extends Widget implements DTarget, ItemInfo.Owner {
    public final Label lbl;
    public final Button tbtn, pbtn, ebtn;
    public Indir<Resource> icon;
    private ItemInfo.Raw rawinfo = null;
    private Tex iconc;

    public Grainslot() {
	super(UI.scale(390, 40));
	lbl = adda(new Label(""), sz.y, sz.y / 2, 0, 0.5);
	int w = UI.scale(60), m = UI.scale(5);
	ebtn = adda(new Button(w, "Empty", () -> wdgmsg("empty")), sz.x, sz.y / 2, 1.0, 0.5);
	tbtn = adda(new Button(w, "Take", () -> wdgmsg("take")), ebtn.c.x - m, sz.y / 2, 1.0, 0.5);
	pbtn = adda(new Button(w, "Put", () -> wdgmsg("put")), tbtn.c.x - m, sz.y / 2, 1.0, 0.5);
	ebtn.hide(); tbtn.hide(); pbtn.hide();
    }

    public static Widget mkwidget(UI ui, Object... args) {
	return(new Grainslot());
    }

    public void draw(GOut g) {
	int ic = (sz.y - invsq.sz().y) / 2;
	g.image(invsq, new Coord(ic, ic));
	if(icon != null) {
	    try {
		if(iconc == null)
		    iconc = icon.get().layer(Resource.imgc).tex();
		g.image(iconc, new Coord(ic + 1, ic + 1));
	    } catch(Loading e) {
			CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
	    }
	}
	super.draw(g);
    }

    private static final OwnerContext.ClassResolver<Widget> ctxr = new OwnerContext.ClassResolver<Widget>()
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    private List<ItemInfo> info = Collections.emptyList();
    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, rawinfo);
	return(info);
    }

    private double hoverstart;
    private Tex shorttip, longtip;
    public Object tooltip(Coord c, Widget prev) {
	int ic = (sz.y - invsq.sz().y) / 2;
	if((rawinfo != null) && c.isect(new Coord(ic, ic), invsq.sz())) {
	    double now = Utils.rtime();
	    if(prev != this)
		hoverstart = now;
	    try {
		if(now - hoverstart < 1.0) {
		    if(shorttip == null)
			shorttip = new TexI(ItemInfo.shorttip(info()));
		    return(shorttip);
		} else {
		    if(longtip == null)
			longtip = new TexI(ItemInfo.longtip(info()));
		    return(longtip);
		}
	    } catch(Loading l) {
		return("...");
	    }
	}
	return(super.tooltip(c, prev));
    }

    public boolean mousedown(Coord c, int button) {
	int ic = (sz.y - invsq.sz().y) / 2;
	if(c.isect(new Coord(ic, ic), invsq.sz())) {
	    wdgmsg("click", button, ui.modflags());
	    return(true);
	}
	return(super.mousedown(c, button));
    }

    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop", ui.modflags());
	return(true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	wdgmsg("itemiact", ui.modflags());
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "upd") {
	    lbl.settext((String)args[0]);
	    icon = (args[1] == null) ? null : ui.sess.getres((Integer)args[1]);
	    iconc = null;
	    int bfl = (Integer)args[2];
	    tbtn.show((bfl & 1) != 0);
	    pbtn.show((bfl & 2) != 0);
	    ebtn.show((bfl & 4) != 0);
	    rawinfo = (args.length > 3) ? new ItemInfo.Raw((Object[])args[3]) : null;
	    info = null;
	    shorttip = longtip = null;
	} else {
	    super.uimsg(msg, args);
	}
    }
}
