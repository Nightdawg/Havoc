package haven.automated;

import haven.*;

import java.util.*;
import java.util.stream.Collectors;

public class EquipSacks implements Runnable {
    private GameUI gui;

    public EquipSacks(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            WItem leftHand = gui.getequipory().slots[6];
            WItem rightHand = gui.getequipory().slots[7];

            boolean isLeftHandTravelersSack = leftHand != null && leftHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack");
            boolean isRightHandTravelersSack = rightHand != null && rightHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack");

            boolean isLeftHandWanderersBindle = leftHand != null && leftHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle");
            boolean isRightHandWanderersBindle = rightHand != null && rightHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle");

            if (isLeftHandTravelersSack && isRightHandTravelersSack) {
                gui.ui.msg("Already wearing Traveler Sacks.");
            } else if (isLeftHandWanderersBindle && isRightHandWanderersBindle) {
                gui.ui.msg("Already wearing Wanderer Bindles.");
            } else {
                equipTravellerSacksInBelt();
            }

        } catch (Exception e) {
            gui.ui.error("Error in EquipSacks script.");
            e.printStackTrace();
        }
    }

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
                                        (entry.getKey().res.get().name.equals("gfx/invobjs/small/travellerssack")
                                                || entry.getKey().res.get().name.equals("gfx/invobjs/small/wanderersbindle")))
                                .forEach(entry -> finalItems.put(entry.getKey(), indexCoord));
                    }
                }
            }
        }

        if (items.isEmpty()) {
            System.out.println("No traveller sacks or wanderer bindles found in the inventory.");
            return;
        }

        if (items.size() > 2) {
            items = items.entrySet().stream()
                    .limit(2)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        WItem firstHand = equipory.slots[6];
        boolean isNullFirst = firstHand == null;
        boolean isWrongItemFirst = firstHand != null && !(firstHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack")
                || firstHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle"));
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
        boolean isWrongItemSecond = secondHand != null && !(secondHand.item.res.get().name.equals("gfx/invobjs/small/travellerssack")
                || secondHand.item.res.get().name.equals("gfx/invobjs/small/wanderersbindle"));
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
        gui.ui.msg("Equip T.Sacks script done");
    }

}