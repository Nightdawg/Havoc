package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static haven.OCache.posres;

public class FishingBot extends Window implements Runnable {
    private final GameUI gui;
    private boolean stop;
    private boolean active;

    public FishingBot(GameUI gui) {
        super(new Coord(200, 200), "FishingBot");
        this.gui = gui;
        this.stop = false;

        add(new Button(50, "Start") {
            @Override
            public void click() {
//                for(WItem item : gui.maininv.getAllItems()){
//                    if(item.item != null && item.item.getname() != null){
//                        System.out.println("----------------------------------------------------");
//                        System.out.println(item.item.wdgid());
//                        System.out.println(item.item.getname());
//                        System.out.println(item.item.getres().name);
//                        for(ItemInfo info : item.item.info()){
//                            System.out.println(info.getClass().getName());
//                            if(info instanceof ItemInfo.Contents){
//                                System.out.println("content here");
//                            }
//                            if(info instanceof GItem.Amount){
//                                System.out.println("amount " + ((GItem.Amount) info).itemnum());
//                            }
//                        }
//                    }
//                }

//                List<WItem> items = gui.getAllContentsWindows();
//                for(WItem item : items){
//                    System.out.println("----------------------------------------------------");
//                        System.out.println(item.item.wdgid());
//                        System.out.println(item.item.getname());
//                        System.out.println(item.item.getres().name);
//                    System.out.println("info: ");
//                        for(ItemInfo info : item.item.info()){
//                            System.out.println(info.getClass().getName());
//                        }
//                }

                for(Inventory inventory : gui.getAllInventories()){
                    System.out.println(inventory.children().size());
                }
            }
        }, UI.scale(50, 50));
    }

    private int contentAnalysis(WItem item) {
        int count = 0;
        for (ItemInfo info : item.item.info()) {
            if (info instanceof ItemInfo.Contents) {
                count++;
            }
        }
        return count;
    }

    private List<WItem> getAllItemsFromAllInventoriesAndStacks(){
        List<WItem> items = new ArrayList<>();
        for(Inventory inventory : gui.getAllInventories()){
            for(WItem item : inventory.getAllItems()){
                if(!item.item.getname().contains("stack of")){
                    items.add(item);
                }
            }
        }
        items.addAll(gui.getAllContentsWindows());
        return items;
    }

    private void manageFishingPole(){
        Equipory equipory = gui.getequipory();
        WItem leftHand = equipory.slots[6];
        WItem rightHand = equipory.slots[7];
        int fishingPoleState = -1;
        if (leftHand != null && leftHand.item != null && leftHand.item.getres() != null && leftHand.item.getres().name.contains("gfx/invobjs/small/bushpole")) {
            fishingPoleState = contentAnalysis(leftHand);
        } else if (rightHand != null && rightHand.item != null && rightHand.item.getres() != null && rightHand.item.getres().name.contains("gfx/invobjs/small/bushpole")) {
            fishingPoleState = contentAnalysis(rightHand);
        }

        if(fishingPoleState == 0){
//                    putOnFishingLine();
        } else if (fishingPoleState == 1){
//                    putOnHook();
        } else if (fishingPoleState == 2){
//                    putOnBait();
        } else if (fishingPoleState == 3){
//                    continueFishing(); if no progbar then start fishing
        }
    }

    @Override
    public void run() {
        while (!stop) {
            if(active){
//                checkInventory();
                manageFishingPole();


            }
            sleep(1000);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }
}
