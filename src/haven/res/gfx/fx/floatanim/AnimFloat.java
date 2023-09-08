/* Preprocessed source code */
package haven.res.gfx.fx.floatanim;

import haven.*;
import haven.render.*;

@haven.FromResource(name = "gfx/fx/floatanim", version = 13)
public class AnimFloat implements Sprite.Factory {
    public static final float normsz = -2.45f;
    public float scale;
    public boolean loop;

    public AnimFloat(float scale, boolean loop) {
	this.scale = scale;
	this.loop = loop;
    }

    public AnimFloat(float scale) {
	this(scale, false);
    }

    public AnimFloat() {
	this(1);
    }

    public AnimFloat(Object[] args) {
	this();
	for(Object argp : args) {
	    Object[] arg = (Object[])argp;
	    switch((String)arg[0]) {
	    case "scale":
		this.scale = ((Number)arg[1]).floatValue();
		break;
	    case "loop":
		this.loop = ((Number)arg[1]).intValue() != 0;
		break;
	    }
	}
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	Resource.Anim anim = null;
	if(sdt.eom()) {
	    anim = res.layer(Resource.animc);
	    if(anim == null)
		throw(new Sprite.ResourceException(String.format("no animation found in %s", res.name), res));
	} else {
	    int id = sdt.int16();
	    for(Resource.Anim al : res.layers(Resource.animc)) {
		if(al.id == id) {
		    anim = al;
		    break;
		}
	    }
	    if(anim == null)
		throw(new Sprite.ResourceException(String.format("animation %d not found in %s", id, res.name), res));
	}
	Coord cc = null;
	Resource.Neg neg = res.layer(Resource.negc);
	if(neg != null)
	    cc = neg.cc;
	if(cc == null)
	    cc = FloatAnim.autocc(anim);
	FloatAnim ret = new FloatAnim(owner, res, anim, cc, normsz / scale);
	if(loop)
	    ret.loop = true;
	return(ret);
    }
}
