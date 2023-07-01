package haven.sprites.mesh;

import haven.FastMesh;
import haven.MapMesh;
import haven.Utils;
import haven.VertexBuf;
import haven.render.Pipe;
import haven.render.RenderTree;
import haven.render.States;
import haven.render.VertexColor;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ColoredSupportMesh extends FastMesh {
    private static final Map<MeshKey, ColoredSupportMesh> sprmap = new HashMap<>();


    private ColoredSupportMesh(VertexBuf buf, ShortBuffer sa) {
        super(buf, sa);
    }

    public void added(RenderTree.Slot slot) {
        slot.ostate(Pipe.Op.compose(MapMesh.postmap,
                new VertexColor(),
                new States.Facecull(States.Facecull.Mode.NONE)));
    }

    public synchronized static ColoredSupportMesh getMesh(final Color col, final int[][] positions, final float angle, final float height) {
        MeshKey key = new MeshKey(col, positions, angle);
        if(sprmap.containsKey(key))
            return sprmap.get(key);
        else {
            final float[] squarecol = Utils.c2fa(col);
            final float sideLength = 11f;
            final float h = height;

            final int totalSquares = positions.length;

            final FloatBuffer pa = Utils.mkfbuf(4 * 3 * totalSquares);
            final FloatBuffer na = Utils.mkfbuf(4 * 3 * totalSquares);
            final FloatBuffer cl = Utils.mkfbuf(4 * 4 * totalSquares);
            final ShortBuffer sa = Utils.mksbuf(4 * 3 * totalSquares);

            short vertexIndex = 0;
            for (int[] position : positions) {
                float xOff = sideLength * position[0];
                float yOff = sideLength * position[1];

                float[][] vertices = {
                        {xOff - sideLength / 2, yOff - sideLength / 2},
                        {xOff + sideLength / 2, yOff - sideLength / 2},
                        {xOff + sideLength / 2, yOff + sideLength / 2},
                        {xOff - sideLength / 2, yOff + sideLength / 2}
                };

                for (float[] vertex : vertices) {
                    float rotatedX = (float) (vertex[0] * Math.cos(angle) - vertex[1] * Math.sin(angle));
                    float rotatedY = (float) (vertex[0] * Math.sin(angle) + vertex[1] * Math.cos(angle));
                    pa.put(rotatedX).put(rotatedY).put(h);
                    na.put(rotatedX).put(rotatedY).put(h);
                    cl.put(squarecol[0]).put(squarecol[1]).put(squarecol[2]).put(squarecol[3]);
                }

                sa.put(vertexIndex).put((short) (vertexIndex + 1)).put((short) (vertexIndex + 2));
                sa.put(vertexIndex).put((short) (vertexIndex + 2)).put((short) (vertexIndex + 3));

                vertexIndex += 4;
            }

            sprmap.put(key, new ColoredSupportMesh(new VertexBuf(new VertexBuf.VertexData(pa),
                    new VertexBuf.NormalData(na),
                    new VertexBuf.ColorData(cl)),
                    sa));
            return sprmap.get(key);
        }
    }

    public static class MeshKey {
        final Color color;
        final int[][] positions;
        final float angle;

        public MeshKey(Color color, int[][] positions, float angle) {
            this.color = color;
            this.positions = positions;
            this.angle = angle;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MeshKey meshKey = (MeshKey) obj;
            return Float.compare(meshKey.angle, angle) == 0 &&
                    Arrays.deepEquals(positions, meshKey.positions) &&
                    Objects.equals(color, meshKey.color);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(color, angle);
            result = 31 * result + Arrays.deepHashCode(positions);
            return result;
        }
    }
}