package haven.automated;


import haven.*;

import java.util.Arrays;
import java.util.HashSet;

import static haven.OCache.posres;

public class ClickNearestGate implements Runnable {
    private GameUI gui;

    public ClickNearestGate(GameUI gui) {
        this.gui = gui;
    }

    public final static HashSet<String> gates = new HashSet<String>(Arrays.asList(
            "brickwallgate",
            "brickbiggate",
            "drystonewallgate",
            "drystonewallbiggate",
            "palisadegate",
            "palisadebiggate",
            "polegate",
            "polebiggate"
    ));

    double maxDistance = 8 * 11;
    @Override
    public void run() {
        Gob theGate = null;
        Gob player = gui.map.player();
        if (player == null)
            return; //player is null, possibly taking a road, don't bother trying to do all of the below.
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
                    continue;
                Resource res = null;
                try {
                    res = gob.getres();
                } catch (Loading l) {
                }
                if (res != null) {
                    //Open nearby gates, but not visitor gates, since you dont often open/close them
                    boolean isGate = gates.contains(res.basename());
                    try {
                        if (isGate) {
                            for (Gob.Overlay ol : gob.ols) {
                                String oname = gui.map.glob.sess.getres(haven.Utils.uint16d(ol.sdt.rbuf, 0)).get().basename();
                                if (oname.equals("visflag"))
                                    isGate = false;
                            }
                        }
                    } catch (NullPointerException ignored) {}
                    if (isGate) {
                        if (distFromPlayer < maxDistance && (theGate == null || distFromPlayer < theGate.rc.dist(gui.map.player().rc))) {
                            theGate = gob;
                        }
                    }
                }
            }
        }
        if (theGate == null)
            return;
        gui.map.wdgmsg("click", Coord.z, theGate.rc.floor(posres), 3, 0, 0, (int) theGate.id, theGate.rc.floor(posres), 0, -1);
        if (gui.clickNearestGateThread != null) {
            gui.clickNearestGateThread.interrupt();
            gui.clickNearestGateThread = null;
        }
    }
}
