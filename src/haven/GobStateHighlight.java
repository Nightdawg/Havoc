package haven;

import haven.render.MixColor;
import haven.render.Pipe;

public class GobStateHighlight extends GAttrib implements Gob.SetupMod {
    private static final MixColor green = new MixColor(0,120,0, 180);
    private static final MixColor yellow = new MixColor(194, 155, 2, 140);
    private static final MixColor red = new MixColor(170, 0, 0, 170);
    private static final MixColor gray = new MixColor(20, 20, 20, 170);
    public State state;

    public enum State {
        GREEN, RED, GRAY, YELLOW,
    }
    public GobStateHighlight(Gob g, State state) {
	super(g);
	this.state = state;
    }
    
    public Pipe.Op gobstate() {
        switch (state) {
            case GREEN: return green;
            case YELLOW: return yellow;
            case RED: return red;
            case GRAY: return gray;
            default: return null;
        }
    }
}