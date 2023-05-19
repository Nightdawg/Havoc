package haven;

import haven.render.MixColor;
import haven.render.Pipe;

public class GobStateHighlight extends GAttrib implements Gob.SetupMod {
    private static final MixColor empty = new MixColor(0, 170, 20, 200);
    private static final MixColor full = new MixColor(210, 20, 20, 200);
    public State state;

    public enum State {
        EMPTY, FULL, OTHER
    }
    public GobStateHighlight(Gob g, State state) {
	super(g);
	this.state = state;
    }
    
    public Pipe.Op gobstate() {
        switch (state) {
            case EMPTY: return empty;
            case FULL: return full;
            default: return null;
        }
    }
}