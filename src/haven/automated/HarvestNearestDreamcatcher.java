package haven.automated;


import haven.*;

import static haven.OCache.posres;

public class HarvestNearestDreamcatcher implements Runnable {
    private GameUI gui;

    public HarvestNearestDreamcatcher(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        Gob dreca = null;
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && res.name.startsWith("gfx/terobjs/dreca")) {
                        Coord2d plc = gui.map.player().rc;
                        if ((dreca == null || gob.rc.dist(plc) < dreca.rc.dist(plc)))
                            dreca = gob;
                    }
                } catch (Loading l) {
                }
            }
        }

        if (dreca == null)
            return;

        int initialSpace = gui.maininv.getFreeSpace();

        FlowerMenu.setNextSelection("Harvest");
        gui.map.wdgmsg("click", Coord.z, dreca.rc.floor(posres), 3, 0, 0, (int) dreca.id, dreca.rc.floor(posres), 0, -1);

        long now = System.currentTimeMillis();
        while (initialSpace <= gui.maininv.getFreeSpace() && System.currentTimeMillis() - now < 500) {
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                return;
            }
        }

        FlowerMenu.setNextSelection("Harvest");
        gui.map.wdgmsg("click", Coord.z, dreca.rc.floor(posres), 3, 0, 0, (int) dreca.id, dreca.rc.floor(posres), 0, -1);

        if (gui.harvestNearestDreamcatcherThread != null) {
            gui.harvestNearestDreamcatcherThread.interrupt();
            gui.harvestNearestDreamcatcherThread = null;
        }
    }
}

