package haven.automated;

import haven.*;
import haven.resutil.WaterTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static haven.OCache.posres;

public class SkisScript implements Runnable {

    private GameUI gui;
    private static final int TIMEOUT = 4000;

    private static final int HAND_DELAY = 8;


    public SkisScript(GameUI gui) {
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

        if (player.imOnSkis) { // ND: If you're on skis already, try to pick them up
            //find closest skis and pick it up
            Gob gobSkis = null;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && (res.name.startsWith("gfx/terobjs/vehicle/skis-wilderness"))) {
                            Coord2d plc = gui.map.player().rc;
                            if ((gobSkis == null || gob.rc.dist(plc) < gobSkis.rc.dist(plc)))
                                gobSkis = gob;
                        }
                    } catch (Loading l) {
                    }
                }
            }

            if (gobSkis == null)
                return;
            if (gobSkis.rc.dist(gui.map.player().rc) < 11*5) {
                    FlowerMenu.setNextSelection("Pick up");
                    gui.map.wdgmsg("click", Coord.z, gobSkis.rc.floor(posres), 3, 0, 0, (int) gobSkis.id, gobSkis.rc.floor(posres), 0, -1);
            }
            if (eq.slots[14] == null) { // ND: Don't need to do anything here, cause "Pick up" already puts it on your cape slot
                return;
            } else {
                int timeout = 0;
                while (gui.hand.isEmpty() || gui.vhand == null) {
                    timeout += HAND_DELAY;
                    if (timeout >= TIMEOUT) {
                        gui.error("Skis Script: Timed out trying to Pick up Skis");
                        return;
                    }
                    try {
                        Thread.sleep(HAND_DELAY);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }

                //Check if there is a 2x3 space in inventory
                Coord freecoord = gui.maininv.isRoom(2,3);
                if (freecoord != null) {
                    gui.maininv.wdgmsg("drop", freecoord);
                } else {
                    gui.error("Skis Script: No free space in Inventory for Skis.");
                }
            }

        } else { // ND: If you're NOT on skis already, run this part instead
            // Find WItem Skis by going through equipment list
            WItem skis = null;

            skis = gui.maininv.getItemPartial("Wilderness Skis");

            for (WItem wi : eq.slots) {
                try {
                    if (wi.item.getname().equals("Wilderness Skis")) {
                        skis = wi;
                    }
                } catch (NullPointerException ex) {
                    //System.out.println("nothing equipped in this slot");
                }
            }


            if (skis != null) { // ND: If I have any Skis in my inventory at all, do the following
                //Get gui item and drop skis
                GItem skisItem = skis.item;
                skisItem.wdgmsg("drop", new Coord(skisItem.sz.x / 2, skisItem.sz.y / 2));
            }

            Gob gobSkis = null;
            boolean moutableSkis = false;
            // ND: try this 3 times, cause sometimes it fails to click the skis. Idk why, or how, I tried multiple print statements. 3 seems to be enough.
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
//                    e1.printStackTrace();
                    return;
                }

                // find closest skis
                synchronized (gui.map.glob.oc) {
                    for (Gob gob : gui.map.glob.oc) {
                        try {
                            Resource res = gob.getres();
                            if (res != null && (res.name.startsWith("gfx/terobjs/vehicle/skis-wilderness"))) {
                                Coord2d plc = gui.map.player().rc;
                                if ((gobSkis == null || gob.rc.dist(plc) < gobSkis.rc.dist(plc)))
                                    gobSkis = gob;
                            }
                        } catch (Loading l) {
                        }
                    }
                }
                if (gobSkis != null) {
                    Integer peekrbuf = null;
                    for (GAttrib g : gobSkis.attr.values()) {
                        if (g instanceof Drawable) {
                            if (g instanceof ResDrawable) {
                                ResDrawable resDrawable = gobSkis.getattr(ResDrawable.class);
                                peekrbuf = resDrawable.sdt.peekrbuf(0);
                            }
                        }
                    }
                    if (peekrbuf != null && peekrbuf == 0) { // ND: peekrbuf 0 means not-moving skis. Not necessarily empty.
                        if (gobSkis.rc.dist(gui.map.player().rc) < 11 * 6) {
                            moutableSkis = true;
                            FlowerMenu.setNextSelection("Ski off");
                            gui.map.wdgmsg("click", Coord.z, gobSkis.rc.floor(posres), 3, 0, 0, (int) gobSkis.id, gobSkis.rc.floor(posres), 0, -1);
                            //System.out.println("I CLICKED: " + i);
                        }
                    }
                }
            }
            if (skis == null && (gobSkis == null || !moutableSkis)){
                gui.error("Skis Script: No Skis found in Inventory and no mountable Skis found in close proximity.");
            }
        }
    }
}
