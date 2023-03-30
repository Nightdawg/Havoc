/* Preprocessed source code */
package haven.res.gfx.fx.floatimg;

import haven.*;
import haven.render.*;
import java.awt.Color;

@haven.FromResource(name = "gfx/fx/floatimg", version = 3)
public class FloatText extends FloatSprite {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 10);
    
    public FloatText(Owner owner, Resource res, String str, Color col) {
	super(owner, res, new TexI(Utils.outline2(fnd.render(str, col).img, Color.BLACK,true)), 2);
    }
}

/* >spr: Score */
