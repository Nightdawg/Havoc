package haven.automated;

import haven.*;
import haven.Window;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MiningAssistant extends Window implements Runnable {
    private final GameUI gui;
    private final boolean stop;
    public static boolean preventMiningOutsideSupport = false;
    private final CheckBox preventUnsafeMiningCb;
    private boolean stopMiningWhenOutsideSupport;
    private final CheckBox stopUnsafeMiningCb;
    private boolean stopMiningFifty;
    private final CheckBox stopMiningFiftyCb;
    private boolean stopMiningTwentyFive;
    private final CheckBox stopMiningTwentyFiveCb;
    private boolean stopMiningLooseRock;
    private final CheckBox stopMiningLooseRockCb;




    ArrayList<Gob> supports = new ArrayList<>();
    ArrayList<Gob> looseRocks = new ArrayList<>();
    private int counter = 0;

    public MiningAssistant(GameUI gui) {
        super(new Coord(180, 190), "Mining Assistant");
        this.gui = gui;
        this.stop = false;
        Widget prev;

        preventUnsafeMiningCb = new CheckBox("Prevent unsafe mining.") {
            {
                a = preventMiningOutsideSupport;
            }

            public void set(boolean val) {
                preventMiningOutsideSupport = val;
                a = val;
            }
        };
        prev = add(preventUnsafeMiningCb, new Coord(10, 20));
        preventUnsafeMiningCb.tooltip = RichText.render("This option will prevent selecting mining area even \npartially outside (visible) mining supports range. \n(Cannot select area outside view range)", 300);

        stopUnsafeMiningCb = new CheckBox("Stop unsafe mining.") {
            {
                a = stopMiningWhenOutsideSupport;
            }

            public void set(boolean val) {
                stopMiningWhenOutsideSupport = val;
                a = val;
            }
        };
        prev = add(stopUnsafeMiningCb, prev.pos("bl").adds(0,20));
        stopUnsafeMiningCb.tooltip = RichText.render("If currently mined tile is outside support range \nmining will stop. (Drinking animation overrides mining \nand delay bot reaction - not 100% safe)", 300);

        stopMiningFiftyCb = new CheckBox("Stop mining <50.") {
            {
                a = stopMiningFifty;
            }

            public void set(boolean val) {
                stopMiningFifty = val;
                a = val;
            }
        };
        prev = add(stopMiningFiftyCb, prev.pos("bl").adds(0,20));
        stopMiningFiftyCb.tooltip = RichText.render("If currently mined tile is withing support range \nbelow 50% hp mining will stop.", 300);


        stopMiningTwentyFiveCb = new CheckBox("Stop mining <25.") {
            {
                a = stopMiningTwentyFive;
            }

            public void set(boolean val) {
                stopMiningTwentyFive = val;
                a = val;
            }
        };
        prev = add(stopMiningTwentyFiveCb, prev.pos("bl").adds(0,20));
        stopMiningTwentyFiveCb.tooltip = RichText.render("If currently mined tile is withing support range \nbelow 25% hp mining will stop.", 300);

        stopMiningLooseRockCb = new CheckBox("Stop mining near loose rock.") {
            {
                a = stopMiningLooseRock;
            }

            public void set(boolean val) {
                stopMiningLooseRock = val;
                a = val;
            }
        };
        prev = add(stopMiningLooseRockCb, prev.pos("bl").adds(0,20));
        stopMiningLooseRockCb.tooltip = RichText.render("If currently mined tile is withing ~9 tiles from any \nloose rock mining will stop.", 300);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (msg == "close")) {
            gui.miningAssistantThread.interrupt();
            gui.miningAssistantThread = null;
            reqdestroy();
            gui.miningAssistantWindow = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public void run() {
        while (!stop) {
            if (counter == 0 && (stopMiningWhenOutsideSupport || stopMiningFifty || stopMiningTwentyFive)) {
                supports = AUtils.getAllSupports(gui);
            }
            if (stopMiningWhenOutsideSupport && (gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("gfx/borka/choppan"))) {
                Gob player = gui.map.player();
                Coord2d minedTile = new Coord2d(player.rc.x + (Math.cos(player.a) * 13.75), player.rc.y + Math.sin(player.a) * 13.75);
                Set<Gob> gobsInRange = new HashSet<>();
                for (Gob support : supports) {
                    String res = support.getres().name;
                    if (res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) {
                        if (support.rc.dist(minedTile) <= 100) {
                            gobsInRange.add(support);
                        }
                    } else if (res.equals("gfx/terobjs/column")) {
                        if (support.rc.dist(minedTile) <= 125) {
                            gobsInRange.add(support);
                        }
                    } else if (res.equals("gfx/terobjs/minebeam")) {
                        if (support.rc.dist(minedTile) <= 150) {
                            gobsInRange.add(support);
                        }
                    }
                }
                if (gobsInRange.size() < 1) {
                    ui.root.wdgmsg("gk", 27);
                    gui.error("Trying to mine outside supports.");
                }
            }

            if(stopMiningLooseRock && (gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("gfx/borka/choppan"))){
                Gob player = gui.map.player();
                Coord2d minedTile = new Coord2d(player.rc.x + (Math.cos(player.a) * 13.75), player.rc.y + Math.sin(player.a) * 13.75);
                if (counter == 0) {
                    looseRocks = AUtils.getGobs("gfx/terobjs/looserock", gui);
                }
                for(Gob looseRock : looseRocks){
                    if(looseRock.rc.dist(minedTile) <= 125){
                        looseRock.highlight(Color.red);
                        ui.root.wdgmsg("gk", 27);
                        gui.error("Loose rock is too close to mine safely.");
                    }
                }
            }

            if((stopMiningFifty || stopMiningTwentyFive) && (gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("gfx/borka/choppan"))){
                Gob player = gui.map.player();
                Coord2d minedTile = new Coord2d(player.rc.x + (Math.cos(player.a) * 13.75), player.rc.y + Math.sin(player.a) * 13.75);
                for(Gob support : supports){
                    String res = support.getres().name;
                    if (res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) {
                        if (support.rc.dist(minedTile) <= 100) {
                            if(support.getattr(GobHealth.class) != null){
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if(support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 25%..");
                                    support.highlight(Color.red);
                                }
                            }
                        }
                    } else if (res.equals("gfx/terobjs/column")) {
                        if (support.rc.dist(minedTile) <= 125) {
                            if(support.getattr(GobHealth.class) != null){
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if(support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 25%..");
                                    support.highlight(Color.red);
                                }
                            }
                        }
                    } else if (res.equals("gfx/terobjs/minebeam")) {
                        if (support.rc.dist(minedTile) <= 150) {
                            if(support.getattr(GobHealth.class) != null){
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if(support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive){
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 25%..");
                                    support.highlight(Color.red);
                                }
                            }
                        }
                    }

                }
            }

            counter++;
            if (counter > 10) {
                counter = 0;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static boolean isAreaInSupportRange(Coord one, Coord two, GameUI gui) {
        Coord northWestCoord = new Coord(Math.min(one.x, two.x) * 11, Math.min(one.y, two.y) * 11);
        Set<Coord2d> tiles = new HashSet<>();
        int tilesCountX = Math.abs(one.x - two.x)+1;
        int tilesCountY = Math.abs(one.y - two.y)+1;
        for (int x = 0; x < tilesCountX; x++) {
            for (int y = 0; y < tilesCountY; y++) {
                System.out.println(new Coord2d(northWestCoord.x + (x * 11) + 5.5 , northWestCoord.y + (y * 11) + 5.5));
                tiles.add(new Coord2d(northWestCoord.x + (x * 11) + 5.5 , northWestCoord.y + (y * 11) + 5.5));
            }
        }
        ArrayList<Gob> supportsStatic = AUtils.getAllSupports(gui);
        for(Coord2d tile: tiles){
            int inRange = 0;
            for(Gob support : supportsStatic){
                String res = support.getres().name;
                if (res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) {
                    if (support.rc.dist(tile) <= 100) {
                        inRange++;
                    }
                } else if (res.equals("gfx/terobjs/column")) {
                    if (support.rc.dist(tile) <= 125) {
                        inRange++;
                        System.out.println(tile + " sup: " + support.rc);
                    }
                } else if (res.equals("gfx/terobjs/minebeam")) {
                    if (support.rc.dist(tile) <= 150) {
                        inRange++;
                    }
                }
            }
            if(inRange == 0){
                return false;
            }
        }
        return true;
    }
}
