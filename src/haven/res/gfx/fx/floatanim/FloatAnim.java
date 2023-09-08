/* Preprocessed source code */
package haven.res.gfx.fx.floatanim;

import haven.*;
import haven.render.*;

@haven.FromResource(name = "gfx/fx/floatanim", version = 13)
public class FloatAnim extends Sprite implements PView.Render2D {
    public final Resource.Anim anim;
    public final float zo;
    public final Coord cc;
    public final float normsz;
    public boolean loop;
    public int f;
    public double a;

    public FloatAnim(Owner owner, Resource res, Resource.Anim anim, Coord cc, float normsz) {
	super(owner, res);
	this.anim = anim;
	zo = 0;
	this.cc = cc;
	this.normsz = normsz;
    }

    public static Coord autocc(Resource.Anim anim) {
	Coord ret = new Coord();
	for(int i = 0; i < anim.f.length; i++) {
	    for(int o = 0; o < anim.f[i].length; o++) {
		ret.x = Math.max(ret.x, anim.f[i][o].o.x + anim.f[i][o].sz.x);
		ret.y = Math.max(ret.y, anim.f[i][o].o.y + anim.f[i][o].sz.y);
	    }
	}
	return(ret);
    }

    public void draw(GOut g, Pipe state) {
	Coord3f fsc = Homo3D.obj2view(new Coord3f(0, 0, zo), state, Area.sized(Coord.z, g.sz()));
	Coord3f sczu = Homo3D.obj2view(new Coord3f(0, 0, zo + 1), state, Area.sized(Coord.z, g.sz()));
	Coord sc = fsc.round2();
	float scale = (sczu.y - fsc.y) / UI.scale(normsz);
	if(f < anim.f.length) {
	    for(int i = 0; i < anim.f[f].length; i++) {
		Resource.Image img = anim.f[f][i];
		Coord ul = new Coord(Math.round((-cc.x + img.o.x) * scale),
				     Math.round((-cc.y + img.o.y) * scale));
		Tex tex = img.tex();
		g.image(tex, sc.add(UI.scale(ul)), tex.sz().mul(scale));
	    }
	}
    }

    public boolean tick(double dt) {
	double d = anim.d * 0.001;
	if(!loop)
	    for(a += dt; a >= d; f++, a -= d);
	else
	    for(a += dt; a >= d; f = (f + 1) % anim.f.length, a -= d);
	return(f >= anim.f.length);
    }
}

/* >spr: AnimFloat */
