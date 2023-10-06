package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static haven.OCache.posres;

public class Transition implements Runnable {
    private GameUI gui;

    public Transition(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        Gob player = gui.map.player();
        if (player == null) return;
        Coord2d plc = player.rc;
        Target targetDoor = getTarget(gui, buildings, 40 * 11);
        Gob targetGob = getGob(gui, gobNameSuffix, 40 * 11);
        if ((targetDoor == null) && (targetGob == null))
            return;
        if (targetGob != null)
            if ((targetDoor == null) || (targetGob.rc.dist(plc) < targetDoor.c.dist(plc)))
                targetDoor = new Target(targetGob.rc,   Coord.z, targetGob.id, -1);

        try {
            gui.map.wdgmsg("click", targetDoor.s, targetDoor.c.floor(posres), 3, 0, 0, (int) targetDoor.g, targetDoor.c.floor(posres), 0, targetDoor.m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Target getTarget(GameUI gui, ArrayList<DoorShiftData> b, double r) {
        Coord2d plc = gui.map.player().rc;
        Target result = null;
        ArrayList<Target> targetList = new ArrayList<>();
        if (r == 0) r = 1024.0;
        for (Gob gob : AUtils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();

                if (res == null)
                    continue;
                if (!res.name.startsWith("gfx/terobjs/arch/"))
                    continue;

                for (DoorShiftData bld : b) {
                    if (bld.gobName.equals(res.name)) {
                        for (Door drs : bld.doorList) {
                            targetList.add(new Target(
                                    gob.rc.add(drs.meshRC.rotate(gob.a)),
                                    Coord.z,
                                    gob.id,
                                    drs.meshID
                            ));
                        }
                    }
                }
            } catch (Loading l) {
                l.printStackTrace();
            }
        }
        for (Target t : targetList) {
            try {
                if ((result == null) || (t.c.dist(plc) < result.c.dist(plc)))
                    result = t;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private Gob getGob(GameUI gui, ArrayList<String> gobNameAL, double maxrange) {
        Coord2d plc = gui.map.player().rc;
        Gob result = null;
        if (maxrange == 0) maxrange = 1024.0;
        for (Gob gob : AUtils.getAllGobs(gui)) {
            try {
                if (gob.getres() == null)
                    continue;
                boolean skipGob = true;
                for (String n : gobNameAL)
                    if ((gob.getres().name.endsWith(n)) && (!gob.getres().name.endsWith("gfx/terobjs/arch/greathall-door")))
                        skipGob = false;
                if (skipGob) continue;
                if ((result == null || gob.rc.dist(plc) < result.rc.dist(plc)) && gob.rc.dist(plc) < maxrange)
                    result = gob;
            } catch (Loading l) {
                l.printStackTrace();
            }
        }
        return result;
    }

    private ArrayList<String> gobNameSuffix = new ArrayList<String>(Arrays.asList(
            "-door",
            "ladder",
            "upstairs",
            "downstairs",
            "cellarstairs",
            "cavein",
            "caveout"
    ));

    public ArrayList<DoorShiftData> buildings = new ArrayList<>(Arrays.asList(
            new DoorShiftData("gfx/terobjs/arch/logcabin", new ArrayList<>(List.of(
                    new Door(new Coord2d(22, 0), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/timberhouse", new ArrayList<>(List.of(
                    new Door(new Coord2d(33, 0), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/stonestead", new ArrayList<>(List.of(
                    new Door(new Coord2d(44, 0), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/stonemansion", new ArrayList<>(List.of(
                    new Door(new Coord2d(48, 0), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/greathall", new ArrayList<>(Arrays.asList(
                    new Door(new Coord2d(77, -28), 18),
                    new Door(new Coord2d(77, 0), 17),
                    new Door(new Coord2d(77, 28), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/stonetower", new ArrayList<>(List.of(
                    new Door(new Coord2d(36, 0), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/windmill", new ArrayList<>(List.of(
                    new Door(new Coord2d(0, 28), 16)
            ))),
            new DoorShiftData("gfx/terobjs/arch/greathall-door", new ArrayList<>(Arrays.asList(
                    new Door(new Coord2d(0, -30), 18),
                    new Door(new Coord2d(0, 0), 17),
                    new Door(new Coord2d(0, 30), 16)
            )))
    ));

    public static class Door {
        public Coord2d meshRC;
        public int meshID;

        public Door(Coord2d c, int id) {
            meshRC = c;
            meshID = id;
        }
    }

    public class DoorShiftData {
        public String gobName;
        public ArrayList<Door> doorList;

        public DoorShiftData(String gobName, ArrayList<Door> doorList) {
            this.gobName = gobName;
            this.doorList = doorList;
        }
    }

    public class Target {
        public Coord2d c;
        public Coord s;
        public long g;
        public int m;

        public Target(Coord2d ic, Coord is, long ig, int im) {
            c = ic;
            s = is;
            g = ig;
            m = im;
        }
    }
}
