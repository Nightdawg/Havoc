package haven.automated;

import haven.*;

import java.util.*;
import java.util.stream.Collectors;

public class EquipFromBelt implements Runnable {
    private GameUI gui;
    private String actionButtonString;

    public EquipFromBelt(GameUI gui, String actionButtonString) {
        this.gui = gui;
        this.actionButtonString = actionButtonString;
    }

    @Override
    public void run() {
        try {
            WItem leftHand = gui.getequipory().slots[6];
            WItem rightHand = gui.getequipory().slots[7];
            if (gui.vhand == null) {
                switch (actionButtonString) {
                    case "tsacks":
                        boolean isLeftHandTravelersSack = leftHand != null && leftHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack");
                        boolean isRightHandTravelersSack = rightHand != null && rightHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack");
                        if (isLeftHandTravelersSack && isRightHandTravelersSack) {
                            gui.ui.msg("Already wearing Traveler Sacks.");
                        } else {
                            equipTravellerSacksInBelt();
                        }
                        break;
                    case "wbindles":
                        boolean isLeftHandWanderersBindle = leftHand != null && leftHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle");
                        boolean isRightHandWanderersBindle = rightHand != null && rightHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle");
                        if (isLeftHandWanderersBindle && isRightHandWanderersBindle) {
                            gui.ui.msg("Already wearing Wanderer Bindles.");
                        } else {
                            equipBindlesInBelt();
                        }
                        break;
                    case "b12":
                        boolean isB12Equipped = leftHand != null && leftHand.item.res.get().name.equals("gfx/invobjs/small/b12axe");
                        if (isB12Equipped) {
                            gui.ui.msg("Already wearing B12.");
                        } else {
                            equipB12InBelt();
                        }
                        break;
                    default:
                        //
                }
            }
        } catch (Exception e) {
            gui.ui.error("Error in Equip from Belt Script.");
            e.printStackTrace();
        }
    }

    private static final String[] RESTRICTED_ITEMS = {
            "bucket",
            "pickingbasket",
            "splint",
    };

    //TODO: Maybe there's some way to determine if the item held in both hands is one and the same?
    // Like check for some unique item ID? Do items even have a unique ID?
    private static final String[] TWOHANDED_ITEMS = {
            // ND: Weapons
            "b12axe",
            "boarspear",
            "cutblade",
            "huntersbow",
            "rangersbow",

            // ND: Tools
            "scythe",
            "sledgehammer",
            "glassrod",
            "pickaxe",
            "shovel-m",
            "shovel-w",
            "shovel-t",

            // ND: Instruments too
            "bagpipe",
            "flute",
            "lute",
            "drum",
            "harmonica",
            "fiddle",
            "claycuckoou",
            "wildgoatlur",
    };

    public void equipTravellerSacksInBelt() throws InterruptedException {
        Equipory equipory = gui.getequipory();
        Inventory belt = null;
        Map<GItem, Coord> items = new HashMap<>();
        Coord sqsz = UI.scale(new Coord(33, 33));

        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player) continue;

            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;

