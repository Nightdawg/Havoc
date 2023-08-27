package haven.automated;

import haven.*;
import haven.resutil.WaterTile;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static haven.OCache.posres;

public class CoracleScript implements Runnable {

    private GameUI gui;
    private static final int TIMEOUT = 4000;

    private static final int HAND_DELAY = 8;
    public List<String> bogtype = new ArrayList<>(Arrays.asList("gfx/tiles/bog", "gfx/tiles/bogwater", "gfx/tiles/fen", "gfx/tiles/fenwater", "gfx/tiles/swamp", "gfx/tiles/swampwater", "", ""));

    public CoracleScript(GameUI gui) {
        this.gui = gui;
    }

    public void run() {

        Gob player = gui.map.player();
        if (player == null)
            return; //player is null, possibly taking a road, don't bother trying to do all of the below.

        //Get Equipment
        Equipory eq = gui.getequipory();
        if (eq == null)
            return;

        if (player.imInCoracle) { // ND: If you're in a coracle already, try to pick it up
            //find closest coracle and pick it up
            Gob gobCoracle = null;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && (res.name.startsWith("gfx/terobjs/vehicle/coracle"))) {
                            Coord2d plc = gui.map.player().rc;
                            if ((gobCoracle == null || gob.rc.dist(plc) < gobCoracle.rc.dist(plc)))
                                gobCoracle = gob;
                        }
                    } catch (Loading l) {
                    }
                }
            }

            if (gobCoracle == null)
                return;

            if (gobCoracle.rc.dist(gui.map.player().rc) < 11*5) {
                FlowerMenu.setNextSelection("Pick up");
                gui.map.wdgmsg("click", Coord.z, gobCoracle.rc.floor(posres), 3, 0, 0, (int) gobCoracle.id, gobCoracle.rc.floor(posres), 0, -1);
            }

            if (eq.slots[14] == null) { // ND: Don't need to do anything here, cause "Pick up" already puts it on your cape slot
                return;
            } else {
                int timeout = 0;
                while (gui.hand.isEmpty() || gui.vhand == null) {
                    timeout += HAND_DELAY;
                    if (timeout >= TIMEOUT) {
                        gui.error("Timeout trying to pickup coracle");
                        return;
                    }
                    try {
                        Thread.sleep(HAND_DELAY);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }

                //Check if there is a 4x3 space in inventory
                Coord freecoord = gui.maininv.isRoom(4,3);
                if (freecoord != null) {
                    gui.maininv.wdgmsg("drop", freecoord);
                } else {
                    gui.error("No free space in inventory for coracle :(");
                }
            }

        } else { // ND: If you're NOT in a coracle already, run this part instead
            // Find WItem coracle by going through equipment list
            WItem coracle = null;

            coracle = gui.maininv.getItemPartial("Coracle");

            for (WItem wi : eq.slots) {
                try {
                    if (wi.item.getname().equals("Coracle")) {
                        coracle = wi;
                    }
                } catch (NullPointerException ex) {
                    //System.out.println("nothing equipped in this slot");
                }
            }


            if (coracle != null) { // ND: If I have any coracle in my inventory at all, do the following
                // ND: Check for about 4 seconds if I'm on a water tile
                Tiler tl = gui.ui.sess.glob.map.tiler(gui.ui.sess.glob.map.gettile(gui.map.player().rc.floor(MCache.tilesz)));
                int id = gui.ui.sess.glob.map.gettile(gui.map.player().rc.floor(MCache.tilesz));
                int timeout = 0;
                Resource tileRes = gui.ui.sess.glob.map.tilesetr(id);
                while (tl != null && !(tl instanceof WaterTile || bogtype.contains(tileRes.name))) {
                    tl = gui.ui.sess.glob.map.tiler(gui.ui.sess.glob.map.gettile(gui.map.player().rc.floor(MCache.tilesz)));
                    timeout++;
                    // ND: I copied this from Cediner. I wonder if this is less stressful for the CPU compared to just doing something like (System.currentTimeMillis() - start > 4000)
                    if (timeout > 300) {
                        gui.error("Timed out waiting for water tile to drop coracle on.");
                        return;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //Get gui item and drop coracle
                GItem coracleItem = coracle.item;
                coracleItem.wdgmsg("drop", new Coord(coracleItem.sz.x / 2, coracleItem.sz.y / 2));
            }

            // ND: try this 3 times, cause sometimes it fails to click the coracle. Idk why, or how, I tried multiple print statements. 3 seems to be enough.
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                // find closest coracle
                Gob gobCoracle = null;
                synchronized (gui.map.glob.oc) {
                    for (Gob gob : gui.map.glob.oc) {
                        try {
                            Resource res = gob.getres();
                            if (res != null && (res.name.startsWith("gfx/terobjs/vehicle/coracle"))) {
                                Coord2d plc = gui.map.player().rc;
                                if ((gobCoracle == null || gob.rc.dist(plc) < gobCoracle.rc.dist(plc)))
                                    gobCoracle = gob;
                            }
                        } catch (Loading l) {
                        }
                    }
                }
                if (gobCoracle != null) {
                    Integer peekrbuf = null;
                    for (GAttrib g : gobCoracle.attr.values()) {
                        if (g instanceof Drawable) {
                            if (g instanceof ResDrawable) {
                                ResDrawable resDrawable = gobCoracle.getattr(ResDrawable.class);
                                peekrbuf = resDrawable.sdt.peekrbuf(0);
                            }
                        }
                    }
                    if (peekrbuf != null && peekrbuf == 22) { // ND: peekrbuf 22 means empty coracle, on water
                        if (gobCoracle.rc.dist(gui.map.player().rc) < 11 * 5) {
                            FlowerMenu.setNextSelection("Into the blue yonder!");
                            gui.map.wdgmsg("click", Coord.z, gobCoracle.rc.floor(posres), 3, 0, 0, (int) gobCoracle.id, gobCoracle.rc.floor(posres), 0, -1);
                            //System.out.println("I CLICKED: " + i);
                        }
                    }
                }
            }
        }
    }
}
