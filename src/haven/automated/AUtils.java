package haven.automated;

import haven.*;

import java.util.*;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
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

    public static ArrayList<Gob> getGobFromTypeArray(List<String> gobNames, GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    if (gob.getres() != null && gobNames.contains(gob.getres().name)) {
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

    public static ArrayList<Gob> getGobsInSelectionStartingWith(String name, Coord start, Coord end, GameUI gui) {
        ArrayList<Gob> selected = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.rc.x > start.x && gob.rc.x < end.x && gob.rc.y > start.y && gob.rc.y < end.y) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && res.name.startsWith(name)) {
                            selected.add(gob);
                        }
                    } catch (Loading l) {
                    }
                }
            }
        }
        return selected;
    }


    public static ArrayList<Gob> getTrellisPlantsInSelection(Coord start, Coord end, GameUI gui) {
        ArrayList<Gob> plantsInArea = new ArrayList<>();
        List<String> plants = Arrays.asList("gfx/terobjs/plants/wine", "gfx/terobjs/plants/pepper", "gfx/terobjs/plants/hops", "gfx/terobjs/plants/peas", "gfx/terobjs/plants/cucumber");

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.rc.x > start.x && gob.rc.x < end.x && gob.rc.y > start.y && gob.rc.y < end.y) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && plants.contains(res.name)) {
                            plantsInArea.add(gob);
                        }
                    } catch (Loading l) {}
                }
            }
        }
        return plantsInArea;
    }



    public static Map<Gob, Integer> getGobsInSelectedArea(Coord start, Coord end, GameUI gui) {
        Map<Gob, Integer> gobsInArea = new HashMap<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.rc.x > start.x && gob.rc.x < end.x && gob.rc.y > start.y && gob.rc.y < end.y) {
                    try {
                        Resource res = gob.getres();
                        if (res != null) {
                            String name = res.name;
                            if(name.contains("borka/body")){
                                continue;
                            }
                            Gob existingGob = null;
                            for (Gob keyGob : gobsInArea.keySet()) {
                                if (keyGob.getres().name.equals(name)) {
                                    existingGob = keyGob;
                                    break;
                                }
                            }
                            if (existingGob != null) {
                                gobsInArea.put(existingGob, gobsInArea.get(existingGob) + 1);
                            } else {
                                gobsInArea.put(gob, 1);
                            }
                        }
                    } catch (Loading ignored) {}
                }
            }
        }
        return gobsInArea;
    }

    public static void rightClickGob(GameUI gui, Gob gob, int mods) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, mods, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }

    public static Gob getClosestCropInSelectionStartingWith(String name, Coord start, Coord end, GameUI gui, int stageP) {
        Gob closestGob = null;
        double minDist = Double.MAX_VALUE;
        Coord2d player = gui.map.player().rc;

        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.rc.x > start.x && gob.rc.x < end.x && gob.rc.y > start.y && gob.rc.y < end.y) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && res.name.equals(name)) {
                            int stage = AUtils.getDrawState(gob);
                            double dist = player.dist(gob.rc);
                            if(dist < minDist && (stage >= stageP)) {
                                minDist = dist;
                                closestGob = gob;
                            }
                        }
                    } catch (Loading l) {
                    }
                }
            }
        }
        return closestGob;
    }

    public static boolean isPlayerInSelectedArea(Coord start, Coord end, GameUI gui) {
        try {
            Coord2d player = gui.map.player().rc;
            return player.x > start.x && player.x < end.x && player.y > start.y && player.y < end.y;
        } catch (Exception e){
            return false;
        }
    }

    public static Gob getGobNearPlayer(String name, GameUI gui) {
        Coord playerCoord = gui.map.player().rc.floor();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.rc.x > playerCoord.x - 55 && gob.rc.x < playerCoord.x + 55 && gob.rc.y > playerCoord.y - 55 && gob.rc.y < playerCoord.y + 55) {
                    try {
                        Resource res = gob.getres();
                        if (res != null && res.name.equals(name)) {
                            return gob;
                        }
                    } catch (Loading l) {
                    }
                }
            }
        }
        return null;
    }


    public static int countTiles(String name, Coord start, Coord end, GameUI gui) {
        int count = 0;
        for (int x = start.x+5; x < end.x; x+= 11) {
            for (int y = start.y+5; y < end.y; y+= 11) {
                String tilename = getTileName(new Coord(x,y), gui.map.glob.map);
                if (name.equals(tilename)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int getDrawState(Gob gob) {
        try {
            return gob.getattr(ResDrawable.class).sdt.peekrbuf(0);
        } catch (NullPointerException | Loading e) {
            System.out.println(e.getClass() + " when trying to get drawstate from gob " + gob.id);
        }
        return 0;
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
        if(gui.map.pfthread == null){
            return false;
        }
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

    public static void getGridHeightAvg(GameUI gui){
        try {
            Coord playerCoord = gui.map.player().rc.floor(tilesz);
            MCache.Grid grid = gui.ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
            float wholeGridHeight = 0;
            float[] quarterHeights = new float[4];
            int gridSize = 100;
            int halfGridSize = gridSize / 2;
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    wholeGridHeight += grid.z[i * gridSize + j];
                    int quarterIndex;
                    if(i < halfGridSize) {
                        quarterIndex = (j < halfGridSize) ? 0 : 1;
                    } else {
                        quarterIndex = (j < halfGridSize) ? 2 : 3;
                    }
                    quarterHeights[quarterIndex] += grid.z[i * gridSize + j];
                }
            }
            String[] quarterNames = {"N-W", "N-E", "S-W", "S-E"};
            StringBuilder message = new StringBuilder("Whole grid average height is: " + wholeGridHeight / 10000 + ", ");
            for (int i = 0; i < 4; i++) {
                message.append("\n").append(quarterNames[i]).append(" quarter average height is: ").append(quarterHeights[i] / 2500).append(", ");
            }
            gui.msg(message.toString());
        } catch (Loading ignored) {}
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

    public static void rightClickShiftCtrl(GameUI gui, Gob gob) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 3, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }


    public final static HashSet<String> potentialAggroTargets = new HashSet<String>() {{ // ND: Probably still missing dungeon ants, dungeon bees, dungeon beavers, dungeon bats?
        add("gfx/borka/body");
        add("gfx/kritter/adder/adder");
        add("gfx/kritter/ants/ants");
//        add("gfx/kritter/cattle/cattle"); // ND: Aurochs are handled differently in the method below!
        add("gfx/kritter/badger/badger");
        add("gfx/kritter/bat/bat");
        add("gfx/kritter/bear/bear");
        add("gfx/kritter/beaver/beaver");
        add("gfx/kritter/boar/boar");
        add("gfx/kritter/boreworm/boreworm");
        add("gfx/kritter/caveangler/caveangler");
        add("gfx/kritter/cavelouse/cavelouse");
        add("gfx/kritter/chasmconch/chasmconch"); // ND: I even added this one
        add("gfx/kritter/eagleowl/eagleowl");
        add("gfx/kritter/fox/fox");
        add("gfx/kritter/goat/wildgoat");
        add("gfx/kritter/goldeneagle/goldeneagle");
        add("gfx/kritter/greyseal/greyseal");
        add("gfx/kritter/horse/horse");
        add("gfx/kritter/lynx/lynx");
        add("gfx/kritter/mammoth/mammoth");
        add("gfx/kritter/moose/moose");
//        add("gfx/kritter/sheep/sheep"); // ND: Mouflons are handled differently in the method below!
        add("gfx/kritter/nidbane/nidbane");
        add("gfx/kritter/ooze/greenooze");
        add("gfx/kritter/orca/orca");
        add("gfx/kritter/otter/otter");
        add("gfx/kritter/pelican/pelican");
        add("gfx/kritter/rat/caverat");
        add("gfx/kritter/reddeer/reddeer");
        add("gfx/kritter/reindeer/reindeer");
        add("gfx/kritter/roedeer/roedeer");
        add("gfx/kritter/spermwhale/spermwhale");
        add("gfx/kritter/stoat/stoat");
        add("gfx/kritter/swan/swan");
        add("gfx/kritter/troll/troll");
        add("gfx/kritter/walrus/walrus");
        add("gfx/kritter/wolf/wolf");
        add("gfx/kritter/wolverine/wolverine");
        add("gfx/kritter/woodgrouse/woodgrouse-m");

        add("gfx/kritter/ants/queenant");
        add("gfx/kritter/ants/royalguardant");
        add("gfx/kritter/ants/warriorant");
        add("gfx/kritter/ants/redants");

        add("gfx/kritter/beaver/beaverking");
        add("gfx/kritter/beaver/oldbeaver");
        add("gfx/kritter/beaver/grizzlybeaver");

        add("gfx/kritter/bees/warriordrone");
        add("gfx/kritter/bees/queenbee");
        add("gfx/kritter/bees/sentinelbee");
        add("gfx/kritter/bees/vulturebee");
        add("gfx/kritter/bees/honeybee");
        add("gfx/kritter/bees/beelarva");
        add("gfx/kritter/wildbees/beeswarm");

        add("gfx/kritter/bat/nightqueen");
        add("gfx/kritter/bat/vampire");
        add("gfx/kritter/bat/bloodstalker");
        add("gfx/kritter/bat/denmother");
        add("gfx/kritter/bat/fatbat");



    }
    };
    public static HashMap<Long, Gob> getAllAttackableMap(GameUI gui) {
        HashMap<Long, Gob> gobs = new HashMap<>();
        if (gui.map.plgob == -1) {
            return gobs;
        }
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.getres() != null && gob.getres().name != null){
                    if (gob.id != gui.map.plgob) {
                        if (potentialAggroTargets.contains(gob.getres().name)){
                            gobs.put(gob.id, gob);
                        } else if (gob.getres().name.equals("gfx/kritter/cattle/cattle")) { // ND: Special case for Aurochs
                            for (GAttrib g : gob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("aurochs")){
                                                    gobs.put(gob.id, gob);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (gob.getres().name.equals("gfx/kritter/sheep/sheep")) { // ND: Special case for Mouflon
                            for (GAttrib g : gob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("mouflon")){
                                                    gobs.put(gob.id, gob);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        return gobs;
    }

    public static HashMap<Long, Gob> getAllAttackablePlayersMap(GameUI gui) {
        HashMap<Long, Gob> gobs = new HashMap<>();
        if (gui.map.plgob == -1) {
            return gobs;
        }
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.getres() != null && gob.getres().name != null){
                    if (gob.id != gui.map.plgob) {
                        if (gob.getres().name.equals("gfx/borka/body")){
                            gobs.put(gob.id, gob);
                        }
                    }
                }
            }
        }
        return gobs;
    }

    public static List<WItem> getAllItemsFromAllInventoriesAndStacksExcludeBeltAndKeyring(GameUI gui){
        List<WItem> items = new ArrayList<>();
        List<Inventory> allInventories = gui.getAllInventories();

        for (Inventory inventory : allInventories) {
            if (!isBeltOrKeyring(inventory)) {
                for (WItem item : inventory.getAllItems()) {
                    if (!item.item.getname().contains("stack of")) {
                        items.add(item);
                    }
                }
            }
        }

        items.addAll(gui.getAllContentsWindows());
        return items;
    }

    public static boolean isBeltOrKeyring(Inventory inventory) {
        if (inventory.parent instanceof Window) {
            String cap = ((Window) inventory.parent).cap;
            return cap.contains("Belt") || cap.contains("Keyring");
        }
        return false;
    }

    public static boolean waitForEmptyHand(final GameUI gui, final int timeout, final String error) throws InterruptedException {
        int t = 0;
        while (gui.vhand != null) {
            t += 5;
            if (t >= timeout) {
                gui.error(error);
                return false;
            }
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException ie) {
                throw ie;
            }
        }
        return true;
    }

    public static boolean waitForOccupiedHand(final GameUI gui, final int timeout, final String error) throws InterruptedException {
        int t = 0;
        while (gui.vhand == null) {
            t += 5;
            if (t >= timeout) {
                gui.error(error);
                return false;
            }
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException ie) {
                throw ie;
            }
        }
        return true;
    }

    public static WItem findItemByPrefixInInv(final Inventory inv, final String resNamePrefix) {
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                final WItem witm = (WItem)wdg;
                try {
                    if (witm.item.getres().name.startsWith(resNamePrefix)) {
                        return witm;
                    }
                }
                catch (Loading ignored) {}
            }
        }
        return null;
    }

    public static WItem findItemInInv(final Inventory inv, final String resName) {
        for (Widget wdg = inv.child; wdg != null; wdg = wdg.next) {
            if (wdg instanceof WItem) {
                final WItem witm = (WItem)wdg;
                try {
                    if (witm.item.getres().name.equals(resName)) {
                        return witm;
                    }
                }
                catch (Loading ignored) {}
            }
        }
        return null;
    }


}