                Coord inventorySize = ((Inventory) ww).isz;
                belt = (Inventory) ww;

                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);

                        Map<GItem, Coord> finalItems = items;
                        ((Inventory) ww).wmap.entrySet().stream()
                                .filter(entry -> entry.getValue().c.equals(calculatedCoord) &&
                                        (entry.getKey().res.get().name.equals("gfx/invobjs/small/travellerssack")))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }

        if (items.isEmpty()) {
            gui.ui.error("No traveller sacks found in the belt.");
            return;
        }

        if (items.size() > 2) {
            items = items.entrySet().stream()
                    .limit(2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        WItem firstHand = equipory.slots[6];
        boolean isNullFirst = firstHand == null;
        boolean isWrongItemFirst = firstHand != null && !(firstHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack"));
        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(firstHand.item.res.get().name::contains)) {
            isWrongItemFirst = false;
            gui.ui.error("Item in left hand can not be placed in belt. Moving on.");
        }
        if (isNullFirst || isWrongItemFirst) {
            if (!items.isEmpty()) {
                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
                items.remove(itemEntry.getKey());

                GItem item = itemEntry.getKey();
                Coord originalPosition = itemEntry.getValue();

                item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                equipory.wdgmsg("drop", 6);
                Thread.sleep(5);
                if (!isNullFirst) {
                    belt.wdgmsg("drop", originalPosition);
                    Thread.sleep(5);
                }
            }
        }

        //This timeout is to get server response in case you had two handed item
        Thread.sleep(100);

        WItem secondHand = equipory.slots[7];
        boolean isNullSecond = secondHand == null;
        boolean isWrongItemSecond = secondHand != null && !(secondHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack"));
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(secondHand.item.res.get().name::contains)) {
            isWrongItemSecond = false;
            gui.ui.error("Item in right hand can not be placed in belt. Moving on.");
        }
        if (isNullSecond || isWrongItemSecond) {
            if (!items.isEmpty()) {
                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
                items.remove(itemEntry.getKey());

                GItem item = itemEntry.getKey();
                Coord originalPosition = itemEntry.getValue();

                item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                equipory.wdgmsg("drop", 7);
                Thread.sleep(5);
                if (!isNullSecond) {
                    belt.wdgmsg("drop", originalPosition);
                    Thread.sleep(5);
                }
            }
        }
        //gui.ui.msg("Traveller Sacks equip script Finished.");
    }

    public void equipBindlesInBelt() throws InterruptedException {
        Equipory equipory = gui.getequipory();
        Inventory belt = null;
        Map<GItem, Coord> items = new HashMap<>();
        Coord sqsz = UI.scale(new Coord(33, 33));

        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player) continue;

            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;

                Coord inventorySize = ((Inventory) ww).isz;
                belt = (Inventory) ww;

                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);

                        Map<GItem, Coord> finalItems = items;
                        ((Inventory) ww).wmap.entrySet().stream()
                                .filter(entry -> entry.getValue().c.equals(calculatedCoord) &&
                                        (entry.getKey().res.get().name.equals("gfx/invobjs/small/wanderersbindle")))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }

        if (items.isEmpty()) {
            gui.ui.error("No wanderer bindles found in the belt.");
            return;
        }

        if (items.size() > 2) {
            items = items.entrySet().stream()
                    .limit(2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        WItem firstHand = equipory.slots[6];
        boolean isNullFirst = firstHand == null;
        boolean isWrongItemFirst = firstHand != null && !(firstHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle"));
        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(firstHand.item.res.get().name::contains)) {
            isWrongItemFirst = false;
            gui.ui.error("Item in left hand can not be placed in belt. Moving on.");
        }
        if (isNullFirst || isWrongItemFirst) {
            if (!items.isEmpty()) {
                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
                items.remove(itemEntry.getKey());

                GItem item = itemEntry.getKey();
                Coord originalPosition = itemEntry.getValue();

                item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                equipory.wdgmsg("drop", 6);
                Thread.sleep(5);
                if (!isNullFirst) {
                    belt.wdgmsg("drop", originalPosition);
                    Thread.sleep(5);
                }
            }
        }

        //This timeout is to get server response in case you had two handed item
        Thread.sleep(100);

        WItem secondHand = equipory.slots[7];
        boolean isNullSecond = secondHand == null;
        boolean isWrongItemSecond = secondHand != null && !(secondHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle"));
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(secondHand.item.res.get().name::contains)) {
            isWrongItemSecond = false;
            gui.ui.error("Item in left hand can not be placed in belt. Moving on.");
        }
        if (isNullSecond || isWrongItemSecond) {
            if (!items.isEmpty()) {
                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
                items.remove(itemEntry.getKey());

                GItem item = itemEntry.getKey();
                Coord originalPosition = itemEntry.getValue();

                item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                equipory.wdgmsg("drop", 7);
                Thread.sleep(5);
                if (!isNullSecond) {
                    belt.wdgmsg("drop", originalPosition);
                    Thread.sleep(5);
                }
            }
        }
        //gui.ui.msg("Wanderer Bindles equip script Finished.");
    }

    public void equipB12InBelt() throws InterruptedException {
        Equipory equipory = gui.getequipory();
        Inventory belt = null;
        Map<GItem, Coord> items = new HashMap<>();
        Coord sqsz = UI.scale(new Coord(33, 33));

        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof haven.GItem.ContentsWindow) || !((GItem.ContentsWindow) w).player) continue;

            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;

                Coord inventorySize = ((Inventory) ww).isz;
                belt = (Inventory) ww;

                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);

                        Map<GItem, Coord> finalItems = items;
                        ((Inventory) ww).wmap.entrySet().stream()
                                .filter(entry -> entry.getValue().c.equals(calculatedCoord) &&
                                        (entry.getKey().res.get().name.equals("gfx/invobjs/small/b12axe")))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }

        if (items.isEmpty()) {
            gui.ui.error("No B12 found in the belt.");
            return;
        }

        if (items.size() > 1) {
            items = items.entrySet().stream()
                    .limit(1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        WItem firstHand = equipory.slots[6];
        WItem secondHand = equipory.slots[7];
        boolean isNullFirst = firstHand == null;
        boolean isNullSecond = secondHand == null;

        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(firstHand.item.res.get().name::contains)) {
            gui.ui.error("Item in left hand can not be placed in belt. B12 can not be equipped from belt.");
            return;
        }
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(secondHand.item.res.get().name::contains)) {
            gui.ui.error("Item in right hand can not be placed in belt. B12 can not be equipped from belt.");
            return;
        }

        boolean needExtraEmptyBeltSlot = false;
        if (!isNullFirst && !isNullSecond){
            if (Arrays.stream(TWOHANDED_ITEMS).noneMatch(firstHand.item.res.get().name::endsWith)){
                needExtraEmptyBeltSlot = true;
            }
        }

        if (needExtraEmptyBeltSlot){

        } else {

        }



//        if (isNullFirst) {
//            if (!items.isEmpty()) {
//                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
//                items.remove(itemEntry.getKey());
//
//                GItem item = itemEntry.getKey();
//                Coord originalPosition = itemEntry.getValue();
//
//                item.wdgmsg("take", Coord.z);
//                Thread.sleep(5);
//                equipory.wdgmsg("drop", 6);
//                Thread.sleep(5);
//                if (!isNullFirst) {
//                    belt.wdgmsg("drop", originalPosition);
//                    Thread.sleep(5);
//                }
//            }
//        }

//        //This timeout is to get server response in case you had two handed item
//        Thread.sleep(100);
//
//
//
//        if (isNullSecond) {
//            if (!items.isEmpty()) {
//                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
//                items.remove(itemEntry.getKey());
//
//                GItem item = itemEntry.getKey();
//                Coord originalPosition = itemEntry.getValue();
//
//                item.wdgmsg("take", Coord.z);
//                Thread.sleep(5);
//                equipory.wdgmsg("drop", 7);
//                Thread.sleep(5);
//                if (!isNullSecond) {
//                    belt.wdgmsg("drop", originalPosition);
//                    Thread.sleep(5);
//                }
//            }
//        }
        //gui.ui.msg("B12 equip script Finished.");
    }


}