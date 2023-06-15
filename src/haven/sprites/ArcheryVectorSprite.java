package haven.sprites;

import haven.*;
import haven.render.BaseColor;
import haven.render.Render;
import haven.render.RenderTree;
import haven.sprites.mesh.ObstMesh;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ArcheryVectorSprite extends Sprite {
    public static final int id = -59129521;
    private ObstMesh mesh;
    private static final BaseColor col = new BaseColor(new Color(255, 0, 0, 170));
	private Coord2d lc;
	private float offset = 0f;
	VertexBuf.VertexData posa;

    public ArcheryVectorSprite(final Gob g, int range) {
	super(g, null);
		DrawOffset dro = g.getattr(DrawOffset.class);
		if (dro != null) {
			this.offset = dro.off.z;
		}
		{
			final Coord2d[][] shapes = new Coord2d[1][4];
			final Coord2d offset = new Coord2d(range, 0);
			{
				shapes[0][0] = offset.rotate(Math.toRadians(0 - 1));
				shapes[0][1] = offset.rotate(Math.toRadians(0 + 1));
				shapes[0][2] = offset.rotate(Math.toRadians(180 - 2)).norm(3);
				shapes[0][3] = offset.rotate(Math.toRadians(180 + 2)).norm(3);

			}
			mesh = makeMesh(shapes, col.color(), 1F);
		}
    }

    @Override
    public void added(RenderTree.Slot slot) {
	super.added(slot);
	slot.add(mesh, col);
    }

    @Override
    public String toString() {
	return "ArcheryVectorSprite";
    }

	public ObstMesh makeMesh(final Coord2d[][] shapes, final Color col, final float h) {
		final int polygons = shapes.length;
		final float[] hiddencolor = Utils.c2fa(col);
		final FloatBuffer pa, na, cl;
		final ShortBuffer sa;

		{
			int verts = 0, inds = 0;
			for (Coord2d[] shape : shapes) {
				verts += shape.length;
				inds += (int) (Math.ceil(shape.length / 3.0));
			}
			pa = Utils.mkfbuf(verts * 3);
			na = Utils.mkfbuf(verts * 3);
			cl = Utils.mkfbuf(verts * 4);
			sa = Utils.mksbuf(inds * 3);
		}

		for (Coord2d[] shape : shapes) {
			for (final Coord2d off : shape) {
				pa.put((float) off.x).put((float) off.y).put(h - this.offset);
				na.put((float) off.x).put((float) off.y).put(0f);
				cl.put(hiddencolor[0]).put(hiddencolor[1]).put(hiddencolor[2]).put(hiddencolor[3]);
			}
		}

		short voff = 0;
		for (int poly = 0; poly < polygons; ++poly) {
			final int vertsper = shapes[poly].length;
			for (int j = 0; j < (int) Math.ceil(vertsper / 3.0); ++j) {
				short s1 = (short) ((voff * j % vertsper) + (poly * vertsper));
				short s2 = (short) (((voff * j + 1) % vertsper) + (poly * vertsper));
				short s3 = (short) (((voff * j + 2) % vertsper) + (poly * vertsper));
				sa.put(s1).put(s2).put(s3);
				voff += 2;
			}
			voff = 0;
		}
		this.posa = new VertexBuf.VertexData(pa);
		return new ObstMesh(new VertexBuf(new VertexBuf.VertexData(pa),
				new VertexBuf.NormalData(na),
				new VertexBuf.ColorData(cl)),
				sa);
	}

	private void setz(Render g, Glob glob, Coord2d c) {
		FloatBuffer posb = posa.data;
		int n = posa.size() / 2;
		try {
			float bz = (float)glob.map.getcz(c.x, c.y);
			for(int i = 0; i < n; i++) {
				float z = (float)glob.map.getcz(c.x + posb.get(i * 3), c.y - posb.get(i * 3 + 1)) - bz;
				posb.put(i * 3 + 2, z + 10);
				posb.put((n + i) * 3 + 2, z - 10);
			}
		} catch(Loading e) {
			return;
		}
	}

	public void gtick(Render g) {
		Coord2d cc = ((Gob)owner).rc;
		if((lc == null) || !lc.equals(cc)) {
			setz(g, owner.context(Glob.class), cc);
			lc = cc;
		}
	}
}
