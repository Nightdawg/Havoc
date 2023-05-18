//
// Decompiled by Procyon v0.5.30
//

package haven.res.gfx.fx.bprad;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BPRad extends Sprite
{
    private Pipe.Op col;
    public static final Color redr = new Color(192, 0, 0, 60);
    public static final Color bluer = new Color(22, 67, 219, 60);
    public static final Color yelr = new Color(248, 210, 0, 60);
    public static final Color gren = new Color(88, 255, 0, 150);
    public static final Color purp = new Color(193, 0, 255, 150);
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod;
    private Coord2d lc;

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



    private FillBuffer sidx(final Model.Indices indices, final Environment environment) {
        final FillBuffer fillbuf = environment.fillbuf(indices);
        final ShortBuffer shortBuffer = fillbuf.push().asShortBuffer();
        for (int i = 0, n = indices.n / 6; i < n; ++i) {
            final int n2 = i * 6;
            shortBuffer.put(n2 + 0, (short)i).put(n2 + 1, (short)(i + n)).put(n2 + 2, (short)((i + 1) % n));
            shortBuffer.put(n2 + 3, (short)(i + n)).put(n2 + 4, (short)((i + 1) % n + n)).put(n2 + 5, (short)((i + 1) % n));
        }
        return fillbuf;
    }

    private void setz(final Render render, final Glob glob, final Coord2d coord2d) {
        final FloatBuffer data = this.posa.data;
        final int n = this.posa.size() / 2;
        try {
            final float n2 = (float)glob.map.getcz(coord2d.x, coord2d.y);
            for (int i = 0; i < n; ++i) {
                final float n3 = (float)glob.map.getcz(coord2d.x + data.get(i * 3), coord2d.y - data.get(i * 3 + 1)) - n2;
                data.put(i * 3 + 2, n3 + 10.0f);
                data.put((n + i) * 3 + 2, n3 - 10.0f);
            }
        }
        catch (Loading loading) {
            return;
        }
        this.vbuf.update(render);
    }

    public void gtick(final Render render) {
        final Coord2d rc = ((Gob)this.owner).rc;
        if (this.lc == null || !this.lc.equals(rc)) {
            this.setz(render, (Glob)this.owner.context((Class) Glob.class), rc);
            this.lc = rc;
        }
    }

    public void added(final RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(Rendered.postpfx, new States.Facecull(States.Facecull.Mode.NONE), (p -> p.put(Clickable.slot, null)), Location.goback("gobx")));
        if (col == null) {
                this.col = new BaseColor(redr);
        }
        slot.add(this.smod, this.col);
    }
}
