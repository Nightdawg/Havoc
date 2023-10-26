package haven.sprites;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class GobSearchHighlight extends Sprite {
    static final Pipe.Op smat;
    private float a;
    private boolean rising = true;
    VertexBuf.VertexData posa;
    VertexBuf vbuf;
    Model smod;


    public GobSearchHighlight(final Owner owner, final Resource resource) {
        super(owner, resource);
        init();
        this.a = 0.0f;
    }

    private void init() {
        final float time = 0;
        final float s = 2f;
        final int max = Math.max(24, (int) (6.283185307179586 * s / 11.0));
        final FloatBuffer wfbuf = Utils.wfbuf(max * 3 * 2);
        final FloatBuffer wfbuf2 = Utils.wfbuf(max * 3 * 2);
        for (int i = 0; i < max; i++) {
            final float ang = (time + (6.283185307179586f * i / max)) % 6.283185307179586f;
            final float sinx = (float) Math.sin(ang);
            final float cosx = (float) Math.cos(ang);
            final float n = 2.5f * ang * 3;
            final float m = 2f * ang * 3;

            wfbuf.put(i * 3, cosx * n)
                    .put(i * 3 + 1, sinx * n)
                    .put(i * 3 + 2, 10.0f);
            wfbuf.put((max + i) * 3, cosx * m)
                    .put((max + i) * 3 + 1, sinx * m)
                    .put((max + i) * 3 + 2, -10.0f);
            wfbuf2.put(i * 3, cosx)
                    .put(i * 3 + 1, sinx)
                    .put(i * 3 + 2, 0.0f);
            wfbuf2.put((max + i) * 3, cosx)
                    .put((max + i) * 3 + 1, sinx)
                    .put((max + i) * 3 + 2, 0.0f);
        }
        this.posa = new VertexBuf.VertexData(wfbuf);

        this.vbuf = new VertexBuf(this.posa, new VertexBuf.NormalData(wfbuf2));
        this.smod = new Model(Model.Mode.TRIANGLES, this.vbuf.data(), new Model.Indices(max * 6, NumberFormat.UINT16, DataBuffer.Usage.STATIC, this::sidx));

    }

    private FillBuffer sidx(final Model.Indices indices, final Environment environment) {
        final FillBuffer fillbuf = environment.fillbuf(indices);
        final ShortBuffer shortBuffer = fillbuf.push().asShortBuffer();
        for (int i = 0, n = indices.n / 6; i < n - 1; i++) {
            final int n2 = i * 6;
            shortBuffer
                    .put(n2, (short) i)
                    .put(n2 + 1, (short) (i + n))
                    .put(n2 + 2, (short) ((i + 1)));
            shortBuffer
                    .put(n2 + 3, (short) (i + n))
                    .put(n2 + 4, (short) ((i + n + 1)))
                    .put(n2 + 5, (short) ((i + 1)));
        }
        return fillbuf;
    }

    private void spin(final Render render) {
        final FloatBuffer posadata = this.posa.data;
        final float twopi = 6.283185307179586f;
        final float time = (a * twopi * 2);
        final float sizemul = 1.4f - (float) Math.pow(a, 0.4);
        final float s = 8f;
        final int max = Math.max(24, (int) (twopi * s / 11.0));
        for (int i = 0; i < max; i++) {
            final float ang = ((twopi) * i / max) - time;
            final float sinx = (float) Math.sin(ang * 3);
            final float cosx = (float) Math.cos(ang * 3);

            final double radiusforangle = 5 + 3 * Math.pow((twopi) * i / max, 1.4);
            final float n = 1.1f * ((float) radiusforangle) * sizemul * 0.5f; // Adjusted for thickness
            final float m = 1f * ((float) radiusforangle) * sizemul * 0.5f;   // Adjusted for thickness

            posadata.put(i * 3, cosx * n)
                    .put(i * 3 + 1, sinx * n)
                    .put(i * 3 + 2, 2.0f + n * 3.6f); // Adjusted for 3x height
            posadata.put((max + i) * 3, cosx * m)
                    .put((max + i) * 3 + 1, sinx * m)
                    .put((max + i) * 3 + 2, n * 3.6f); // Adjusted for 3x height
        }
        this.vbuf.update(render);
    }


    public void gtick(final Render render) {
        this.spin(render);
    }

    public void added(final RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(Rendered.postpfx, new States.Facecull(States.Facecull.Mode.NONE), Location.goback("gobx")));
        slot.add(this.smod, smat);
    }

    public boolean tick(final double n) {
        if(rising){
            this.a += n / 6.0;
        } else {
            this.a -= n / 6.0;
        }

        if (a > 0.95) {
            rising = false;
        } else if (a < 0.05){
            rising = true;
        }

        return this.a >= 1.0;
    }

    static {
        smat = new BaseColor(new Color(255, 255, 255, 255));
    }
}
