package haven.automated;

import haven.Coord;
import haven.GameUI;
import haven.Gob;

import static haven.OCache.posres;

public class Utils {

    public static void attackGob(GameUI gui, Gob gob) {
        if (gob != null && gui != null && gui.map != null) {
            gui.act("aggro");
            gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
            rightClick(gui);
        }
    }

    public static void rightClick(GameUI gui) {
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
    }
}
