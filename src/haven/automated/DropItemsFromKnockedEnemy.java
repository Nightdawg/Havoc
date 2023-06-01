package haven.automated;

import haven.Coord;
import haven.Equipory;
import haven.GameUI;

public class DropItemsFromKnockedEnemy implements Runnable {
    private Equipory equipory;

    public DropItemsFromKnockedEnemy(Equipory equipory) {
        this.equipory = equipory;
    }

    @Override
    public void run() {
        try {
            dropAll();
            equipory.ui.msg("Dropped all items from this enemy.");
        } catch (Exception e) {
        e.printStackTrace();
            equipory.ui.msg("Something went wrong with dropping.");
    }

    }

    public void dropAll() {
        int[] ids = new int[]{ 2, 3};
//        int[] ids = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18};
        for (int id : ids) {
            if (equipory.slots[id] != null) {
                try {
                    if(equipory.slots[id].item.res != null){
                        equipory.slots[id].item.wdgmsg("drop", Coord.z);
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
