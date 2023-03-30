/* Preprocessed source code */
package haven.res.gfx.fx.floatimg;

import haven.*;
import haven.render.*;
import java.awt.Color;

@haven.FromResource(name = "gfx/fx/floatimg", version = 3)
public class Score implements Sprite.Factory {
    private static int dup(int nibble) {
	return((nibble << 4) | nibble);
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	int num = sdt.int32();
	int fl = sdt.uint8();
	int col = sdt.uint16();

	String sign = ((fl & 1) != 0)?"+":"";
	if(num < 0) {
	    num = -num;
	    sign = "-";
	}
	String buf;
	int t = (fl & 6) >> 1;
	if(t == 1) {
	    buf = Integer.toString(num / 10) + "." + Integer.toString(num % 10);
	} else if(t == 2) {
	    buf = String.format("%02d:%02d", num / 60, num % 60);
	} else {
	    buf = Integer.toString(num);
	}
	buf = sign + buf;
	Color dcol = new Color(dup((col & 0xf000) >> 12),
			       dup((col & 0x0f00) >> 8),
			       dup((col & 0x00f0) >> 4),
			       dup((col & 0x000f) >> 0));
	return(new FloatText(owner, res, buf, dcol));
    }
}
