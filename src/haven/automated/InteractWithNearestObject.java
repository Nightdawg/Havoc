package haven.automated;


import haven.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static haven.OCache.posres;

public class InteractWithNearestObject implements Runnable {
    private GameUI gui;

    public InteractWithNearestObject(GameUI gui) {
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

    public final static Set<String> otherPickableObjects = new HashSet<String>(Arrays.asList( // ND: Pretty much any ground item can be added here
            "adder",
            "arrow",
            "bat",
            "precioussnowflake",
            "truffle-black0",
            "truffle-black1",
            "truffle-black2",
            "truffle-black3",
            "truffle-white0",
            "truffle-white1",
            "truffle-white2",
            "truffle-white3"
    ));

    double maxDistance = 12 * 11;
    @Override
    public void run() {
        Gob theObject = null;
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
                                String oname = gui.map.glob.sess.getres(Utils.uint16d(ol.sdt.rbuf, 0)).get().basename();
                                if (oname.equals("visflag"))
                                    isGate = false;
                            }
                        }
                    } catch (NullPointerException ignored) {}
                    if (isGate || res.name.startsWith("gfx/terobjs/herbs") || otherPickableObjects.contains(res.basename()) || Arrays.stream(Gob.CRITTERAURA_PATHS).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) {
                        if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(gui.map.player().rc))) {
                            theObject = gob;
                            if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                        }
                    }
                }
            }
        }
        if (theObject == null)
            return;
        gui.map.wdgmsg("click", Coord.z, theObject.rc.floor(posres), 3, 0, 0, (int) theObject.id, theObject.rc.floor(posres), 0, -1);
        if (gui.interactWithNearestObjectThread != null) {
            gui.interactWithNearestObjectThread.interrupt();
            gui.interactWithNearestObjectThread = null;
        }
    }
}
