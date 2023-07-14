package haven.automated;

import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class AUtils {

    public static void attackGob(GameUI gui, Gob gob) {
        if (gob != null && gui != null && gui.map != null) {
            gui.act("aggro");
            gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
            rightClick(gui);
        }
    }

    public static void rightClick(GameUI gui) {
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
    }

    public static void clickWItemAndSelectOption(GameUI gui, WItem wItem, int index) {
        wItem.item.wdgmsg("iact", Coord.z, gui.ui.modflags());
        gui.ui.rcvr.rcvmsg(gui.ui.lastid+1, "cl", index, gui.ui.modflags());
    }

    public static void clearhand(GameUI gui) {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            } else {
                gui.error("could not get item on hand (minimized window), but dropped it anyway, hope it wasnt something important");
            }
        }
        rightClick(gui);
    }

    public static void drinkTillFull(GameUI gui, double threshold, double stoplevel) throws InterruptedException {
        while (gui.drink(threshold)) {
            Thread.sleep(490);
            do {
                Thread.sleep(10);
                IMeter.Meter stam = gui.getmeter("stam", 0);
                if (stam.a >= stoplevel)
                    break;
            } while (gui.prog != null && gui.prog.prog >= 0);
        }
    }

    public static ArrayList<Gob> getGobType(String gobName, GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.getres() != null && gob.getres().name.startsWith(gobName)) {
                        gobs.add(gob);
                    }
                } catch (Loading ignored) {
                }
            }
        }
        return gobs;
    }

    public static String getTileName(Coord coord, MCache mcache) {
        try {
            Coord c = new Coord(coord.x / 11, coord.y / 11).add(-1, -1);
            int t = mcache.gettile(c);
            Resource res = mcache.tilesetr(t);
            if (res == null)
                return "";

            return res.basename();
        } catch (Loading l) {
            System.out.println("could not get tile");
            return "";
        }
    }

    public static void clickUiButton(String name, GameUI gui) {
        for (MenuGrid.Pagina pag : gui.menu.paginae) {
            if (pag.res().name.equals(name)) {
                gui.act(pag.act().ad);
            }
        }
    }

    public static void activateSign(String name, GameUI gui) {
        Window w = gui.getwnd(name);
        if (w != null) {
            for (Widget wi = w.lchild; wi != null; wi = wi.prev) {
                if (wi instanceof Button) {
                    ((Button) wi).click();
                }
            }
        }
    }

    public static void leftClick(GameUI gui, Coord c) {
        gui.map.wdgmsg("click", Coord.z, new Coord2d(c.x, c.y).floor(posres), 1, 0);
    }

    public static ArrayList<Gob> getAllGobs(GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null) {
                        gobs.add(gob);
                    }
                } catch (Loading l) {
                }
            }
        }
        return gobs;
    }

    public static ArrayList<Gob> getAllSupports(GameUI gui) {
        ArrayList<Gob> supports = new ArrayList<>();
        Set<String> types = new HashSet<>(Arrays.asList("gfx/terobjs/ladder", "gfx/terobjs/minesupport", "gfx/terobjs/column", "gfx/terobjs/minebeam"));
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && types.contains(res.name)) {
                        supports.add(gob);
                    }
                } catch (Loading ignored) {}
            }
        }
        return supports;
    }



    public static boolean waitPf(GameUI gui) throws InterruptedException {
        int time = 0;
        boolean moved = false;
        Thread.sleep(300);
        while (gui.map.pfthread.isAlive()) {
            time += 70;
            Thread.sleep(70);
            if (gui.map.player().getv() > 0) {
                time = 0;
            }
            if (time > 2000 && moved == false) {
                System.out.println("TRYING UNSTUCK");
                return false;
            } else if (time > 20000) {
                return false;
            }
        }
        return true;
    }

    public static void waitProgBar(GameUI gui) throws InterruptedException {
        while (gui.prog != null && gui.prog.prog >= 0) {
            Thread.sleep(40);
        }
    }

    public static void unstuck(GameUI gui) throws InterruptedException {
        Coord2d pc = gui.map.player().rc;
        Random r = new Random();
        for (int i = 0; i < 5; i++) {
            int xAdd = r.nextInt(500) - 250;
            int yAdd = r.nextInt(500) - 250;
            gui.map.wdgmsg("click", Coord.z, pc.floor(posres).add(xAdd, yAdd), 1, 0);
            Thread.sleep(100);
        }
    }

    public static ArrayList<Gob> getGobs(String name, GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && res.name.equals(name)) {
                        gobs.add(gob);
                    }
                } catch (Loading l) {
                }
            }
        }
        return gobs;
    }

    public static Gob getClosestSupport(GameUI gui) {
        Set<String> supports = new HashSet<>(Arrays.asList("gfx/terobjs/ladder", "gfx/terobjs/minesupport", "gfx/terobjs/column", "gfx/terobjs/minebeam"));
        Coord2d player = gui.map.player().rc;
        Gob closestGob = null;
        double closestDistance  = 10000;
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && supports.contains(res.name)) {
                        double currentDistance = gob.rc.dist(player);
                        if(currentDistance < closestDistance){
                            closestGob = gob;
                            closestDistance = currentDistance;
                        }
                    }
                } catch (Loading ignored) {}
            }
        }
        return closestGob;
    }

    public static ArrayList<Gob> getGobsPartial(String name, GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    if (res != null && res.name.contains(name)) {
                        gobs.add(gob);
                    }
                } catch (Loading l) {
                }
            }
        }
        return gobs;
    }

    public static Gob closestGob(List<Gob> gobs, Coord c) {
        if (gobs.isEmpty())
            return null;
        Gob closestGob = gobs.get(0);
        for (Gob gob : gobs) {
            if (gob.rc.floor().dist(c) < closestGob.rc.floor().dist(c))
                closestGob = gob;
        }
        return closestGob;
    }

    public static void rightClickGobAndSelectOption(GameUI gui, Gob gob, int index) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
        gui.ui.rcvr.rcvmsg(gui.ui.lastid+1, "cl", index, gui.ui.modflags());
    }

}
