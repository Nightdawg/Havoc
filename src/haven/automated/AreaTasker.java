package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;
import haven.ScrollableWidgetList;

import java.util.*;

import static haven.OCache.posres;

public class AreaTasker extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop = false;

    private CheckBox[] checkBoxes = new CheckBox[5];

    private String areaSelectionType = "";

    //terobjs
    private Coord workingAreaNW = null;
    private Coord workingAreaSE = null;
    Map<Gob, Integer> gobsInWorkingArea = null;
    private ScrollableWidgetList<GobSelection> gobSelectionList;

    //petals
    private ScrollableWidgetList<PetalSelection> petalSelectionList;
    private Button refreshPetalsButton;

    //storage
    private Coord storageAreaNW = null;
    private Coord storageAreaSE = null;

    //invobjs
    private ScrollableWidgetList<StorableItems> storableItems;
    private Button refreshStorableButton;


    public AreaTasker(GameUI gui) {
        super(new Coord(160, 200), "Area Tasker");
        this.gui = gui;

        //Permanent
        add(new Button(130, "Select Working Area"){
            @Override
            public void click() {
                areaSelectionType = "working";
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select working area.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(10, 3));
        addCheckboxes();

        //Mode 1
        gobSelectionList = new ScrollableWidgetList<>(300, 10, GobSelection.class);
        add(gobSelectionList, UI.scale(170, 10));
        gobSelectionList.hide();

        //Mode 2
        petalSelectionList = new ScrollableWidgetList<>(300, 10, PetalSelection.class);
        add(petalSelectionList, UI.scale(170, 10));
        petalSelectionList.hide();
        refreshPetals();
        refreshPetalsButton = add(new Button(100, "Refresh petals"){
            @Override
            public void click() {
                refreshPetals();
            }
        }, UI.scale(185, 310));
        refreshPetalsButton.hide();

        //Mode 4
        storableItems = new ScrollableWidgetList<>(300, 10, StorableItems.class);
        add(storableItems, UI.scale(170, 10));
        storableItems.hide();
        refreshStorable();
        refreshStorableButton = add(new Button(100, "Refresh items"){
            @Override
            public void click() {
                refreshStorable();
            }
        }, UI.scale(185, 310));
        refreshStorableButton.hide();

    }

    private void addCheckboxes(){
        String[] labels = {
                "Compact Mode",
                "Gob Selection",
                "Petal Selection",
                "Storage Selection",
                "Items To Storage"
        };

        for (int i = 0; i < labels.length; i++) {
            final int mode = i + 1;  // Since your modes are 1-indexed
            checkBoxes[i] = new CheckBox(labels[i]) {
                {
                    a = mode == 1;  // Only "Hidden" is initially true
                }

                public void set(boolean val) {
                    if (val) {
                        changeVisibilityMode(mode);
                    }
                }
            };
            add(checkBoxes[i], UI.scale(10, 63 + i * 30));
        }
    }

    public void changeVisibilityMode(int mode) {
        if (mode == 2 && (gobsInWorkingArea == null || gobsInWorkingArea.size() == 0)) {
            gui.error("Firstly select are containing gobs to process.");
            return;
        }

        for (int i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].a = i + 1 == mode;
            modeTasks[i].run();
        }
    }

    private final Runnable[] modeTasks = {
            () -> processMode0(checkBoxes[0].a),
            () -> processMode1(checkBoxes[1].a),
            () -> processMode2(checkBoxes[2].a),
            () -> processMode3(checkBoxes[3].a),
            () -> processMode4(checkBoxes[4].a)
    };

    public void processMode0(boolean show){
        if(show){
            this.resize(160, 200);
        } else {

        }
    }

    public void processMode1(boolean show){
        if(show){
            this.resize(480, 340);
            gobSelectionList.show();
        } else {
            gobSelectionList.hide();
        }
    }

    public void processMode2(boolean show){
        if(show){
            this.resize(340, 340);
            petalSelectionList.show();
            refreshPetalsButton.show();
        } else {
            petalSelectionList.hide();
            refreshPetalsButton.hide();
        }
    }

    public void processMode3(boolean show){
        if(show){
            this.resize(480, 400);
        } else {

        }
    }

    public void processMode4(boolean show){
        if(show){
            this.resize(500, 340);
            storableItems.show();
            refreshStorableButton.show();
        } else {
            storableItems.hide();
            refreshStorableButton.hide();
        }
    }

    public void refreshPetals(){
        petalSelectionList.removeAllItems();
        for(Map.Entry<String, Boolean> petal : FlowerMenu.autoChoose.entrySet()){
            petalSelectionList.addItem(new PetalSelection(petal.getKey()));
        }
    }

    public void refreshStorable(){
        storableItems.removeAllItems();
        Set<String> uniqueItems = new HashSet<>();
        for(WItem item : gui.maininv.getAllItems()){
            if(item.item != null && item.item.getres() != null){
                uniqueItems.add(item.item.getname());
            }
        }
        for(String item: uniqueItems){
            storableItems.addItem(new StorableItems(item));
        }
    }

    @Override
    public void areaselect(Coord a, Coord b) {
        Coord northWest = a.mul(MCache.tilesz2);
        Coord southEast = b.mul(MCache.tilesz2);
        switch (areaSelectionType) {
            case "working":
                workingAreaNW = northWest;
                workingAreaSE = southEast;
                gobsInWorkingArea = AUtils.getGobsInSelectedArea(workingAreaNW, workingAreaSE, gui);
                if(gobsInWorkingArea.size() == 0){
                    gui.error("There are no gobs in selected area. Try selecting different area.");
                } else {
                    gobSelectionList.removeAllItems();
                    for(Map.Entry<Gob, Integer> gobList : gobsInWorkingArea.entrySet()){
                        gobSelectionList.addItem(new GobSelection(gobList.getKey().getres().name, gobList.getValue()));
                    }
                    checkBoxes[1].set(true);
                    gui.msg("Working area selected. Choose gobs from area to process.");
                }
                break;
            case "storage":
                storageAreaNW = northWest;
                storageAreaSE = southEast;
                gui.msg("Storage / Stockpile area selected");
                break;
            case "other":
                //TODO Inne
                //System.out.println("It's other");
                break;
            default:
                gui.error("Error in area selection. Try again.");
                break;
        }
        gui.map.unregisterAreaSelect();
    }

    @Override
    public void run() {
        while(!stop){

            sleep(500);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {}
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

    public static class GobSelection extends Widget{
        private String name;
        private Label countLbl;
        private int count;
        private CheckBox selectedCb;
        private boolean selected;

        public GobSelection(String name, int count) {
            String result = Character.toUpperCase(name.charAt(name.lastIndexOf("/") + 1)) + name.substring(name.lastIndexOf("/") + 2)
                    + " (" + name.substring(0, name.lastIndexOf("/")) + ")";

            selectedCb = new CheckBox(result) {
                {
                    a = false;
                }

                public void set(boolean val) {
                    selected = val;
                    a = val;
                }
            };
            this.name = name;
            this.selected = false;

            countLbl = new Label(String.valueOf(count), 100);
            this.count = count;

            add(selectedCb, UI.scale(10, 4));
            add(countLbl, UI.scale(250, 4));
        }
    }

    public static class PetalSelection extends Widget{
        private String name;
        private CheckBox selectedCb;
        private boolean selected;

        public PetalSelection(String name) {
            selectedCb = new CheckBox(name) {
                {
                    a = false;
                }

                public void set(boolean val) {
                    selected = val;
                    a = val;
                }
            };
            this.name = name;
            this.selected = false;

            add(selectedCb, UI.scale(10, 4));
        }
    }

    public static class StorableItems extends Widget{
        private String name;
        private CheckBox selectedCb;
        private CheckBox stockpileCb;
        private boolean selected;
        private boolean stockpile;

        public StorableItems(String name) {
            selectedCb = new CheckBox(name) {
                {
                    a = false;
                }

                public void set(boolean val) {
                    selected = val;
                    a = val;
                }
            };

            stockpileCb = new CheckBox("Stockpileable") {
                {
                    a = false;
                }

                public void set(boolean val) {
                    stockpile = val;
                    a = val;
                }
            };
            this.name = name;
            this.selected = false;
            this.stockpile = false;

            add(selectedCb, UI.scale(10, 4));
            add(stockpileCb, UI.scale(200, 4));
        }
    }
}
