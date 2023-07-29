package haven.sprites;

import haven.*;
import haven.render.*;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ChaseVectorSprite extends Sprite implements PView.Render2D {


    public static final Color MAINCOLOR = new Color(255, 255, 255, 220);
    public static final Color FOECOLOR = new Color(255, 0, 0, 230);
    public static final Color FRIENDCOLOR = new Color(47, 191, 7, 230);
    public static final Color UNKNOWNCOLOR = new Color(255, 199, 0, 230);
    private final Moving mov;

    public ChaseVectorSprite(Gob gob, Homing mov) {
        super(gob, null);
        this.mov = mov;

    }

    public void draw(GOut g, Pipe state) {
        if (OptWnd.drawChaseVectors) {
            try {
                Gob gob = (Gob) owner;
                UI ui = gob.glob.sess.ui;
                if (ui != null) {
                    MapView mv = ui.gui.map;
                    if (mv != null) {
                        if (mov instanceof Homing) {
                            Gob Target = gob.glob.oc.getgob(((Homing) mov).tgt);
                            Color chaserColor;
                            if (gob.isMe()) {
                                chaserColor = MAINCOLOR;
                            } else if (gob.isPartyMember() && !gob.isMe()) {
                                chaserColor = FRIENDCOLOR;
                            } else if (Target.isMe() || Target.isPartyMember()) {
                                chaserColor = FOECOLOR;
                            } else {
                                chaserColor = UNKNOWNCOLOR;
                            }
                            if (Target != null) {
                                Coord ChaserCoord = mv.screenxf(gob.getc()).round2();
                                Coord TargetCoord = mv.screenxf(Target.getc()).round2();
                                g.chcolor(Color.BLACK);
                                g.line(ChaserCoord, TargetCoord, 5);
                                g.chcolor(chaserColor);
                                g.line(ChaserCoord, TargetCoord, 3);
                                g.chcolor();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
