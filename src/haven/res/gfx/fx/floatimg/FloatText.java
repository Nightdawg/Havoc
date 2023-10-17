/* Preprocessed source code */
package haven.res.gfx.fx.floatimg;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

@haven.FromResource(name = "gfx/fx/floatimg", version = 4)
public class FloatText extends FloatSprite {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);

    public static BufferedImage render(String str, Color col) {
	Color col2 = Utils.contrast(col);
	return(rasterimg(blurmask2(fnd.render(str, col).img.getRaster(), UI.rscale(1.0), UI.rscale(1.0), Color.BLACK)));
    }

    public FloatText(Owner owner, Resource res, String str, Color col) {
	super(owner, res, new TexI(Utils.outline2(fnd.renderstroked(str, col, Color.BLACK).img, Color.BLACK,true)), 2);
    }
}

/* >spr: Score */
