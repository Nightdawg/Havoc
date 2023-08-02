package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class MiningSafetyAssistant extends Window implements Runnable {
    private final GameUI gui;
    private final boolean stop;
    public static boolean preventMiningOutsideSupport = Utils.getprefb("preventMiningOutsideSupport", false);
    private final CheckBox preventUnsafeMiningCb;
    private boolean stopMiningWhenOutsideSupport = Utils.getprefb("stopMiningWhenOutsideSupport", false);
    private final CheckBox stopUnsafeMiningCb;
    private boolean stopMiningFifty = Utils.getprefb("stopMiningFifty", false);
    private final CheckBox stopMiningFiftyCb;
    private boolean stopMiningTwentyFive = Utils.getprefb("stopMiningTwentyFive", false);
    private final CheckBox stopMiningTwentyFiveCb;
    private boolean stopMiningLooseRock = Utils.getprefb("stopMiningLooseRock", false);
    private final CheckBox stopMiningLooseRockCb;


    ArrayList<Gob> supports = new ArrayList<>();
    ArrayList<Gob> looseRocks = new ArrayList<>();
    private int counter = 0;

    public MiningSafetyAssistant(GameUI gui) {
        super(new Coord(220, 180), "Mining Safety Assistant");
        this.gui = gui;
        this.stop = false;
        Widget prev;

        preventUnsafeMiningCb = new CheckBox("Prevent unsafe mining.") {
            {
                a = preventMiningOutsideSupport;
            }

            public void set(boolean val) {
                preventMiningOutsideSupport = val;
                Utils.setprefb("preventMiningOutsideSupport", val);
                a = val;
            }
        };
        prev = add(preventUnsafeMiningCb, new Coord(10, 20));
        preventUnsafeMiningCb.tooltip = RichText.render("This option will prevent selecting mining area even \npartially outside (visible) mining supports range. \n(Cannot select area outside view range)", UI.scale(300));

        stopUnsafeMiningCb = new CheckBox("Stop unsafe mining.") {
            {
                a = stopMiningWhenOutsideSupport;
            }

            public void set(boolean val) {
                stopMiningWhenOutsideSupport = val;
                Utils.setprefb("stopMiningWhenOutsideSupport", val);
                a = val;
            }
        };
        prev = add(stopUnsafeMiningCb, prev.pos("bl").adds(0, 20));
        stopUnsafeMiningCb.tooltip = RichText.render("If currently mined tile is outside support range \nmining will stop. (Drinking animation overrides mining \nand delay bot reaction - not 100% safe)", UI.scale(300));

        stopMiningFiftyCb = new CheckBox("Stop mining <50.") {
            {
                a = stopMiningFifty;
            }

            public void set(boolean val) {
                stopMiningFifty = val;
                Utils.setprefb("stopMiningFifty", val);
                a = val;
            }
        };
        prev = add(stopMiningFiftyCb, prev.pos("bl").adds(0, 20));
        stopMiningFiftyCb.tooltip = RichText.render("If currently mined tile is withing support range \nbelow 50% hp mining will stop.", UI.scale(300));


        stopMiningTwentyFiveCb = new CheckBox("Stop mining <25.") {
            {
                a = stopMiningTwentyFive;
            }

            public void set(boolean val) {
                stopMiningTwentyFive = val;
                Utils.setprefb("stopMiningTwentyFive", val);
                a = val;
            }
        };
        prev = add(stopMiningTwentyFiveCb, prev.pos("bl").adds(0, 20));
        stopMiningTwentyFiveCb.tooltip = RichText.render("If currently mined tile is withing support range \nbelow 25% hp mining will stop.", UI.scale(300));

        stopMiningLooseRockCb = new CheckBox("Stop mining near loose rock.") {
            {
                a = stopMiningLooseRock;
            }

            public void set(boolean val) {
                stopMiningLooseRock = val;
                Utils.setprefb("stopMiningLooseRock", val);
                a = val;
            }
        };
        prev = add(stopMiningLooseRockCb, prev.pos("bl").adds(0, 20));
        stopMiningLooseRockCb.tooltip = RichText.render("If currently mined tile is withing ~9 tiles from any \nloose rock mining will stop.", UI.scale(300));


        add(new Label("Movement"), UI.scale(154, 15));
        add(new Button(20, "↖") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(-11, -11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(150, 27));
        add(new Button(20, "↑") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(0, -11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(170, 27));
        add(new Button(20, "↗") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(11, -11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(190, 27));

        add(new Button(20, "←") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(-11, 0).floor(posres), 1, 0);
                }
            }
        }, UI.scale(150, 50));
        add(new Button(20, "○") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    Coord2d center = gameui().map.player().rc.div(11).floord().mul(11).add(5.5, 5.5);
                    gui.map.wdgmsg("click", Coord.z, center.floor(posres), 1, 0);
                }
            }
        }, UI.scale(170, 50));
        add(new Button(20, "→") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(11, 0).floor(posres), 1, 0);
                }
            }
        }, UI.scale(190, 50));


        add(new Button(20, "↙") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(-11, 11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(150, 73));
        add(new Button(20, "↓") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(0, 11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(170, 73));
        add(new Button(20, "↘") {
            @Override
            public void click() {
                if (gui.map.player().getv() == 0) {
                    gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(11, 11).floor(posres), 1, 0);
                }
            }
        }, UI.scale(190, 73));


    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (msg == "close")) {
            gui.miningSafetyAssistantThread.interrupt();
            gui.miningSafetyAssistantThread = null;
            reqdestroy();
            gui.miningSafetyAssistantWindow = null;
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

            if (stopMiningLooseRock && (gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("gfx/borka/choppan"))) {
                Gob player = gui.map.player();
                Coord2d minedTile = new Coord2d(player.rc.x + (Math.cos(player.a) * 13.75), player.rc.y + Math.sin(player.a) * 13.75);
                if (counter == 0) {
                    looseRocks = AUtils.getGobs("gfx/terobjs/looserock", gui);
                }
                for (Gob looseRock : looseRocks) {
                    if (looseRock.rc.dist(minedTile) <= 125) {
                        looseRock.highlight(Color.red);
                        ui.root.wdgmsg("gk", 27);
                        gui.error("Loose rock is too close to mine safely.");
                    }
                }
            }

            if ((stopMiningFifty || stopMiningTwentyFive) && (gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("gfx/borka/choppan"))) {
                Gob player = gui.map.player();
                Coord2d minedTile = new Coord2d(player.rc.x + (Math.cos(player.a) * 13.75), player.rc.y + Math.sin(player.a) * 13.75);
                for (Gob support : supports) {
                    String res = support.getres().name;
                    if (res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) {
                        if (support.rc.dist(minedTile) <= 100) {
                            if (support.getattr(GobHealth.class) != null) {
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if (support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty) {
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive) {
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 25%..");
                                    support.highlight(Color.red);
                                }
                            }
                        }
                    } else if (res.equals("gfx/terobjs/column")) {
                        if (support.rc.dist(minedTile) <= 125) {
                            if (support.getattr(GobHealth.class) != null) {
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if (support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty) {
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive) {
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 25%..");
                                    support.highlight(Color.red);
                                }
                            }
                        }
                    } else if (res.equals("gfx/terobjs/minebeam")) {
                        if (support.rc.dist(minedTile) <= 150) {
                            if (support.getattr(GobHealth.class) != null) {
                                System.out.println(support.getattr(GobHealth.class).hp);
                                if (support.getattr(GobHealth.class).hp <= 0.5 && stopMiningFifty) {
                                    ui.root.wdgmsg("gk", 27);
                                    gui.error("Support nearby below 50%..");
                                    support.highlight(Color.red);
                                } else if (support.getattr(GobHealth.class).hp <= 0.25 && stopMiningTwentyFive) {
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
        int tilesCountX = Math.abs(one.x - two.x) + 1;
        int tilesCountY = Math.abs(one.y - two.y) + 1;
        for (int x = 0; x < tilesCountX; x++) {
            for (int y = 0; y < tilesCountY; y++) {
                tiles.add(new Coord2d(northWestCoord.x + (x * 11) + 5.5, northWestCoord.y + (y * 11) + 5.5));
            }
        }
        ArrayList<Gob> supportsStatic = AUtils.getAllSupports(gui);
        for (Coord2d tile : tiles) {
            int inRange = 0;
            for (Gob support : supportsStatic) {
                String res = support.getres().name;
                if (res.equals("gfx/terobjs/ladder") || res.equals("gfx/terobjs/minesupport")) {
                    if (support.rc.dist(tile) <= 100) {
                        inRange++;
                    }
                } else if (res.equals("gfx/terobjs/column")) {
                    if (support.rc.dist(tile) <= 125) {
                        inRange++;
                    }
                } else if (res.equals("gfx/terobjs/minebeam")) {
                    if (support.rc.dist(tile) <= 150) {
                        inRange++;
                    }
                }
            }
            if (inRange == 0) {
                return false;
            }
        }
        return true;
    }
}
