package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobStateHighlight extends GAttrib implements Gob.SetupMod {
    public static MixColor green = new MixColor(OptWnd.emptyContainerOrPreparedWorkstationColorOptionWidget.currentColor);
    public static MixColor yellow = new MixColor(OptWnd.somewhatFilledContainerOrInProgressWorkstationColorOptionWidget.currentColor);
    public static MixColor red = new MixColor(OptWnd.fullContainerOrFinishedWorkstationColorOptionWidget.currentColor);
    public static MixColor gray = new MixColor(OptWnd.unpreparedWorkstationColorOptionWidget.currentColor);
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