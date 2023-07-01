/* Preprocessed source code */
package haven.sprites.baseSprite;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;
import haven.resutil.WaterTile;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static haven.MCache.tilesz;

public class ColoredCircleSprite extends Sprite {
    static final Pipe.Op smat = new BaseColor(new Color(0, 0, 0, 140));
    static final Pipe.Op blacc = Pipe.Op.compose(new BaseColor(new Color(0, 0, 0, 140)), new States.LineWidth(2));
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod, emod;
	private final float innerrad, outerrad;
    private Coord2d lc;
	private final float height;

	private final Pipe.Op col;

	public ColoredCircleSprite(final Owner owner, final Color col, final float innerrad, final float outerrad, final float height) {
		super(owner, null);
		this.col = new BaseColor(col);
		this.innerrad = innerrad;
		this.outerrad = outerrad;
		this.height = height;
		init();
	}

	private void init() {
		final double step = Math.PI / 64;
		final int max = 4 * 64;
		final FloatBuffer wfbuf = Utils.wfbuf(max * 3 * 2);
		final FloatBuffer wfbuf2 = Utils.wfbuf(max * 3 * 2);
		double rad = 0;
		for (int i = 0; i < max; ++i) {
			final float angx = (float) (Math.cos(rad)), angy = (float) (Math.sin(rad));
			final float ox = angx * outerrad, oy = angy * outerrad;
			final float ix = angx * innerrad, iy = angy * innerrad;
			wfbuf.put(i * 3 + 0, ox)
					.put(i * 3 + 1, oy)
					.put(i * 3 + 2, height);
			wfbuf.put((max + i) * 3 + 0, ix)
					.put((max + i) * 3 + 1, iy)
					.put((max + i) * 3 + 2, height);
			wfbuf2.put(i * 3 + 0, ox)
					.put(i * 3 + 1, oy)
					.put(i * 3 + 2, 0);
			wfbuf2.put((max + i) * 3 + 0, ix)
					.put((max + i) * 3 + 1, iy)
					.put((max + i) * 3 + 2, 0);
			rad += step;
		}
		final VertexBuf.VertexData posa = new VertexBuf.VertexData(wfbuf);
		final VertexBuf vbuf = new VertexBuf(posa, new VertexBuf.NormalData(wfbuf2));
		this.smod = new Model(Model.Mode.TRIANGLES, vbuf.data(), new Indices(max * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));
		this.emod = new Model(Model.Mode.LINE_STRIP, vbuf.data(), new Indices(max + 1, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::eidx));
		this.posa = posa;
		this.vbuf = vbuf;
	}

    private FillBuffer sidx(Indices dst, Environment env) {
	FillBuffer ret = env.fillbuf(dst);
	ShortBuffer buf = ret.push().asShortBuffer();
	for(int i = 0, n = dst.n / 6; i < n; i++) {
	    int b = i * 6;
	    buf.put(b + 0, (short)i).put(b + 1, (short)(i + n)).put(b + 2, (short)((i + 1) % n));
	    buf.put(b + 3, (short)(i + n)).put(b + 4, (short)(((i + 1) % n) + n)).put(b + 5, (short)((i + 1) % n));
	}
	return(ret);
    }

	private FillBuffer eidx(Indices dst, Environment env) {
		FillBuffer ret = env.fillbuf(dst);
		ShortBuffer buf = ret.push().asShortBuffer();
		for(int i = 0; i < dst.n - 1; i++)
			buf.put(i, (short)i);
		buf.put(dst.n - 1, (short)0);
		return(ret);
	}

    private void setz(Render g, Glob glob, Coord2d c) {
	FloatBuffer posb = posa.data;
	int n = posa.size() / 2;
	Gob gob = (Gob)owner;
	try {
		DrawOffset dro = gob.getattr(DrawOffset.class);
		MCache map = glob.map;
		Tiler t = map.tiler(map.gettile(c.floor(tilesz)));
		float extraWaterHeight = 0;
		if(t instanceof WaterTile) {
			extraWaterHeight = map.getzp(gob.rc).z - gob.getrc().z;
		}
	    float bz = (float)glob.map.getcz(c.x, c.y);
	    for(int i = 0; i < n; i++) {
			float z = (float)glob.map.getcz(c.x + posb.get(i * 3), c.y - posb.get(i * 3 + 1)) - bz;
			float z2 = (float)glob.map.getcz(c.x + posb.get((n + i) * 3), c.y - posb.get((n + i) * 3 + 1)) - bz;
			if (dro != null) {
				posb.put(i * 3 + 2, z + height - dro.off.z);
				posb.put((n + i) * 3 + 2, z2 + height - dro.off.z);
			} else {
				posb.put(i * 3 + 2, z + height + extraWaterHeight);
				posb.put((n + i) * 3 + 2, z2 + height + extraWaterHeight);
			}
	    }
	} catch(Loading e) {
	    return;
	}
	vbuf.update(g);
    }

    public void gtick(Render g) {
	Coord2d cc = ((Gob)owner).rc;
	if((lc == null) || !lc.equals(cc)) {
	    setz(g, owner.context(Glob.class), cc);
	    lc = cc;
	}
    }

    public void added(RenderTree.Slot slot) {
	// XXXRENDER rl.prepo(Rendered.eyesort);
	slot.ostate(Pipe.Op.compose(Rendered.postpfx,
				    new States.Facecull(States.Facecull.Mode.NONE),
				    Location.goback("gobx")));
		if (col != null) {
			slot.add(this.smod, this.col);
			slot.add(emod, blacc);
		} else {
			slot.add(smod, smat);
			slot.add(emod, blacc);
		}
    }
}
