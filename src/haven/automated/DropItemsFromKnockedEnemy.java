package haven.automated;

import haven.Coord;
import haven.Equipory;
import haven.GameUI;

public class DropItemsFromKnockedEnemy implements Runnable {
    private Equipory equipory;
    private GameUI gui;

    public DropItemsFromKnockedEnemy(Equipory equipory, GameUI gameUI) {
        this.equipory = equipory;
        this.gui = gameUI;
    }

    @Override
    public void run() {
        try {
            dropAll();
            gui.msg("Dropped all items from this enemy.");
        } catch (Exception e) {
            e.printStackTrace();
            gui.msg("Something went wrong with dropping.");
        }
    }

    public void dropAll() {
        int[] ids = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18};
        for (int id : ids) {
            if (equipory.slots[id] != null) {
                try {
                    if (equipory.slots[id].item.res != null) {
                        equipory.slots[id].item.wdgmsg("drop", Coord.z);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
