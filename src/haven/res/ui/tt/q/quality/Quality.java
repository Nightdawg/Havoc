/* Preprocessed source code */
package haven.res.ui.tt.q.quality;

/* $use: ui/tt/q/qbuff */
import haven.*;
import haven.res.ui.tt.q.qbuff.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import haven.MenuGrid.Pagina;

/* >tt: Quality */
@haven.FromResource(name = "ui/tt/q/quality", version = 25)
public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static boolean show = Utils.getprefb("qtoggle", true);
    public static final BufferedImage qualityWorkaround = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).scaled();
    public Quality(Owner owner, double q) {
	//super(owner, Resource.classres(Quality.class).layer(Resource.imgc, 0).scaled(), "Quality", q);
        super(owner, qualityWorkaround, "Quality", q); //ND: workaround suggested by loftar
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Quality(owner, ((Number)args[1]).doubleValue()));
    }

    public Tex overlay() {
	return(new TexI(GItem.NumberInfo.numrenderStroked(q, new Color(255, 255, 255, 255), true)));
    }

    public void drawoverlay(GOut g, Tex ol) {
	if(show)
	    g.aimage(ol, new Coord(g.sz().x, 0), 1, 0);
    }
}
