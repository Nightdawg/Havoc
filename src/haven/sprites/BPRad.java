/* Preprocessed source code */
package haven.sprites;

import haven.*;
import haven.render.*;
import haven.render.Model.Indices;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/* >spr: BPRad */
@FromResource(name = "gfx/fx/bprad", version = 8)
public class BPRad extends Sprite {
    static final Pipe.Op smat = new BaseColor(new Color(192, 0, 0, 128));
    static final Pipe.Op emat = Pipe.Op.compose(new BaseColor(new Color(255, 224, 96)), new States.LineWidth(4));
	static final Pipe.Op blacc = Pipe.Op.compose(new BaseColor(new Color(0, 0, 0, 140)), new States.LineWidth(1));
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod, emod;
    private Coord2d lc;
    float[] barda;

	private Pipe.Op col;
	public static final Color redr = new Color(192, 0, 0, 70);
	public static final Color bluer = new Color(22, 67, 219, 70);
	public static final Color yelr = new Color(248, 210, 0, 70);
	public static final Color gren = new Color(88, 255, 0, 70);
	public static final Color purp = new Color(193, 0, 255, 70);

    public BPRad(Owner owner, Resource res, float r) {
	super(owner, res);
	int n = Math.max(24, (int)(2 * Math.PI * r / 11.0));
	FloatBuffer posb = Utils.wfbuf(n * 3 * 2);
	FloatBuffer nrmb = Utils.wfbuf(n * 3 * 2);
	for(int i = 0; i < n; i++) {
	    float s = (float)Math.sin(2 * Math.PI * i / n);
	    float c = (float)Math.cos(2 * Math.PI * i / n);
	    posb.put(     i  * 3 + 0, c * r).put(     i  * 3 + 1, s * r).put(     i  * 3 + 2,  10);
	    posb.put((n + i) * 3 + 0, c * r).put((n + i) * 3 + 1, s * r).put((n + i) * 3 + 2, -10);
	    nrmb.put(     i  * 3 + 0, c).put(     i  * 3 + 1, s).put(     i  * 3 + 2, 0);
	    nrmb.put((n + i) * 3 + 0, c).put((n + i) * 3 + 1, s).put((n + i) * 3 + 2, 0);
	}
	VertexBuf.VertexData posa = new VertexBuf.VertexData(posb);
	VertexBuf.NormalData nrma = new VertexBuf.NormalData(nrmb);
	VertexBuf vbuf = new VertexBuf(posa, nrma);
	this.smod = new Model(Model.Mode.TRIANGLES, vbuf.data(), new Indices(n * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));
	this.emod = new Model(Model.Mode.LINE_STRIP, vbuf.data(), new Indices(n + 1, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::eidx));
	this.posa = posa;
	this.vbuf = vbuf;
    }

	public BPRad(final Owner owner, final Resource resource, final float n, Color col) {
		super(owner, resource);
		this.col = new BaseColor(col);
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
					.put((max + i) * 3 + 2, -10.0f);
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
		this.posa = posa;
		this.vbuf = vbuf;
	}
    public BPRad(Owner owner, Resource res, Message sdt) {
	this(owner, res, Utils.hfdec((short)sdt.int16()) * 11);
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
		posb.put((n + i) * 3 + 2, z - 10);
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
			slot.add(emod, emat);
		}
    }
}
