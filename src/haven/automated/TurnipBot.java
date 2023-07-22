package haven.automated;

import haven.*;
import haven.Button;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;
import haven.automated.helpers.FarmingStatic;
import haven.res.ui.tt.q.quality.Quality;

import java.util.*;

import static haven.OCache.posres;

public class TurnipBot extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop;
    private boolean selectGranary;

    private final List<TurnipField> fields;
    private final Label fieldsLabel;

    private Gob granary;
    private final Label granaryLabel;

    private boolean plant = true;

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

        add(new Button(60, "Field") {
            @Override
            public void click() {
                selectGranary = false;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select single field.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(15, 15));

        add(new Button(60, "Granary") {
            @Override
            public void click() {
                selectGranary = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select area with granary.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(80, 15));

        add(new Button(50, "Reset") {
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

        CheckBox plantCheckbox = new CheckBox("Plant") {
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
                if (fields.size() > 0 && granary != null) {
                    active = !active;
                    if (active) {
                        this.change("Stop");
                    } else {
                        this.change("Start");
                    }
                } else {
                    gui.error("Need to select at least one field and granary.");
                }
            }
        }, UI.scale(150, 115));

    }

    @Override
    public void areaselect(Coord a, Coord b) {
        Coord nw = a.mul(MCache.tilesz2);
        Coord se = b.mul(MCache.tilesz2);
        if (selectGranary) {
            handleGranarySelection(nw, se);
        } else {
            handleFieldSelection(nw, se);
        }
        gui.map.unregisterAreaSelect();
    }

    private void handleGranarySelection(Coord nw, Coord se) {
        List<Gob> granaries = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/granary", nw, se, gui);
        if (granaries.size() == 1) {
            granary = granaries.get(0);
            granaryLabel.settext("Granary: ✔");
            gui.msg("Granary found.");
        } else {
            gui.msg("No granary in selected area.");
        }
    }

    private void handleFieldSelection(Coord nw, Coord se) {
        fields.add(new TurnipField(fields.size(), nw, se));
        fieldsLabel.settext("Fields: " + fields.size());
        gui.msg("Area selected: " + (se.x - nw.x) / 11 + "x" + (se.y - nw.y) / 11);
    }

    @Override
    public void run() {
        while (!stop) {
            if (active) {
                clearhand();
                dropTurnips();
                if (fields.size() > 0 && granary != null) {
                    TurnipField curField = getFieldByIndex(currentField);
                    checkHealthStaminaEnergy();
                    if (curField == null) {
                        resetFarmBot();
                    } else {
                        if (curField.closestCoord == null) {
                            setClosestFieldCoord();
                        } else {
                            handleStage(curField);
                        }
                    }
                }
            }
            sleep(200);
        }
    }

    private void setClosestFieldCoord() {
        TurnipField field = getFieldByIndex(currentField);
        Coord[] coords = {
                field.fieldNW.add(5, 5), // top-left
                new Coord(field.fieldSE.x, field.fieldNW.y).add(-5, 5), // top-right
                field.fieldSE.add(-5, -5), // bottom-right
                new Coord(field.fieldNW.x, field.fieldSE.y).add(5, -5) // bottom-left
        };

        Optional<Coord> minCoord = Arrays.stream(coords).min(Comparator.comparingDouble(coord -> coord.dist(granary.rc.floor())));
        field.setClosestCoord(minCoord.get());
        closestFieldCoord = minCoord.get();
    }

    private void handleStage(TurnipField curField) {
        switch (stage) {
            case 0:
                handleStage0(curField);
                break;
            case 1:
                handleStage1();
                break;
            case 2:
                handleStage2(curField);
                break;
        }
    }

    private void handleStage0(TurnipField curField) {
        if (gui.maininv.getFreeSpace() < 1) {
            depositIfFullInventory();
        } else {
            processField(curField);
        }
    }

    private void handleStage1() {
        if (checkIfSeedsInInventory()) {
            depositAllSeeds();
        } else {
            stage = 2;
            gui.msg("Seeds deposited, planting.");
        }
    }

    private void handleStage2(TurnipField curField) {
        List<Gob> gobs = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/plants/turnip", curField.getFieldNW(), curField.getFieldSE(), gui);
        if (!checkIfSeedsInInventory() && gobs.size() < curField.size) {
            getHighestQualitySeeds();
        } else if (checkIfSeedsInInventory() && gobs.size() < curField.size) {
            plantSeeds(curField);
        } else if (gobs.size() >= curField.size) {
            stage = 0;
            currentField++;
            gui.msg("Going for next field.");
        }
    }

    private void processField(TurnipField curField) {
        if (!AUtils.isPlayerInSelectedArea(curField.getFieldNW(), curField.getFieldSE(), gui)) {
            try {
                Thread.sleep(1000);
                gui.map.pfLeftClick(closestFieldCoord, null);
                AUtils.waitPf(gui);
            } catch (InterruptedException ignored) {
            }
        } else {
            Gob closest = AUtils.getClosestCropInSelectionStartingWith("gfx/terobjs/plants/turnip", curField.getFieldNW(), curField.getFieldSE(), gui, 1);
            if (closest == null) {
                if (plant) {
                    stage = 1;
                    gui.msg("Depositing seeds");
                } else {
                    gui.msg("Going for next field.");
                    currentField++;
                }
            } else {
                if (gui.map.player().getv() == 0 && gui.prog == null) {
                    AUtils.rightClickGob(gui, closest, 1);
                    gui.map.wdgmsg("sel", curField.fieldNW.div(11), curField.fieldSE.div(11).sub(1, 1), 0);
                } else {
                    sleep(1000);
                }
            }
        }
    }

    private void plantSeeds(TurnipField curField) {
        if (!AUtils.isPlayerInSelectedArea(curField.getFieldNW(), curField.getFieldSE(), gui)) {
            try {
                Thread.sleep(1000);
                gui.map.pfLeftClick(closestFieldCoord, null);
                AUtils.waitPf(gui);
            } catch (InterruptedException ignored) {
            }
        } else {
            if (gui.map.player().getv() == 0 && gui.prog == null) {
                GItem firstSeedInInventory = null;
                for (WItem wItem : gui.maininv.getAllItems()) {
                    try {
                        if (wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip")) {
                            firstSeedInInventory = wItem.item;
                        }
                    } catch (Loading e) {
                    }
                }
                if (firstSeedInInventory != null) {
                    firstSeedInInventory.wdgmsg("iact", Coord.z, 1);
                    gui.map.wdgmsg("sel", curField.fieldNW.div(11), curField.fieldSE.div(11).sub(1, 1), 0);
                } else {
                    gui.error("Something went wrong.");
                }
            } else {
                sleep(1000);
            }
        }
    }

    private boolean checkIfSeedsInInventory() {
        boolean seeds = false;
        for (WItem wItem : gui.maininv.getAllItems()) {
            try {
                if (wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip")) {
                    seeds = true;
                }
            } catch (Loading e) {
            }
        }
        return seeds;
    }

    private void getHighestQualitySeeds() {
        try {
            Thread.sleep(300);
            if (FarmingStatic.grainSlots.size() == 0) {
                gui.map.pfRightClick(granary, -1, 3, 0, null);
                AUtils.waitPf(gui);
                Thread.sleep(1000);
            } else if (FarmingStatic.grainSlots.size() == 10) {
                takeBestSeeds();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void takeBestSeeds() {
        Grainslot best = FarmingStatic.grainSlots.stream()
                .filter(grainslot -> grainslot.getRawinfo() != null)
                .filter(grainslot -> {
                    boolean turnip = grainslot.info().stream().anyMatch(info -> info instanceof ItemInfo.Name && ((ItemInfo.Name) info).original.contains("Turnip"));
                    boolean enoughForField = grainslot.info().stream().anyMatch(info -> info instanceof GItem.Amount && ((GItem.Amount) info).itemnum() >= gui.maininv.getFreeSpace() * 50);
                    double qualityTemp = grainslot.info().stream().filter(info -> info instanceof Quality).mapToDouble(info -> ((Quality) info).q).findFirst().orElse(0.0);
                    boolean betterQl = qualityTemp > 0;
                    return turnip && betterQl && enoughForField;
                })
                .findFirst().orElse(null);

        if (best != null) {
            while (gui.maininv.getFreeSpace() > 0) {
                takeSeeds(best, gui.maininv.getFreeSpace());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void takeSeeds(Grainslot best, int freeSpace) {
        for (int i = 0; i < freeSpace; i++) {
            best.wdgmsg("take");
        }
    }

    private void depositAllSeeds() {
        handleSeedsDeposit(false);
    }

    private void depositIfFullInventory() {
        handleSeedsDeposit(true);
    }

    private void handleSeedsDeposit(boolean checkFreeSpace) {
        try {
            int freeSpace = checkFreeSpace ? gui.maininv.getFreeSpace() : 1; // Assign freeSpace value only if checkFreeSpace is true

            Thread.sleep(300);
            if (freeSpace < 1 && FarmingStatic.grainSlots.size() == 0) {
                gui.map.pfRightClick(granary, -1, 3, 0, null);
                AUtils.waitPf(gui);
                Thread.sleep(1000);
            } else if (freeSpace < 1 && FarmingStatic.grainSlots.size() == 10) {
                iterateThroughSeeds();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void iterateThroughSeeds() {
        gui.maininv.getAllItems().stream()
                .filter(wItem -> wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip"))
                .forEach(wItem -> {
                    try {
                        double quality = wItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
                        int amount = wItem.item.info().stream().filter(info -> info instanceof GItem.Amount).mapToInt(info -> ((GItem.Amount) info).itemnum()).findFirst().orElse(0);

                        Grainslot firstEmpty = FarmingStatic.grainSlots.stream().filter(grainslot -> grainslot.getRawinfo() == null).findFirst().orElse(null);
                        Grainslot matchingQl = FarmingStatic.grainSlots.stream()
                                .filter(grainslot -> grainslot.getRawinfo() != null)
                                .filter(grainslot -> {
                                    boolean turnip = grainslot.info().stream().anyMatch(info -> info instanceof ItemInfo.Name && ((ItemInfo.Name) info).original.contains("Turnip"));
                                    boolean fitAll = grainslot.info().stream().anyMatch(info -> info instanceof GItem.Amount && ((GItem.Amount) info).itemnum() + amount <= 200000);
                                    boolean qlMatch = grainslot.info().stream().anyMatch(info -> info instanceof Quality && ((Quality) info).q == quality);
                                    return turnip && fitAll && qlMatch;
                                })
                                .findFirst().orElse(null);

                        handleGrainslot(wItem, matchingQl, firstEmpty);
                    } catch (Loading ignored) {
                    }
                });
    }

    private void handleGrainslot(WItem wItem, Grainslot matchingQl, Grainslot firstEmpty) {
        if (matchingQl != null) {
            moveItem(wItem, matchingQl);
        } else if (firstEmpty != null) {
            moveItem(wItem, firstEmpty);
        } else {
            active = false;
            startButton.change("Start");
            gui.error("No space in granary. Stopping.");
        }
    }

    private void clearhand() {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            }
        }
        AUtils.rightClick(gui);
    }

    private void dropTurnips() {
        for (WItem wItem : gameui().maininv.getAllItems()) {
            GItem gitem = wItem.item;
            if (gitem.getname().equals("Turnip") || gitem.getname().equals("Turnip, stack of")) {
                gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2));
            }
        }
    }

    private void moveItem(WItem wItem, Grainslot grainslot) {
        wItem.item.wdgmsg("take", Coord.z);
        grainslot.wdgmsg("drop", 0);
    }

    private void checkHealthStaminaEnergy() {
        if (gui.getmeters("hp").get(1).a < 0.1) {
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
                AUtils.drinkTillFull(gui, 0.98, 0.98);
            } catch (InterruptedException e) {
                System.out.println("Drinking interrupted.");
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

    private void resetFarmBot() {
        gui.msg("Farming Finished.");
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

    private static class TurnipField {
        private final int fieldIndex;
        private final int size;
        private final int height;
        private final int width;

        private final Coord fieldNW;
        private final Coord fieldSE;
        private Coord closestCoord;

        public TurnipField(int fieldIndex, Coord farmNW, Coord farmSE) {
            this.fieldIndex = fieldIndex;
            this.height = (farmSE.y - farmNW.y) / 11;
            this.width = (farmSE.x - farmNW.x) / 11;
            this.size = height * width;
            this.fieldNW = farmNW;
            this.fieldSE = farmSE;
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
