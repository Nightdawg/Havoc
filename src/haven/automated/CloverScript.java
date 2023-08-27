package haven.automated;

import haven.*;

import java.util.List;

import static haven.OCache.posres;

public class CloverScript implements Runnable {
    private GameUI gui;
    private Gob animal;
    private static final int TIMEOUT = 1000;
    private static final int HAND_DELAY = 8;

    double maxDistance = 7 * 11;

    public CloverScript(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = null;
                try {
                    res = gob.getres();
                } catch (Loading l) {
                }
                if (res != null) {
                    if (((res.name.equals("gfx/kritter/horse/horse")) || (res.name.equals("gfx/kritter/goat/wildgoat")) || (res.name.equals("gfx/kritter/boar/boar")))) {
                        double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                        if (distFromPlayer < maxDistance) {
                            if (animal == null)
                                animal = gob;
                            else if (gob.rc.dist(gui.map.player().rc) < animal.rc.dist(gui.map.player().rc))
                                animal = gob;
                        }
                    } else if (res.name.equals("gfx/kritter/cattle/cattle")) { // ND: Special case for Aurochs
                        for (GAttrib g : gob.attr.values()) {
                            if (g instanceof Drawable) {
                                if (g instanceof Composite) {
                                    Composite c = (Composite) g;
                                    if (c.comp.cmod.size() > 0) {
                                        for (Composited.MD item : c.comp.cmod) {
                                            if (item.mod.get().basename().equals("aurochs")){
                                                double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                                                if (distFromPlayer < maxDistance) {
                                                    if (animal == null)
                                                        animal = gob;
                                                    else if (gob.rc.dist(gui.map.player().rc) < animal.rc.dist(gui.map.player().rc))
                                                        animal = gob;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (res.name.equals("gfx/kritter/sheep/sheep")) { // ND: Special case for Mouflon
                        for (GAttrib g : gob.attr.values()) {
                            if (g instanceof Drawable) {
                                if (g instanceof Composite) {
                                    Composite c = (Composite) g;
                                    if (c.comp.cmod.size() > 0) {
                                        for (Composited.MD item : c.comp.cmod) {
                                            if (item.mod.get().basename().equals("mouflon")){
                                                double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                                                if (distFromPlayer < maxDistance) {
                                                    if (animal == null)
                                                        animal = gob;
                                                    else if (gob.rc.dist(gui.map.player().rc) < animal.rc.dist(gui.map.player().rc))
                                                        animal = gob;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (animal == null) {
            gui.error("Clover Script: No applicable animal found in close proximity. (Max: 7 tiles)");
            return;
        }

        WItem cloverw = null;
        // ND: First, check if you have any unstacked clover in any of your inventories
        for (Inventory inventory : gui.getAllInventories()){
            cloverw = inventory.getItemPrecise("Clover");
            if (cloverw != null) {
                break;
            }
        }
        // ND: If there's no unstacked clover anywhere, check the stacks
        if (cloverw == null) {
            for (WItem wItem : gui.getAllContentsWindows()) {
                if (wItem.item.getname().equals("Clover")){
                    cloverw = wItem;
                    break;
                }
            }
        }
        if (cloverw == null) {
            gui.error("Clover Script: No clover found anywhere in the inventory.");
            return;
        }
        if (!gui.hand.isEmpty()){
            if (gui.vhand != null){
                if (!gui.vhand.item.getname().equals("Clover")){
                    gui.error("Clover Script: I can't pick up the clover, you're already holding something else on the cursor!");
                    return;
                }
            }
        }
        GItem clover = cloverw.item;
        clover.wdgmsg("take", new Coord(clover.sz.x / 2, clover.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty() || gui.vhand == null) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("Clover Script: I found a clover in the inventory, but somehow I didn't pick it up on the cursor.");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }

        gui.map.wdgmsg("itemact", Coord.z, animal.rc.floor(posres), 0, 0, (int) animal.id, animal.rc.floor(posres), 0, -1);
    }
}

