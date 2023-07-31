package haven.automated;

import haven.*;
import haven.automated.helpers.FarmingStatic;

import java.text.DecimalFormat;
import java.util.Objects;

import static haven.OCache.posres;

public class CombatDistanceTool extends Window implements Runnable {
    private final GameUI gui;
    public boolean stop;

    private Label currentDistanceLabel;

    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public CombatDistanceTool(GameUI gui) {
        super(new Coord(180, 60), "Combat Distancing");
        this.gui = gui;
        this.stop = false;
        this.value = "";

        currentDistanceLabel = new Label("Current dist: No target");
        add(currentDistanceLabel, new Coord(15, 40));

        add(new TextEntry(100, value) {
            @Override
            protected void changed() {
                setValue(this.buf.line());
            }
        }, UI.scale(15, 10));

        add(new Button(UI.scale(40), "Go") {
            @Override
            public void click() {
                moveToDistance();
            }
        }, UI.scale(122, 8));

    }

    @Override
    public void run() {
        while(!stop){
                if(gui.fv.current != null){
                    double dist = getDistance(gui.fv.current.gobid);
                    if(dist < 0){
                        currentDistanceLabel.settext("No target");
                    } else {
                        DecimalFormat df = new DecimalFormat("#.##");
                        String result = df.format(dist);
                        currentDistanceLabel.settext("Current dist: " + result + " units.");
                    }
                } else {
                    currentDistanceLabel.settext("Current dist: No target");
                }

            sleep(500);
        }
    }

    private void moveToDistance(){
        try {
            double distance = Double.parseDouble(value);
            Gob enemy = getEnemy();
            if(enemy != null){
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.");
            }
        } catch(NumberFormatException e) {
            gui.error("Wrong distance format. Use ##.###");
        }
    }

    private Coord2d getNewCoord(Gob enemy, double distance, double angle){
        return new Coord2d(enemy.rc.x + distance * Math.cos(angle), enemy.rc.y + distance * Math.sin(angle));
    }

    private Gob getEnemy(){
        if(gui.fv.current != null){
            long id = gui.fv.current.gobid;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    if (gob.id == id) {
                        return gob;
                    }
                }
            }
        }
        return null;
    }

    private double getDistance(long gobId) {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.id == gobId) {
                    return gob.rc.dist(gui.map.player().rc);
                }
            }
        }
        return -1;
    }

    private void sleep(int duration) {
        try {Thread.sleep(duration);} catch (InterruptedException ignored) {}
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            FarmingStatic.turnipDrop = false;
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        ui.root.wdgmsg("gk", 27);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }
}
