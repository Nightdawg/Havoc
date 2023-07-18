package haven.automated;

import haven.*;
import haven.Button;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;
import haven.automated.helpers.FarmingStatic;

import java.util.*;

import static haven.OCache.posres;

public class TurnipBot extends Window implements Runnable, AreaSelectCallback {
    private List<TurnipField> fields;
    private Map<Gob, Integer> granaries;
    private final GameUI gui;
    private boolean stop;
    private Coord farmNW;
    private Coord farmSE;
    private int stage;
    private final List<Integer> fieldsSelected;
    private boolean harvest;
    private boolean plant;
    private List<Coord2d> coordQueue;
    private int scanIndex;

    private final Button areaSelectButton;
    private final Button resetButton;
    private final CheckBox fieldOneCb;
    private final CheckBox fieldTwoCb;
    private final CheckBox fieldThreeCb;
    private final CheckBox fieldFourCb;
    private final CheckBox fieldFiveCb;
    private final CheckBox fieldSixCb;
    private final CheckBox fieldSevenCb;
    private final CheckBox fieldEightCb;
    private final CheckBox fieldNineCb;
    private final CheckBox fieldTenCb;
    private final CheckBox fieldElevenCb;
    private final CheckBox fieldTwelveCb;
    private final Button startScanningButton;
    private final CheckBox harvestCheckbox;
    private final CheckBox plantCheckbox;
    private Button startBot;

