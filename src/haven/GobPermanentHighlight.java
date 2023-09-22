package haven;

import haven.render.MixColor;
import haven.render.Pipe;

public class GobPermanentHighlight extends GAttrib implements Gob.SetupMod {

    public static MixColor purple = new MixColor(OptWnd.permanentHighlightColorOptionWidget.currentColor);
    public State state;
    public enum State {
        PURPLE,
    }
    public GobPermanentHighlight(Gob g, State state) {
	super(g);
	this.state = state;
    }
    
    public Pipe.Op gobstate() {
        switch (state) {
            case PURPLE: return purple;
            default: return null;
        }
    }
}