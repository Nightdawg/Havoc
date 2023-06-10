package haven.sprites.mesh;

import haven.FastMesh;
import haven.MapMesh;
import haven.VertexBuf;
import haven.render.Pipe;
import haven.render.RenderTree;
import haven.render.States;

import java.nio.ShortBuffer;

public class ObstMesh extends FastMesh {
    public ObstMesh(VertexBuf buf, ShortBuffer sa) {
        super(buf, sa);
    }

    @Override
    public void added(RenderTree.Slot slot) {
        super.added(slot);
        slot.ostate(Pipe.Op.compose(MapMesh.postmap,
                new States.Facecull(States.Facecull.Mode.NONE)));
    }
}
