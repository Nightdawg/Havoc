package haven;

import haven.render.MixColor;
import haven.render.Pipe;

public class GobStateHighlight extends GAttrib implements Gob.SetupMod {
    private static final MixColor empty = new MixColor(0,194,0, 150);
    private static final MixColor other = new MixColor(209, 167, 0, 150);
    private static final MixColor full = new MixColor(180, 0, 0, 170);
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
            case OTHER: return other;
            case FULL: return full;
            default: return null;
        }
    }
}