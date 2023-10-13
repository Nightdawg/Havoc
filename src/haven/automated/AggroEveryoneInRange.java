package haven.automated;


import haven.GameUI;
import haven.Gob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;


public class AggroEveryoneInRange implements Runnable {
    private final GameUI gui;

    public AggroEveryoneInRange(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        synchronized (gui.fv.lsrel) {
            if (gui.fv != null && gui.fv.lsrel.size() > 0) {
                HashSet<Long> fightgobs = gui.fv.lsrel.stream().map(rel -> rel.gobid).collect(Collectors.toCollection(HashSet::new));
                HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackableMap(gui);
                HashSet<Long> aggrodplayers = fightgobs.stream().filter(id -> allAttackableMap.get(id) != null && isPlayer(allAttackableMap.get(id))).collect(Collectors.toCollection(HashSet::new));

                Gob player = gui.map.player();
                if (player == null)
                    return;

                aggroAllNonFriendlyAndNotAttackedPlayers(allAttackableMap, aggrodplayers, player);
            } else {
                HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackableMap(gui);
                Gob player = gui.map.player();
                if (player == null)
                    return;
                aggroAllNonFriendlyPlayers(allAttackableMap, player);
            }
        }
    }

    private void aggroAllNonFriendlyAndNotAttackedPlayers(HashMap<Long, Gob> allAttackableMap, HashSet<Long> aggrodplayers, Gob player) {
        for (Gob gob : allAttackableMap.values()) {
            if (isPlayer(gob) && !aggrodplayers.contains(gob.id) && !gob.isFriend() && !gob.getPoses().contains("knock")) {
                if (gob.rc.dist(player.rc) < 195) {
                    AUtils.attackGob(gui, gob);
                }
            }
            Integer ping = GameUI.getPingValue();
            sleep(ping != null ? ping : 20);
        }
    }

    private void aggroAllNonFriendlyPlayers(HashMap<Long, Gob> allAttackableMap, Gob player) {
        for (Gob gob : allAttackableMap.values()) {
            if (isPlayer(gob) && !gob.isFriend()&& !gob.getPoses().contains("knock")) {
                if (gob.rc.dist(player.rc) < 195) {
                    AUtils.attackGob(gui, gob);
                }
            }
            Integer ping = GameUI.getPingValue();
            sleep(ping != null ? ping : 20);
        }
    }

    private boolean isPlayer(Gob gob) {
        return gob.getres() != null && gob.getres().name != null && gob.getres().name.equals("gfx/borka/body");
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }
}