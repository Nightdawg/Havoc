/* Preprocessed source code */
package haven.res.lib.plants;

import haven.*;
import haven.resutil.*;
import java.util.*;

@haven.FromResource(name = "lib/plants", version = 9)
public class GaussianPlant implements Sprite.Factory {
    public final int numl, numh;
    public final float r;

    public GaussianPlant(int numl, int numh, float r) {
	this.numl = numl;
	this.numh = numh;
	this.r = r;
    }

    public GaussianPlant(Object[] args) {
	this(((Number)args[0]).intValue(), ((Number)args[1]).intValue(), ((Number)args[2]).floatValue());
    }

    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	ArrayList<FastMesh.MeshRes> var = new ArrayList<FastMesh.MeshRes>(res.layers(FastMesh.MeshRes.class));
		CSprite spr = new CSprite(owner, res);
	if (OptWnd.simplifiedForageables){
		FastMesh.MeshRes mesh = var.get(0);
		spr.addpart(0, 0, mesh.mat.get(), mesh.m);
	} else {
		Random rnd = owner.mkrandoom();
		int num = rnd.nextInt(numh - numl + 1) + numl;
		for (int i = 0; i < num; i++) {
			FastMesh.MeshRes v = var.get(rnd.nextInt(var.size()));
			spr.addpart((float) rnd.nextGaussian() * r, (float) rnd.nextGaussian() * r, v.mat.get(), v.m);
		}
	}
	return(spr);
    }
}
