/* Preprocessed source code */
/* $use: ui/tt/q/quality */

import haven.*;
import haven.res.ui.tt.q.quality.*;
import haven.MenuGrid.Pagina;

/* >pagina: ShowQuality$Fac */
@haven.FromResource(name = "ui/tt/q/qtoggle", version = 5)
public class ShowQuality extends MenuGrid.PagButton {
    public ShowQuality(Pagina pag) {
	super(pag);
    }

    public static class Fac implements Factory {
	public MenuGrid.PagButton make(Pagina pag) {
	    //return(new ShowQuality(pag));
        return(new ShowQuality(pag));
	}
    }

    /*
    public BufferedImage img() {return(res.layer(Resource.imgc, 1).scaled());}
    */

    public void use(MenuGrid.Interaction iact) {
	        //AUtils.setprefb("qtoggle", Quality.show = !Quality.show);
        OptWnd.toggleQualityDisplayCheckBox.set(!Quality.show);
    }
}
