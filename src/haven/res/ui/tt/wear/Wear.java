/* Preprocessed source code */
package haven.res.ui.tt.wear;

import haven.*;
import java.awt.image.BufferedImage;

/* >tt: Wear */
@haven.FromResource(name = "ui/tt/wear", version = 4)
public class Wear extends ItemInfo.Tip {
    public final int d, m;
    public final double percentage;

    public Wear(Owner owner, int d, int m) {
	super(owner);
	this.d = d;
	this.m = m;
    this.percentage = ((m-d)/(m/100d));
    }

    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Wear(owner, (Integer)args[1], (Integer)args[2]));
    }

    public BufferedImage tipimg() {
	if(d >= m)
        return(RichText.render(String.format("$col[70,194,80]{Durability}: $col[255,128,128]{%,d/%,d} $col[255,255,255]{(%,.1f%%)}", (m-d), m, percentage), 0).img); // ND: Remember to add the width to RichText render, in order for the colors to work
    return(RichText.render((String.format("$col[70,194,80]{Durability}: %,d/%,d (%,.2f%%)", (m-d), m, percentage)), 0).img);
//        return(RichText.render(String.format("Durability: $col[255,128,128]{%,d/%,d}", m-d, m), 0).img);
//	return(RichText.render(String.format("Durability: %,d/%,d", m-d, m)).img);
    }
}
