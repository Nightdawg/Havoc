package haven.automated;

import com.sun.source.tree.Tree;
import haven.*;
import haven.Button;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;
import haven.automated.helpers.FarmingStatic;
import haven.automated.helpers.TileStatic;
import haven.res.ui.tt.q.quality.Quality;

import java.util.*;

import static haven.OCache.posres;

public class TurnipBot extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop;
    private Coord coordNW;
    private Coord coordSE;

    private final Button fieldSelectButton;
    private final Button granarySelectButton;
    private boolean selectGranary;
    private final Button resetButton;

    private List<TurnipField> fields;
    private final Label fieldsLabel;
    private Gob granary;
    private final Label granaryLabel;

    private boolean plant = true;
    private final CheckBox plantCheckbox;

    private boolean active;
    private final Button startButton;

    private Coord closestFieldCoord = null;
    private int currentField;
    private int stage;

    public TurnipBot(GameUI gui) {
        super(new Coord(300, 200), "TurnipFarmer");
        this.gui = gui;
        this.fields = new ArrayList<>();
        currentField = 0;
        stage = 0;

        fieldSelectButton = add(new Button(60, "Field") {
            @Override
            public void click() {
                selectGranary = false;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select single field.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(15, 15));

        granarySelectButton = add(new Button(60, "Granary") {
            @Override
            public void click() {
                selectGranary = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select area with granary.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(80, 15));

        resetButton = add(new Button(50, "Reset") {
            @Override
            public void click() {
                fields.clear();
                fieldsLabel.settext("Fields: 0");
                granary = null;
                granaryLabel.settext("Granary: ✘");
                active = false;
                startButton.change("Start");
                currentField = 0;
                stage = 0;
                closestFieldCoord = null;
            }
        }, UI.scale(150, 15));

        fieldsLabel = new Label("Fields: 0");
        add(fieldsLabel, UI.scale(50, 50));

        granaryLabel = new Label("Granary: ✘");
        add(granaryLabel, UI.scale(120, 50));

        plantCheckbox = new CheckBox("Plant") {
            {
                a = true;
            }

            public void set(boolean val) {
                plant = val;
                a = val;
            }
        };
        add(plantCheckbox, UI.scale(70, 80));

        startButton = add(new Button(50, "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                } else {
                    this.change("Start");
                }
            }
        }, UI.scale(150, 115));

    }

    @Override
    public void areaselect(Coord a, Coord b) {
        this.coordNW = a.mul(MCache.tilesz2);
        this.coordSE = b.mul(MCache.tilesz2);
        if(selectGranary){
            List<Gob> granaries = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/granary", coordNW, coordSE, gui);
            if(granaries.size() == 1){
                granary = granaries.get(0);
                granaryLabel.settext("Granary: ✔");
                gui.msg("Granary found.");
            } else {
                gui.msg("No granary in selected area.");
            }
            gui.map.unregisterAreaSelect();
        } else {
            fields.add(new TurnipField(fields.size(), coordNW, coordSE));
            fieldsLabel.settext("Fields: " + fields.size());
            gui.msg("Area selected: " + (coordSE.x - coordNW.x) + "x" + (coordSE.y - coordNW.y));
            gui.map.unregisterAreaSelect();
        }
    }

    @Override
    public void run() {
        while (!stop) {
            if (active) {
                if (fields.size() > 0 && granary != null) {
                    checkHealthStaminaEnergy();
                    if(stage == 0){
                        if(getFieldByIndex(currentField).closestCoord == null){
                            setClosestFieldCoord();
                        } else {
                            if(gui.maininv.getFreeSpace() < 15){
                                depositIfFullInventory();
                            } else {
                                gui.map.pfLeftClick(closestFieldCoord, null);
                            }
//                          harvestCurrentField();
                        }
                    } else if (stage == 1){
                        gui.msg("Currently planting field nr: " + (currentField + 1));
//                            getSeedsIfNotEnough();
//                            plantCurrentField();
                    }
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void setClosestFieldCoord(){
        TurnipField field = getFieldByIndex(currentField);
        Coord[] coords = {
                field.fieldNW.add(5, 5),
                new Coord(field.fieldSE.x, field.fieldNW.y).add(-5, 5),
                field.fieldNW.add(-5, -5),
                new Coord(field.fieldNW.x, field.fieldSE.y).add(5, -5)
        };

        Optional<Coord> minCoord = Arrays.stream(coords).min(Comparator.comparingDouble(coord -> coord.dist(granary.rc.floor())));
        field.setClosestCoord(minCoord.get());
        closestFieldCoord = minCoord.get();
    }

    private void depositIfFullInventory() {
        try {
            int freeSpace = gui.maininv.getFreeSpace();
            Thread.sleep(300);
            if (freeSpace < 15 && FarmingStatic.grainSlots.size() == 0) {
                gui.map.pfRightClick(granary, -1, 3, 0, null);
                AUtils.waitPf(gui);
                Thread.sleep(1000);
            } else if (freeSpace < 15 && FarmingStatic.grainSlots.size() == 10) {
                for (WItem wItem : gui.maininv.getAllItems()) {
                    try {
                        if (wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip")) {
                            double quality = 0;
                            int amount = 0;
                            for(ItemInfo info : wItem.item.info()){
                                if(info instanceof Quality){
                                    quality = ((Quality) info).q;
                                } else if (info instanceof GItem.Amount){
                                    amount = ((GItem.Amount) info).itemnum();
                                }
                            }

                            Grainslot firstEmpty = null;
                            Grainslot matchingQl = null;
                            for(Grainslot grainslot : FarmingStatic.grainSlots){
                                if(grainslot.getRawinfo() == null){
                                    firstEmpty = grainslot;
                                } else {
                                    boolean turnip = false;
                                    boolean fitAll = false;
                                    boolean qlMatch = false;
                                    for(ItemInfo info : grainslot.info()){
                                        if(info instanceof GItem.Amount){
                                            if(((GItem.Amount) info).itemnum() + amount <= 200000){
                                                fitAll = true;
                                            }
                                        } else if (info instanceof Quality && ((Quality) info).q == quality){
                                            qlMatch = true;
                                        } else if (info instanceof ItemInfo.Name && ((ItemInfo.Name) info).original.contains("Turnip")){
                                            turnip = true;
                                        }
                                    }
                                    if(turnip && fitAll && qlMatch){
                                        matchingQl = grainslot;
                                    }
                                }
                            }
                            if(matchingQl != null){
                                wItem.item.wdgmsg("take", Coord.z);
                                matchingQl.wdgmsg("drop", 0);
                            } else if (firstEmpty != null) {
                                wItem.item.wdgmsg("take", Coord.z);
                                Thread.sleep(100);
                                firstEmpty.wdgmsg("drop", 0);
                                Thread.sleep(100);
                            } else {
                                active = false;
                                startButton.change("Start");
                                gui.error("No space in granary. Stopping.");
                            }
                        }
                    } catch (Loading e) {
                    }
                }
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void checkHealthStaminaEnergy() {
        if (gui.getmeters("hp").get(1).a < 0.02) {
            System.out.println("Low HP, porting home.");
            gui.act("travel", "hearth");
            try {
                Thread.sleep(8000);
            } catch (InterruptedException ignored) {
            }
        } else if (gui.getmeter("nrj", 0).a < 0.30) {
            gui.error("Energy critical. Farmer stopping.");
            stop();
        } else if (gui.getmeter("stam", 0).a < 0.50) {
            try {
                AUtils.drinkTillFull(gui, 0.99, 0.99);
            } catch (InterruptedException e) {
                System.out.println("Drinking interrupted.");
            }
        }
    }

//    private void processGranary() {
//        try {
//            Gob granary = AUtils.getGobNearPlayer("gfx/terobjs/granary", gui);
//            if (granary != null) {
//                gui.map.pfRightClick(granary, -1, 3, 0, null);
//                AUtils.waitPf(gui);
//                Thread.sleep(1000);
//                while (FarmingStatic.grainSlots.size() == 0) {
//                    Thread.sleep(1000);
//                }
//                int seeds = 0;
//                for (Grainslot grainslot : FarmingStatic.grainSlots) {
//                    try {
//                        if (grainslot.getRawinfo() != null) {
//                            int amount = 0;
//                            boolean turnip = false;
//                            for (ItemInfo info : grainslot.info()) {
//                                if (info instanceof GItem.Amount) {
//                                    amount = ((GItem.Amount) info).itemnum();
//                                }
//                                if (info instanceof ItemInfo.Name) {
//                                    if (((ItemInfo.Name) info).original.contains("Turnip")) {
//                                        turnip = true;
//                                    }
//                                }
//                            }
//                            if (turnip) {
//                                seeds += amount;
//                            }
//                        }
//                    } catch (NullPointerException ignored) {
//                    }
//                }
//                granaries.put(granary, seeds);
//            }
//        } catch (InterruptedException ignored) {
//        }
//    }

//    private void processField(int index) {
//        TurnipField field = getFieldByIndex(index);
//        if (field != null) {
//            List<Gob> crops = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/plants/turnip", field.getFieldNW(), field.getFieldSE(), gui);
//            if (!crops.isEmpty()) {
//                List<Gob> freshCrops = new ArrayList<>();
//                List<Gob> cropsToHarvest = new ArrayList<>();
//                for (Gob crop : crops) {
//                    if (AUtils.getDrawState(crop) == 0) {
//                        freshCrops.add(crop);
//                    } else {
//                        cropsToHarvest.add(crop);
//                    }
//                }
//                field.setTurnipHarvestable(cropsToHarvest);
//                field.setTurnipStageZero(freshCrops);
//            }
//        }
//    }

    public TurnipField getFieldByIndex(int index) {
        for (TurnipField field : fields) {
            if (field.fieldIndex == index) {
                return field;
            }
        }
        return null;
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

    private static class TurnipField {
        private final int fieldIndex;

        private final Coord fieldNW;
        private final Coord fieldSE;
        private Coord closestCoord;

        private List<Gob> turnipStageZero;
        private List<Gob> turnipHarvestable;

        public TurnipField(int fieldIndex, Coord farmNW, Coord farmSE) {
            this.fieldIndex = fieldIndex;
            this.fieldNW = farmNW;
            this.fieldSE = farmSE;
            this.turnipStageZero = new ArrayList<>();
            this.turnipHarvestable = new ArrayList<>();
        }

        public Coord getFieldNW() {
            return fieldNW;
        }

        public Coord getFieldSE() {
            return fieldSE;
        }

        public void setClosestCoord(Coord closestCoord) {
            this.closestCoord = closestCoord;
        }
    }
}
