package haven.automated;


import haven.*;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class EnterNearestVessel implements Runnable {
    private GameUI gui;
    public static int numberOfAutoselects = 0;

    public EnterNearestVessel(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        Gob vessel = null;
        int vesseltype = 0;
        Gob player = gui.map.player();
        if (player == null)
            return;
        
        try {
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    Resource res = null;
                    try {
                        res = gob.getres();
                    } catch (Loading l) {
                    }
                    if (res != null) {
                        int type = 0;
                        if (res.basename().equals("stallion") || res.basename().equals("mare")) {
                            type = 1;
                        } else if (res.basename().equals("dugout") || res.basename().equals("rowboat") || res.basename().equals("spark")) {
                            type = 2;
                        } else if (res.name.equals("gfx/terobjs/vehicle/knarr") || res.name.equals("gfx/terobjs/vehicle/snekkja")) {
                            type = 3;
                        } else {
                            continue;
                        }

                        double distFromPlayer = gob.rc.dist(player.rc);
                        if (distFromPlayer <= 20 * 20 && (vessel == null || distFromPlayer < vessel.rc.dist(player.rc))) {
                            vesseltype = type;
                            vessel = gob;
                        }
                    }
                }
            }
            if (vessel == null || vesseltype == 0)
                return;

            if (vesseltype == 1) {
                gui.wdgmsg("act","pose","whistle");
                gui.map.wdgmsg("click", Coord.z, vessel.rc.floor(posres), 1, 0, 0, (int) vessel.id, vessel.rc.floor(posres), 0, -1);
                int n = numberOfAutoselects;
                long timeout = System.currentTimeMillis()+2000;
                while (n == numberOfAutoselects && timeout-System.currentTimeMillis() > 0) {
                    if (vessel.rc.dist(player.rc) > 35) {
                        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
                        gui.map.wdgmsg("click", Coord.z, vessel.rc.floor(posres), 1, 0, 0, (int) vessel.id, vessel.rc.floor(posres), 0, -1);
                        sleep(35);
                    } else {
                        FlowerMenu.setNextSelection("Giddyup!");
                        gui.map.wdgmsg("click", Coord.z, vessel.rc.floor(posres), 3, 0, 0, (int) vessel.id, vessel.rc.floor(posres), 0, -1);
                        sleep(100);
                        if (player.getattr(Moving.class) instanceof Following) {
                            return;
                        }
                    }
                }
            } else if (vesseltype == 3) {
                gui.map.wdgmsg("click", Coord.z, vessel.rc.floor(posres), 3, 0, 0, (int) vessel.id, vessel.rc.floor(posres), 0, -1);
                gui.ui.rcvr.rcvmsg(gui.ui.lastid+1, "cl", 0, gui.ui.modflags());
            } else {
                gui.map.wdgmsg("click",Coord.z, vessel.rc.floor(posres), 3, 0, 0, (int) vessel.id, vessel.rc.floor(posres), 0, -1);
            }
        } catch (InterruptedException e) {
        }
    }
}
