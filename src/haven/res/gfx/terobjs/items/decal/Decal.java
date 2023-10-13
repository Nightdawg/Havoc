/* Preprocessed source code */
/* $use: lib/itemtex */

package haven.res.gfx.terobjs.items.decal;

import haven.*;
import haven.render.*;
import haven.res.lib.itemtex.ItemTex;
import java.util.*;
import java.util.function.*;
import java.awt.image.BufferedImage;

/* >spr: Decal */
@haven.FromResource(name = "gfx/terobjs/items/decal", version = 4)
public class Decal implements Sprite.Factory {
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	Supplier<Pipe.Op> eq = null;
	Gob gob = owner.ocontext(Gob.class).orElse(null);
	if(gob != null) {
	    Resource ores = gob.getres();
	    if(ores != null) {
		Skeleton.BoneOffset bo = ores.layer(Skeleton.BoneOffset.class, "decal");
		if(bo != null)
		    eq = bo.from(Skeleton.getpose(gob));
	    }
	}
	Material base = res.layer(Material.Res.class, 16).get();
	RenderTree.Node proj = res.layer(FastMesh.MeshRes.class, 0).m;
	Coord3f pc;
	if(sdt.eom()) {
	    pc = Coord3f.o;
	} else {
	    pc = new Coord3f((float)(sdt.float16() * MCache.tilesz.x), -(float)(sdt.float16() * MCache.tilesz.y), 0);
	}
	var ownerres = owner.getres();
	if (ownerres != null && ownerres.name.equals("gfx/terobjs/cupboard")) {
		pc = Coord3f.of(0, 0, 1);
	}
	Location offset = null;
	if(eq == null)
	    offset = Location.xlate(pc);
	Material sym = null;
	if(!sdt.eom()) {
	    BufferedImage img = ItemTex.create(owner, sdt);
	    if(img != null) {
		TexRender tex = ItemTex.fixup(img);
		sym = new Material(base, tex.draw, tex.clip);
	    }
	}
	RenderTree.Node[] parts = StaticSprite.lsparts(res, Message.nil);
	if(sym != null)
	    parts = Utils.extend(parts, sym.apply(proj));
	Location cpoffset = offset;
	Supplier<Pipe.Op> cpeq = eq;
	return(new StaticSprite(owner, res, parts) {
		final Collection<RenderTree.Slot> slots = new ArrayList<>(1);

		public void added(RenderTree.Slot slot) {
		    if(cpeq != null)
			slot.ostate(cpeq.get());
		    else if(cpoffset != null)
			slot.ostate(cpoffset);
		    super.added(slot);
		    slots.add(slot);
		}

		public void removed(RenderTree.Slot slot) {
		    super.removed(slot);
		    slots.remove(slot);
		}

		public boolean tick(double dt) {
		    if(cpeq != null) {
			Pipe.Op nst = cpeq.get();
			for(RenderTree.Slot slot : slots)
			    slot.ostate(nst);
		    }
		    return(false);
		}
	    });
    }
}