    public TurnipBot(GameUI gui) {
        super(new Coord(300, 200), "TurnipFarmer");
        this.gui = gui;
        setStageZero();

        areaSelectButton = add(new Button(100, "Select Area") {
            @Override
            public void click() {
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select Farm Grid");
                gui.map.areaSelect = true;
            }
        }, UI.scale(15, 15));

        resetButton = add(new Button(100, "Reset") {
            @Override
            public void click() {
                setStageZero();
                stop();
            }
        }, UI.scale(150, 15));

        fieldOneCb = new CheckBox("1") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 1);
                } else {
                    fieldsSelected.add(1);
                }
                a = val;
            }
        };
        add(fieldOneCb, UI.scale(15, 60));

        fieldTwoCb = new CheckBox("2") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 2);
                } else {
                    fieldsSelected.add(2);
                }
                a = val;
            }
        };
        add(fieldTwoCb, UI.scale(55, 60));

        fieldThreeCb = new CheckBox("3") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 3);
                } else {
                    fieldsSelected.add(3);
                }
                a = val;
            }
        };
        add(fieldThreeCb, UI.scale(95, 60));

        fieldFourCb = new CheckBox("4") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 4);
                } else {
                    fieldsSelected.add(4);
                }
                a = val;
            }
        };
        add(fieldFourCb, UI.scale(135, 60));

        fieldFiveCb = new CheckBox("5") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 5);
                } else {
                    fieldsSelected.add(5);
                }
                a = val;
            }
        };
        add(fieldFiveCb, UI.scale(175, 60));

        fieldSixCb = new CheckBox("6") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 6);
                } else {
                    fieldsSelected.add(6);
                }
                a = val;
            }
        };
        add(fieldSixCb, UI.scale(215, 60));

        fieldSevenCb = new CheckBox("7") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 7);
                } else {
                    fieldsSelected.add(7);
                }
                a = val;
            }
        };
        add(fieldSevenCb, UI.scale(15, 90));

        fieldEightCb = new CheckBox("8") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 8);
                } else {
                    fieldsSelected.add(8);
                }
                a = val;
            }
        };
        add(fieldEightCb, UI.scale(55, 90));

        fieldNineCb = new CheckBox("9") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 9);
                } else {
                    fieldsSelected.add(9);
                }
                a = val;
            }
        };
        add(fieldNineCb, UI.scale(95, 90));

        fieldTenCb = new CheckBox("10") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 10);
                } else {
                    fieldsSelected.add(10);
                }
                a = val;
            }
        };
        add(fieldTenCb, UI.scale(135, 90));


        fieldElevenCb = new CheckBox("11") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 11);
                } else {
                    fieldsSelected.add(11);
                }
                a = val;
            }
        };
        add(fieldElevenCb, UI.scale(175, 90));

        fieldTwelveCb = new CheckBox("12") {
            {
                a = true;
            }

            public void set(boolean val) {
                if (!val) {
                    fieldsSelected.remove((Integer) 12);
                } else {
                    fieldsSelected.add(12);
                }
                a = val;
            }
        };
        add(fieldTwelveCb, UI.scale(215, 90));

        fieldsSelected = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));

        harvestCheckbox = new CheckBox("Harvest") {
            {
                a = true;
            }

            public void set(boolean val) {
                harvest = val;
                a = val;
            }
        };
        add(harvestCheckbox, UI.scale(15, 120));

        plantCheckbox = new CheckBox("Plant") {
            {
                a = true;
            }

            public void set(boolean val) {
                plant = val;
                a = val;
            }
        };
        add(plantCheckbox, UI.scale(80, 120));

        startScanningButton = add(new Button(50, "Scan") {
            @Override
            public void click() {
                setStageOne();
            }
        }, UI.scale(150, 115));

    }

    @Override
    public void areaselect(Coord a, Coord b) {
        if (b.mul(MCache.tilesz2).x - a.mul(MCache.tilesz2).x == 1089 && b.mul(MCache.tilesz2).y - a.mul(MCache.tilesz2).y == 1089) {
            this.farmNW = a.mul(MCache.tilesz2);
            this.farmSE = b.mul(MCache.tilesz2);
            gui.msg("Area selected: " + (farmSE.x - farmNW.x) + "x" + (farmSE.y - farmNW.y));
            gui.map.unregisterAreaSelect();
        } else {
            gui.msg("Incorrect size. You must select your entire farm plot 99x99 including palisade.");
            gui.map.unregisterAreaSelect();
        }
    }


    @Override
    public void run() {
        while (!stop) {
            if (stage > 0 && farmNW != null && farmSE != null) {
                checkHealthStaminaEnergy();
            }

            if (stage == 1 && farmNW != null && farmSE != null) {
                scanFieldsAndGranaries();
            }

            if (stage == 2 && farmNW != null && farmSE != null) {
                depositIfFullInventory();
                harvestFieldByField();
            }




            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }


    private void setStageZero() {
        stage = 0;
        farmNW = null;
        farmSE = null;
        gui.msg("Select field area, and choose fields & actions.");
    }

    private void setStageOne() {
        if (farmNW != null && farmSE != null) {
            if (fieldsSelected.size() == 0) {
                gui.error("Select at least one field.");
            } else {
                Collections.sort(fieldsSelected);
                fields = new ArrayList<>();
                granaries = new HashMap<>();
                for (int i : fieldsSelected){
                    fields.add(new TurnipField(i, farmNW));
                }

                scanIndex = 0;
                coordQueue = new ArrayList<>();
                coordQueue.add(new Coord2d(farmNW).add(280.5, 434.5));
                coordQueue.add(new Coord2d(farmNW).add(225.5, 544.5));
                coordQueue.add(new Coord2d(farmNW).add(280.5, 654.5));

                coordQueue.add(new Coord2d(farmNW).add(544.5, 522.5));

                coordQueue.add(new Coord2d(farmNW).add(808.5, 434.5));
                coordQueue.add(new Coord2d(farmNW).add(863.5, 544.5));
                coordQueue.add(new Coord2d(farmNW).add(808.5, 654.5));


                stage = 1;
                gui.msg("Stage one. Scanning, please wait.");
            }
        } else {
            gui.error("Need to select area first.");
        }
    }

    private void setStageTwo() {
        stage = 2;
        gui.msg("Currently Harvesting field nr: " + "todo number");
    }

    private void setStageThree() {
        stage = 3;
        for(TurnipField field : fields){
            System.out.println("Field nr: " + field.fieldIndex + ", coords from: " + field.fieldNW + " to: " + field.fieldSE + ", turnipsZero: " + field.turnipStageZero.size() + ", turnipsToHarvest: " + field.turnipHarvestable.size());
        }
        for(Map.Entry<Gob, Integer> entry : granaries.entrySet()){
            System.out.println(entry.getKey().id + " - " + entry.getValue());
        }

        gui.msg("Currently planting field nr: " + "todo number");
    }

    private void depositIfFullInventory(){
        //todo
    }

    private void harvestFieldByField(){
        //todo
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

    private void scanFieldsAndGranaries() {
        try {
            if (scanIndex == 0) {
                gui.map.pfLeftClick(coordQueue.get(0).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(0)) < 5) {
                    processField(1);
                    processField(2);
                    processField(3);
                    scanIndex++;
                }
            } else if (scanIndex == 1) {
                gui.map.pfLeftClick(coordQueue.get(1).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(1)) < 5) {
                    processGranary();
                    scanIndex++;
                }
            } else if (scanIndex == 2) {
                gui.map.pfLeftClick(coordQueue.get(2).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(2)) < 5) {
                    processField(7);
                    processField(8);
                    processField(9);
                    scanIndex++;
                }
            } else if (scanIndex == 3) {
                gui.map.pfLeftClick(coordQueue.get(3).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(3)) < 5) {
                    processGranary();
                    scanIndex++;
                }
            } else if (scanIndex == 4) {
                gui.map.pfLeftClick(coordQueue.get(4).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(4)) < 5) {
                    processField(4);
                    processField(5);
                    processField(6);
                    scanIndex++;
                }
            } else if (scanIndex == 5) {
                gui.map.pfLeftClick(coordQueue.get(5).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(5)) < 5) {
                    processGranary();
                    scanIndex++;
                }
            } else if (scanIndex == 6) {
                gui.map.pfLeftClick(coordQueue.get(6).floor(), null);
                AUtils.waitPf(gui);
                if (gui.map.player().rc.dist(coordQueue.get(6)) < 5) {
                    processField(10);
                    processField(11);
                    processField(12);
                    scanIndex++;
                }
            } else {
                if (harvest) {
                    setStageTwo();
                } else {
                    setStageThree();
                }
            }

            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Bot interrupted?");
        }
    }

    private void processGranary(){
        try {
        Gob granary = AUtils.getGobNearPlayer("gfx/terobjs/granary", gui);
        if(granary != null){
            gui.map.pfRightClick(granary, -1, 3, 0 , null);
            AUtils.waitPf(gui);
            Thread.sleep(1000);
            while(FarmingStatic.grainSlots.size() == 0){
                Thread.sleep(1000);
            }
            int seeds = 0;
            for(Grainslot grainslot: FarmingStatic.grainSlots){
                try {
                    if(grainslot.getRawinfo() != null){
                        int amount = 0;
                        boolean turnip = false;
                        for(ItemInfo info : grainslot.info()){
                            if(info instanceof GItem.Amount){
                                amount = ((GItem.Amount) info).itemnum();
                            }
                            if(info instanceof ItemInfo.Name){
                                if(((ItemInfo.Name) info).original.contains("Turnip")){
                                    turnip = true;
                                }
                            }
                        }
                        if(turnip){
                            seeds += amount;
                        }
                    }
                } catch (NullPointerException ignored){}
            }
            granaries.put(granary, seeds);
        }
        } catch (InterruptedException ignored){}
    }

    private void processField(int index) {
        TurnipField field = getFieldByIndex(index);
        if (field != null) {
            List<Gob> crops = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/plants/turnip", field.getFieldNW(), field.getFieldSE(), gui);
            if (!crops.isEmpty()) {
                List<Gob> freshCrops = new ArrayList<>();
                List<Gob> cropsToHarvest = new ArrayList<>();
                for (Gob crop : crops) {
                    if (AUtils.getDrawState(crop) == 0) {
                        freshCrops.add(crop);
                    } else {
                        cropsToHarvest.add(crop);
                    }
                }
                field.setTurnipHarvestable(cropsToHarvest);
                field.setTurnipStageZero(freshCrops);
            }
        }
    }

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
        stage = 0;
        ui.root.wdgmsg("gk", 27);
        this.destroy();
    }

    private static class TurnipField {
        private final int fieldIndex;

        private int line;
        private int chunk;

        private final Coord fieldNW;
        private final Coord fieldSE;

        private List<Gob> turnipStageZero;
        private List<Gob> turnipHarvestable;

        public TurnipField(int fieldIndex, Coord farmNW) {
            this.fieldIndex = fieldIndex;
            Coord calculated = calculateNW(fieldIndex, farmNW);
            this.fieldNW = calculated;
            this.fieldSE = calculated.add(165, 506);
            this.turnipStageZero = new ArrayList<>();
            this.turnipHarvestable = new ArrayList<>();
            this.line = 0;
            this.chunk = 0;
        }

        public Coord getFieldNW() {
            return fieldNW;
        }

        public Coord getFieldSE() {
            return fieldSE;
        }

        private Coord calculateNW(int index, Coord farmNW){
            Coord coord;
            if(index < 7){
                coord = new Coord(farmNW.x + 22 + ((index - 1) * 187) , farmNW.y + 11);
            } else {
                coord = new Coord(farmNW.x + 22 + ((index - 7) * 187) , farmNW.y + 572);
            }
            return coord;
        }

        public void setTurnipStageZero(List<Gob> turnipStageZero) {
            this.turnipStageZero = turnipStageZero;
        }

        public void setTurnipHarvestable(List<Gob> turnipHarvestable) {
            this.turnipHarvestable = turnipHarvestable;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public void setChunk(int chunk) {
            this.chunk = chunk;
        }
    }
}
