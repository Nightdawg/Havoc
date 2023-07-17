package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;

import java.util.*;

import static haven.OCache.posres;

public class TurnipBot extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop;

    private int stage = 0;

    //Permanent button to select area after;
    private Button areaSelectButton;

    private Coord farmNW;
    private Coord farmSE;

    private Button resetButton;

    //Selecting area changes stage to 1;

    //Visible only in stage 1;
    private CheckBox fieldOneCb;
    private CheckBox fieldTwoCb;
    private CheckBox fieldThreeCb;
    private CheckBox fieldFourCb;
    private CheckBox fieldFiveCb;
    private CheckBox fieldSixCb;
    private CheckBox fieldSevenCb;
    private CheckBox fieldEightCb;
    private CheckBox fieldNineCb;
    private CheckBox fieldTenCb;
    private CheckBox fieldElevenCb;
    private CheckBox fieldTwelveCb;

    private Button startScanningButton;

    List<Integer> fieldsSelected;
    private boolean harvest;
    private boolean plant;

    //After its finished set stage to 2

    //Visible in stage 2;
    private CheckBox harvestCheckbox;
    private CheckBox plantCheckbox;

    private Button startBot;
    //Clicking goes to stage 2 if harvest or harvest + plant selected;
    //Clicking goes to stage 3 if plant only selected;
    //gui msg to select at least one is not selected;

    // visible in stage 3,4
    private Label cropsLeftToHarvestLabel;
    private Label cropsLeftToPlantLabel;

    private int currentField;


    public TurnipBot(GameUI gui) {
        super(new Coord(300, 200), "TurnipFarmer");
        this.gui = gui;
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
            }
        }, UI.scale(150, 15));


//        fieldsLeftLabel = new Label("Fields: 0");
//        add(fieldsLeftLabel, new Coord(15, 60));
    }

    @Override
    public void areaselect(Coord a, Coord b) {
        if(b.mul(MCache.tilesz2).x - a.mul(MCache.tilesz2).x < 1089 &&  b.mul(MCache.tilesz2).y - a.mul(MCache.tilesz2).y < 1089){
            this.farmNW = a.mul(MCache.tilesz2);
            this.farmSE = b.mul(MCache.tilesz2);
            gui.msg("Area selected: " + (farmSE.x - farmNW.x) + "x" + (farmSE.y - farmNW.y));
            gui.map.unregisterAreaSelect();
            setStageOne();
        } else {
            gui.msg("Incorrect size. You must select your entire farm plot 99x99 including palisade.");
            gui.map.unregisterAreaSelect();
        }
    }



    @Override
    public void run() {
        while (!stop) {
            if("active" == "active"){
                if (gameui().getmeter("nrj", 0).a < 0.25) {
                    gui.error("Energy critical. Farmer stopping.");
                    stop();
                }
                else if (gui.getmeter("stam", 0).a < 0.40) {
                    try {
                        AUtils.drinkTillFull(gui, 0.99, 0.99);
                    } catch (InterruptedException e) {
                        System.out.println("Drinking interrupted.");
                    }
                }








            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
    }







    private void setStageZero(){
        stage = 0;

        if(farmNW != null){
            farmNW = null;
        }
        if(farmSE != null){
            farmSE = null;
        }

        if(fieldOneCb != null){fieldOneCb.remove();}
        if(fieldTwoCb != null){fieldTwoCb.remove();}
        if(fieldThreeCb != null){fieldThreeCb.remove();}
        if(fieldFourCb != null){fieldFourCb.remove();}
        if(fieldFiveCb != null){fieldFiveCb.remove();}
        if(fieldSixCb != null){fieldSixCb.remove();}
        if(fieldSevenCb != null){fieldSevenCb.remove();}
        if(fieldEightCb != null){fieldEightCb.remove();}
        if(fieldNineCb != null){fieldNineCb.remove();}
        if(fieldTenCb != null){fieldTenCb.remove();}
        if(fieldElevenCb != null){fieldElevenCb.remove();}
        if(fieldTwelveCb != null){fieldTwelveCb.remove();}

        if(startScanningButton != null){startScanningButton.remove();}

        if(harvestCheckbox != null){harvestCheckbox.remove();}
        if(plantCheckbox != null){plantCheckbox.remove();}

        if(startBot != null){startBot.remove();}

        if(cropsLeftToHarvestLabel != null){cropsLeftToHarvestLabel.remove();}
        if(cropsLeftToPlantLabel != null){cropsLeftToPlantLabel.remove();}

        gui.msg("Turnip Bot reset. Select area again.");
    }

    private void setStageOne(){
        stage = 1;
        fieldOneCb = new CheckBox("1") {
            {
                a = true;
            }
            public void set(boolean val) {
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
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
                if(!val){
                    fieldsSelected.remove((Integer) 12);
                } else {
                    fieldsSelected.add(12);
                }
                a = val;
            }
        };
        add(fieldTwelveCb, UI.scale(215, 90));

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
                setStageTwo();
            }
        }, UI.scale(150, 115));

        fieldsSelected = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        harvest = true;
        plant = true;

        gui.msg("Area Selected, Choose fields and actions.");
    }

    private void setStageTwo(){
        Collections.sort(fieldsSelected);
        System.out.println(fieldsSelected.toString());
        System.out.println("harvest: " + harvest + " plant: " + plant);

        gui.msg("Bot is now scanning area & calculating granary space.");
    }

    private void setStageThree(){
        stage = 3;


        gui.msg("Currently Harvesting field nr: " + "todo number");
    }

    private void setStageFour(){
        stage = 4;


        gui.msg("Currently planting field nr: " + "todo number");
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
        gameui().map.wdgmsg("click", Coord.z, gameui().map.player().rc.floor(posres), 1, 0);
        if (gameui().map.pfthread != null) {
            gameui().map.pfthread.interrupt();
        }
        this.destroy();
    }
}
