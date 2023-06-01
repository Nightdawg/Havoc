package haven.automated;

import haven.*;

public class YoinkGoodStuffFromKnockedEnemy implements Runnable {
    private Equipory enemyEquipory;
    private GameUI gui;

    public YoinkGoodStuffFromKnockedEnemy(Equipory enemyEquipory, GameUI gameUI) {
        this.enemyEquipory = enemyEquipory;
        this.gui = gameUI;
    }

    @Override
    public void run() {
        try {
            int playerBeltFreeSlots = getPlayerBeltFreeSlots();
            stealSelectedItems(playerBeltFreeSlots);
            enemyEquipory.ui.msg("Yoinked good items from this enemy.");
        } catch (Exception e) {
            e.printStackTrace();
            enemyEquipory.ui.msg("Something went wrong with yoinking.");
        }
    }

    public void stealSelectedItems(int freeBeltSlots) {
        int currentBeltSlots = freeBeltSlots;
        try {
            //getting left ring into your eq
            if (enemyEquipory.slots[8] != null) {
                enemyEquipory.slots[8].item.wdgmsg("transfer", Coord.z);
            }
            //getting right ring into your eq
            if (enemyEquipory.slots[9] != null) {
                enemyEquipory.slots[9].item.wdgmsg("transfer", Coord.z);
            }

            //try to put left hand weapon into your eq and then try to take it and put into belt
            if (enemyEquipory.slots[6] != null&& !enemyEquipory.slots[6].item.getres().name.equals("gfx/invobjs/small/roundshield")) {
                if(currentBeltSlots > 0){
                    enemyEquipory.slots[6].item.wdgmsg("take", Coord.z);
                    gui.getequipory().slots[5].item.wdgmsg("itemact", 0);
                    currentBeltSlots--;
                } else {
                    enemyEquipory.slots[6].item.wdgmsg("transfer", Coord.z);
                }
            }

            //try to put right hand weapon into your eq and then try to take it and put into belt
            if (enemyEquipory.slots[7] != null && !enemyEquipory.slots[7].item.getres().name.equals("gfx/invobjs/small/roundshield")) {
                if(currentBeltSlots > 0){
                    enemyEquipory.slots[7].item.wdgmsg("take", Coord.z);
                    gui.getequipory().slots[5].item.wdgmsg("itemact", 0);
                } else {
                    enemyEquipory.slots[7].item.wdgmsg("transfer", Coord.z);
                }
            }
            //getting armor into your eq
            if (enemyEquipory.slots[3] != null) {
                enemyEquipory.slots[3].item.wdgmsg("transfer", Coord.z);
            }

        } catch (Exception ignored) {}
    }


    public int getPlayerBeltFreeSlots() {
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player ) continue;
            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;
                int inventorySize = ((Inventory) ww).isz.x * ((Inventory) ww).isz.y ;
                for(Widget www:  ww.children()){
                    if(www instanceof GItem){
                        inventorySize--;
                    }
                }
                return inventorySize;
            }
        }
        return 0;
    }
}

