package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobHighlightParty extends GAttrib implements Gob.SetupMod {
    private final Color c;
    
    public GobHighlightParty(Gob g, Color c) {
	super(g);
	this.c = c;
    }
    
    public void start() {
    }
    
    public Pipe.Op gobstate() {
        return new MixColor(c.getRed(), c.getGreen(), c.getBlue(), 133);
    }
}