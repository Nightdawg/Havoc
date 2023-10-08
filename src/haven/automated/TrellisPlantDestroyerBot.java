package haven.automated;

import haven.*;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static haven.OCache.posres;

public class TrellisPlantDestroyerBot extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop;
    private boolean active = false;
    private List<Gob> plantsToDestroy;
    private Coord nw = null;
    private Coord se = null;
    Button startbutton = null;

    public TrellisPlantDestroyerBot(GameUI gui) {
        super(new Coord(160, 50), "Trellis Plant Destroyer Bot", true);
        this.gui = gui;
        this.plantsToDestroy = new ArrayList<>();

        Widget prev;
        prev = add(new Button(100, "Select Area") {
            @Override
            public void click() {
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select are with trellises to destroy.");
                gui.map.areaSelect = true;
            }
        }, UI.scale(0, 10));

        startbutton = add(new Button(100, "Start") {
            @Override
            public void click() {

                active = !active;
                if (active) {
                    gui.msg("Starting.");
                    this.change("Stop");
                } else {
                    gui.msg("Pausing.");
                    ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
                    this.change("Start");
                }
            }
        }, prev.pos("ur").adds(10, 0));
        pack();
    }

    @Override
    public void run() {
        while (!stop) {
            if (active) {
                Gob closestPlant = null;
                if (plantsToDestroy != null && plantsToDestroy.size() > 0) {
                    if (gui.map.player().getv() == 0 && gui.prog == null) {
                        closestPlant = findClosestPlantToPlayer();
                        if (closestPlant == null) {
                            active = false;
                            startbutton.change("Start");
                            continue;
                        }
                        Coord2d startingPosition = getStartingPosition(closestPlant);
                        if (gui.map.player().rc.dist(startingPosition) < 4) {
                            gui.act("destroy");
                            gui.map.wdgmsg("click", Coord.z, closestPlant.rc.floor(posres), 1, 0, 0, (int) closestPlant.id, closestPlant.rc.floor(posres), 0, -1);
                            gui.map.wdgmsg("click", Coord.z, Coord.z, 3, 0);
                        } else {
                            gui.map.pfLeftClick(startingPosition.floor(), null);
                            sleep(500);
                        }
                    } else {
                        sleep(500);
                    }
                } else {
                    gui.msg("No plants left.");
                    startbutton.change("Start");
                    active = false;
                    nw = null;
                    se = null;
                    plantsToDestroy = null;
                    closestPlant = null;
                }
            }
            sleep(500);
        }
    }

    public Coord2d getStartingPosition(Gob closest) {
        Coord2d adjustedCoord = new Coord2d(closest.rc.x, closest.rc.y);
        Gob player = gui.map.player();
        double angle = (closest.a + 2 * Math.PI) % (2 * Math.PI);

        if (angle > Math.PI / 4 && angle <= 3 * Math.PI / 4 || angle > 5 * Math.PI / 4 && angle <= 7 * Math.PI / 4) {
            if (player.rc.y > closest.rc.y) {
                adjustedCoord.y += 11;
            } else {
                adjustedCoord.y -= 11;
            }
        } else {
            if (player.rc.x > closest.rc.x) {
                adjustedCoord.x += 11;
            } else {
                adjustedCoord.x -= 11;
            }
        }

        return adjustedCoord;
    }

    public Gob findClosestPlantToPlayer() {
        Gob player = gui.map.player();
        Gob closest = null;
        double minDist = Double.MAX_VALUE;
        for (Gob gob : AUtils.getTrellisPlantsInSelection(nw, se, gui)) {
            double distanceToPlayer = player.rc.dist(gob.rc);
            if (distanceToPlayer < minDist) {
                minDist = distanceToPlayer;
                closest = gob;
            }
        }
        return closest;
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void areaselect(Coord a, Coord b) {
        nw = a.mul(MCache.tilesz2);
        se = b.mul(MCache.tilesz2);
        plantsToDestroy = AUtils.getTrellisPlantsInSelection(nw, se, gui);
        gui.msg("Found " + plantsToDestroy.size() + " trellis plants.");
        gui.map.unregisterAreaSelect();
        active = true;
        startbutton.change("Stop");
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
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        if (gui.trellisPlantDestroyerBotThread != null) {
            gui.trellisPlantDestroyerBotThread.interrupt();
            gui.trellisPlantDestroyerBotThread = null;
        }
        this.destroy();
    }
}