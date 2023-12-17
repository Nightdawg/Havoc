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
            if (gui.vhand == null) {
                switch (actionButtonString) {
                    case "tsacks":
                        equipDoubleItemsFromBelt("gfx/invobjs/small/travellerssack", "Traveller's Sacks");
                        break;
                    case "wbindles":
                        equipDoubleItemsFromBelt("gfx/invobjs/small/wanderersbindle", "Wanderer's Bindles");
                        break;
                    case "b12":
                        equipTwoHanderFromBelt("gfx/invobjs/small/b12axe", "B12");
                        break;
                    case "cutblade":
                        equipTwoHanderFromBelt("gfx/invobjs/small/cutblade", "Cutblade");
                        break;
                    case "boarspear":
                        equipTwoHanderFromBelt("gfx/invobjs/small/boarspear", "Boar Spear");
                        break;
                    case "pickaxe":
                        equipTwoHanderFromBelt("gfx/invobjs/small/pickaxe", "Pickaxe");
                        break;
                    case "sledgehammer":
                        equipTwoHanderFromBelt("gfx/invobjs/small/sledgehammer", "Sledgehammer");
                        break;
                    case "scythe":
                        equipTwoHanderFromBelt("gfx/invobjs/small/scythe", "Scythe");
                        break;
                    case "metalshovel":
                        equipTwoHanderFromBelt("gfx/invobjs/small/shovel-m", "Metal Shovel");
                        break;
                    case "tinkershovel":
                        equipTwoHanderFromBelt("gfx/invobjs/small/shovel-t", "Tinker's Shovel");
                        break;
                    case "woodenshovel":
                        equipTwoHanderFromBelt("gfx/invobjs/small/shovel-w", "Wooden Shovel");
                        break;
                    case "hirdsmanshield":
                        equipTwoDifferentItemsFromBelt("gfx/invobjs/small/roundshield", "gfx/invobjs/small/hirdsword", "Shield", "Hirdsman's Sword");
                        break;
                    case "bronzeshield":
                        equipTwoDifferentItemsFromBelt("gfx/invobjs/small/roundshield", "gfx/invobjs/small/bronzesword", "Shield", "Bronze Sword");
                        break;
                    case "fyrdsmanshield":
                        equipTwoDifferentItemsFromBelt("gfx/invobjs/small/roundshield", "gfx/invobjs/small/fyrdsword", "Shield", "Fyrdsman's Sword");
                        break;
                    default:
                        // ND: Default? Do nothing.
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

    private boolean isTwoHander(WItem leftHand, WItem rightHand) {
        return leftHand.item != null && rightHand.item != null && leftHand.item == rightHand.item;
    }

    public void equipDoubleItemsFromBelt(String resourcePath, String itemName) throws InterruptedException {
        Equipory equipory = gui.getequipory();
        WItem leftHand = equipory.slots[6];
        WItem rightHand = equipory.slots[7];
        boolean isLeftHandTravelersSack = leftHand != null && leftHand.item.res.get().name.equals(resourcePath);
        boolean isRightHandTravelersSack = rightHand != null && rightHand.item.res.get().name.equals(resourcePath);
        if (isLeftHandTravelersSack && isRightHandTravelersSack) {
            gui.ui.msg(itemName + " are already equipped.");
            return;
        }
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
                                        (entry.getKey().res.get().name.equals(resourcePath)))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }
        if (items.isEmpty()) {
            gui.ui.error("No " + itemName + " found in the belt.");
            return;
        }
        if (items.size() > 2) {
            items = items.entrySet().stream()
                    .limit(2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        boolean isNullFirst = leftHand == null;
        boolean isWrongItemFirst = leftHand != null && !(leftHand.item.res.get().name.equals(resourcePath));
        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(leftHand.item.res.get().name::contains)) {
            isWrongItemFirst = false;
            gui.ui.error("Item in Left Hand can not be placed in belt. Skipping.");
        }
        if (isNullFirst || isWrongItemFirst) {
            if (!items.isEmpty()) {
                switchItem(items, equipory, 6, isNullFirst, belt);
            }
        }
        boolean isNullSecond = rightHand == null;
        boolean isWrongItemSecond = rightHand != null && !(rightHand.item.res.get().name.equals(resourcePath));
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(rightHand.item.res.get().name::contains)) {
            isWrongItemSecond = false;
            gui.ui.error("Item in Right Hand can not be placed in belt. Skipping.");
        }
        if (isNullSecond || isWrongItemSecond) {
            if (!items.isEmpty()) {
                switchItem(items, equipory, 7, isNullSecond, belt);
            }
        }
    }

    public void equipTwoHanderFromBelt(String resourcePath, String itemName) throws InterruptedException {
        Equipory equipory = gui.getequipory();
        WItem leftHand = equipory.slots[6];
        WItem rightHand = equipory.slots[7];
        boolean isTwoHanderEquipped = leftHand != null && leftHand.item.res.get().name.equals(resourcePath);
        if (isTwoHanderEquipped) {
            gui.ui.msg(itemName + " is already equipped.");
            return;
        }
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
                                        (entry.getKey().res.get().name.equals(resourcePath)))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }
        if (items.isEmpty()) {
            gui.ui.error("No " + itemName + " found in Belt.");
            return;
        }
        if (items.size() > 1) {
            items = items.entrySet().stream()
                    .limit(1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        boolean isNullFirst = leftHand == null;
        boolean isNullSecond = rightHand == null;
        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(leftHand.item.res.get().name::contains)) {
            gui.ui.error("Item in Left Hand can not be placed in belt. " + itemName + " could not be equipped.");
            return;
        }
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(rightHand.item.res.get().name::contains)) {
            gui.ui.error("Item in Right Hand can not be placed in belt." + itemName + " could not be equipped.");
            return;
        }
        boolean needExtraEmptyBeltSlot = false;
        if (!isNullFirst && !isNullSecond) {
            if (!isTwoHander(leftHand, rightHand)) {
                needExtraEmptyBeltSlot = true;
            }
        }
        if (needExtraEmptyBeltSlot) {
            if (belt.getFreeSpace() > 0) {
                Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
                items.remove(itemEntry.getKey());
                GItem item = itemEntry.getKey();
                Coord originalPosition = itemEntry.getValue();
                equipory.slots[6].item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                belt.wdgmsg("drop", belt.isRoom(1, 1));
                Thread.sleep(5);
                item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                equipory.wdgmsg("drop", 6);
                Thread.sleep(5);
                belt.wdgmsg("drop", originalPosition);
                Thread.sleep(5);
            } else {
                gui.ui.error("You need an extra empty slot in your Belt to swap your current handheld items for a Two-Handed (" + itemName + ").");
            }
        } else {
            Map.Entry<GItem, Coord> itemEntry = items.entrySet().iterator().next();
            items.remove(itemEntry.getKey());
            GItem item = itemEntry.getKey();
            Coord originalPosition = itemEntry.getValue();
            item.wdgmsg("take", Coord.z);
            Thread.sleep(5);
            equipory.wdgmsg("drop", 6);
            Thread.sleep(5);
            belt.wdgmsg("drop", originalPosition);
            Thread.sleep(5);
        }
    }

    public void equipTwoDifferentItemsFromBelt(String firstResourcePath, String secondResourcePath, String firstItemName, String secondItemName) throws InterruptedException {
        Equipory equipory = gui.getequipory();
        WItem leftHand = equipory.slots[6];
        WItem rightHand = equipory.slots[7];
        boolean isFirstEquippedLeftHand = (leftHand != null && leftHand.item.res.get().name.equals(firstResourcePath));
        boolean isFirstEquippedRightHand = (rightHand != null && rightHand.item.res.get().name.equals(firstResourcePath));
        boolean isFirstEquipped = isFirstEquippedLeftHand || isFirstEquippedRightHand;
        boolean isSecondEquippedLeftHand = (leftHand != null && leftHand.item.res.get().name.equals(secondResourcePath));
        boolean isSecondEquippedRightHand = (rightHand != null && rightHand.item.res.get().name.equals(secondResourcePath));
        boolean isSecondEquipped = isSecondEquippedLeftHand || isSecondEquippedRightHand;
        if (isFirstEquipped && isSecondEquipped) {
            gui.ui.msg(firstItemName + " and " + secondItemName + " are already equipped.");
            return;
        }
        boolean isNullFirst = leftHand == null;
        if (!isNullFirst && Arrays.stream(RESTRICTED_ITEMS).anyMatch(leftHand.item.res.get().name::contains)) {
            gui.ui.error("Item in Left Hand can not be placed in belt. Can't equip items from belt.");
            return;
        }
        boolean isNullSecond = rightHand == null;
        if (!isNullSecond && Arrays.stream(RESTRICTED_ITEMS).anyMatch(rightHand.item.res.get().name::contains)) {
            gui.ui.error("Item in Right Hand can not be placed in belt. Can't equip items from belt.");
            return;
        }
        Inventory belt = null;
        Map<GItem, Coord> firstItemInBelt = new HashMap<>();
        Map<GItem, Coord> secondItemInBelt = new HashMap<>();
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
                        Map<GItem, Coord> finalFirstItems = firstItemInBelt;
                        ((Inventory) ww).wmap.entrySet().stream()
                                .filter(entry -> entry.getValue().c.equals(calculatedCoord) &&
                                        (entry.getKey().res.get().name.equals(firstResourcePath)))
                                .forEach(entry -> finalFirstItems.put(entry.getKey(), indexCoord));

                        Map<GItem, Coord> finalSecondItems = secondItemInBelt;
                        ((Inventory) ww).wmap.entrySet().stream()
                                .filter(entry -> entry.getValue().c.equals(calculatedCoord) &&
                                        (entry.getKey().res.get().name.equals(secondResourcePath)))
                                .forEach(entry -> finalSecondItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }
        if (isFirstEquipped){
            if (secondItemInBelt.isEmpty()) {
                gui.ui.error("No " + secondItemName + " found in the belt.");
                return;
            }
            if (secondItemInBelt.size() > 1) {
                secondItemInBelt = secondItemInBelt.entrySet().stream()
                        .limit(1)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (isFirstEquippedLeftHand){
                switchItem(secondItemInBelt, equipory, 7, isNullFirst, belt);
            } else if (isFirstEquippedRightHand){
                switchItem(secondItemInBelt, equipory, 6, isNullFirst, belt);
            }
        } else if (isSecondEquipped){
            if (firstItemInBelt.isEmpty()) {
                gui.ui.error("No " + firstItemName + " found in the belt.");
                return;
            }
            if (firstItemInBelt.size() > 1) {
                firstItemInBelt = firstItemInBelt.entrySet().stream()
                        .limit(1)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (isSecondEquippedLeftHand){
                switchItem(firstItemInBelt, equipory, 7, isNullFirst, belt);
            } else if (isSecondEquippedRightHand){
                switchItem(firstItemInBelt, equipory, 6, isNullSecond, belt);
            }
        } else {
            if (firstItemInBelt.isEmpty()) {
                gui.ui.error("No " + firstItemName + " found in the belt.");
                return;
            }
            if (secondItemInBelt.isEmpty()) {
                gui.ui.error("No " + secondItemName + " found in the belt.");
                return;
            }
            if (firstItemInBelt.size() > 1) {
                firstItemInBelt = firstItemInBelt.entrySet().stream()
                        .limit(1)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (secondItemInBelt.size() > 1) {
                secondItemInBelt = secondItemInBelt.entrySet().stream()
                        .limit(1)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            switchItem(firstItemInBelt, equipory, 6, isNullSecond, belt);
            switchItem(secondItemInBelt, equipory, 7, isNullFirst, belt);
        }
    }
    private void switchItem(Map<GItem, Coord> itemInBelt, Equipory equipory, int slot, boolean isNull, Inventory belt) throws InterruptedException {
        Map.Entry<GItem, Coord> itemEntry = itemInBelt.entrySet().iterator().next();
        itemInBelt.remove(itemEntry.getKey());
        GItem item = itemEntry.getKey();
        Coord originalPosition = itemEntry.getValue();
        item.wdgmsg("take", Coord.z);
        Thread.sleep(5);
        equipory.wdgmsg("drop", slot);
        Thread.sleep(5);
        if (!isNull) {
            belt.wdgmsg("drop", originalPosition);
            Thread.sleep(5);
        }
    }
}