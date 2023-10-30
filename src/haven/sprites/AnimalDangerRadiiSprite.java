/* Preprocessed source code */
package haven.sprites;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/* >spr: BPRad */
// ND: If you move the class, it no longer overwrites loftar's resource
public class AnimalDangerRadiiSprite extends Sprite {
	static final Pipe.Op blacc = Pipe.Op.compose(new BaseColor(new Color(0, 0, 0, 140)), new States.LineWidth(4), Clickable.No);
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod, emod;
    private Coord2d lc;

	private Pipe.Op col;

	public AnimalDangerRadiiSprite(final Owner owner, final Resource resource, final float n, Color col) {
		super(owner, resource);
		this.col = Pipe.Op.compose(new BaseColor(col), Clickable.No);
		init(n);
	}

	private void init(float n) {
		final int max = Math.max(24, (int)(6.283185307179586 * n / 11.0));
		final FloatBuffer wfbuf = Utils.wfbuf(max * 3 * 2);
		final FloatBuffer wfbuf2 = Utils.wfbuf(max * 3 * 2);
		for (int i = 0; i < max; ++i) {
			final float n2 = (float)Math.sin(6.283185307179586 * i / max);
			final float n3 = (float)Math.cos(6.283185307179586 * i / max);
			wfbuf.put(i * 3 + 0, n3 * n)
					.put(i * 3 + 1, n2 * n)
					.put(i * 3 + 2, 10.0f);
			wfbuf.put((max + i) * 3 + 0, n3 * n)
					.put((max + i) * 3 + 1, n2 * n)
					.put((max + i) * 3 + 2, -2.0f);
			wfbuf2.put(i * 3 + 0, n3)
					.put(i * 3 + 1, n2)
					.put(i * 3 + 2, 0.0f);
			wfbuf2.put((max + i) * 3 + 0, n3)
					.put((max + i) * 3 + 1, n2)
					.put((max + i) * 3 + 2, 0.0f);
		}
		final VertexBuf.VertexData posa = new VertexBuf.VertexData(wfbuf);
		final VertexBuf vbuf = new VertexBuf(posa, new VertexBuf.NormalData(wfbuf2));
		this.smod = new Model(Model.Mode.TRIANGLES, vbuf.data(), new Model.Indices(max * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));
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
	try {
	    float bz = (float)glob.map.getcz(c.x, c.y);
	    for(int i = 0; i < n; i++) {
		float z = (float)glob.map.getcz(c.x + posb.get(i * 3), c.y - posb.get(i * 3 + 1)) - bz;
		posb.put(i * 3 + 2, z + 10);
		posb.put((n + i) * 3 + 2, z - 2);
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
			slot.add(this.emod, blacc);
		}
    }
}
