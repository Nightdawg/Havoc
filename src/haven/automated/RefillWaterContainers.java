package haven.automated;

import haven.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RefillWaterContainers implements Runnable {
    private static final Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    private GameUI gui;

    public RefillWaterContainers(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            do {
                Inventory belt = returnBelt();
                Map<WItem, Coord> inventoryItems = getInventoryContainers();
                for (Map.Entry<WItem, Coord> item : inventoryItems.entrySet()) {
                    try {
                        item.getKey().item.wdgmsg("take", Coord.z);
                        Thread.sleep(10);
                        gui.map.wdgmsg("itemact", Coord.z, gui.map.player().rc.floor(posres), 0);
                        Thread.sleep(50);
                        gui.maininv.wdgmsg("drop", item.getValue());
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
                Map<WItem, Coord> beltItems = getBeltContainers();
                for (Map.Entry<WItem, Coord> item : beltItems.entrySet()) {
                    try {
                        item.getKey().item.wdgmsg("take", Coord.z);
                        Thread.sleep(10);
                        gui.map.wdgmsg("itemact", Coord.z, gui.map.player().rc.floor(posres), 0);
                        Thread.sleep(50);
                        belt.wdgmsg("drop", item.getValue());
                        gui.maininv.wdgmsg("drop", item.getValue());
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            } while (getInventoryContainers().size() != 0 && getBeltContainers().size() != 0);
            gui.ui.msg("Water Refilled");
        } catch (Exception e) {
            gui.ui.msg("Error in Refill Water Containers Script.");
        }

    }


    public Map<WItem, Coord> getBeltContainers() {
        Map<WItem, Coord> containers = new HashMap<>();
        Coord sqsz = UI.scale(new Coord(33, 33));
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player) continue;
            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;
                Coord inventorySize = ((Inventory) ww).isz;
                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);
                        for (Map.Entry<GItem, WItem> entry : ((Inventory) ww).wmap.entrySet()) {
                            if (entry.getValue().c.equals(calculatedCoord)) {
                                String resName = entry.getKey().res.get().name;
                                ItemInfo.Contents.Content content = getContent(entry.getKey());
                                if(content != null){
                                    System.out.println(resName + " - " + content.count + " - " + content.name + " _ " + content.unit);
                                } else {
                                    System.out.println(resName);
                                }
                                if (resName.equals("gfx/invobjs/small/waterskin") && shouldAddToContainers(content, 3.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                } else if (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                } else if (resName.equals("gfx/invobjs/small/glassjug") && shouldAddToContainers(content, 5.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                }
                            }
                        }
                    }
                }
            }
        }
        return containers;
    }

    public Inventory returnBelt() {
        Inventory belt = null;
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player) continue;
            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;
                belt = (Inventory) ww;
            }
        }
        return belt;
    }

    public Map<WItem, Coord> getInventoryContainers() {
        Inventory playerInventory = gui.maininv;
        Coord inventorySize = playerInventory.isz;
        Coord sqsz = UI.scale(new Coord(33, 33));
        Map<WItem, Coord> containers = new HashMap<>();
        for (int i = 0; i < inventorySize.x; i++) {
            for (int j = 0; j < inventorySize.y; j++) {
                Coord indexCoord = new Coord(i, j);
                Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);

                for (Map.Entry<GItem, WItem> entry : playerInventory.wmap.entrySet()) {
                    if (entry.getValue().c.equals(calculatedCoord)) {
                        String resName = entry.getKey().res.get().name;
                        ItemInfo.Contents.Content content = getContent(entry.getKey());
                        if (resName.equals("gfx/invobjs/waterskin") && shouldAddToContainers(content, 3.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        } else if (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        } else if (resName.equals("gfx/invobjs/glassjug") && shouldAddToContainers(content, 5.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        }
                    }
                }
            }
        }
        return containers;
    }

    private ItemInfo.Contents.Content getContent(GItem item) {
        ItemInfo.Contents.Content content = null;
        for (ItemInfo info : item.info()) {
            if (info instanceof ItemInfo.Contents) {
                content = ((ItemInfo.Contents) info).content;
            }
        }
        return content;
    }

    private boolean shouldAddToContainers(ItemInfo.Contents.Content content, float contentCount) {
        return content == null || (content.count != contentCount && Objects.equals(content.name, "Water"));
    }
}