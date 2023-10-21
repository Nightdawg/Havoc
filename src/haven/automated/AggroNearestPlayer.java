package haven.automated;


import haven.GameUI;
import haven.Gob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class AggroNearestPlayer implements Runnable {
    private final GameUI gui;

    public AggroNearestPlayer(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        synchronized (gui.fv.lsrel) {

            if (gui.fv != null && gui.fv.lsrel.size() > 0) { // If we are in a fight already:
                HashSet<Long> fightgobs = gui.fv.lsrel.stream().map(rel -> rel.gobid).collect(Collectors.toCollection(HashSet::new));
                HashMap<Long, Gob> allAttackablePlayersMap = AUtils.getAllAttackablePlayersMap(gui);
                HashSet<Long> aggrodplayers = fightgobs.stream().filter(id -> allAttackablePlayersMap.get(id) != null && isPlayer(allAttackablePlayersMap.get(id))).collect(Collectors.toCollection(HashSet::new));

                Gob player = gui.map.player();
                if (player == null)
                    return;

                if (!aggrodplayers.isEmpty()) {
                    attackNearestNonAttackedPlayer(allAttackablePlayersMap, aggrodplayers, player);
                    return;
                } else {
                    attackClosestAttackablePlayer();
                    return;
                }
            } else { // If we are not in a fight:
                attackClosestAttackablePlayer();
                return;
            }
        }
    }


    private void attackNearestNonAttackedPlayer(HashMap<Long, Gob> allAttackableMap, HashSet<Long> aggrodplayers, Gob player) {
        Gob closestEnemy = null;
        for (Gob gob : allAttackableMap.values()) {
            //if gob is an enemy player and not already aggroed
            if (isPlayer(gob) && !aggrodplayers.contains(gob.id) && !gob.isFriend()) {
                if (closestEnemy == null || gob.rc.dist(player.rc) < closestEnemy.rc.dist(player.rc)) {
                    closestEnemy = gob;
                }
            }
        }

        if (closestEnemy != null) {
            AUtils.attackGob(gui, closestEnemy);
            return;
        }
    }

    private void attackClosestAttackablePlayer() {
        Gob player = gui.map.player();
        if (player == null)
            return;
        HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackablePlayersMap(gui);

        Gob closestEnemy = null;
        for (Gob gob : allAttackableMap.values()) {
            if (isPlayer(gob) && gob.isFriend()) {
                continue;
            }
            //if gob is an enemy player and not already aggroed
            if ((closestEnemy == null || gob.rc.dist(player.rc) < closestEnemy.rc.dist(player.rc))
                    && (gob.knocked == null || (gob.knocked != null && !gob.knocked))) { // ND: Retarded workaround that I need to add, just like in Gob.java
                closestEnemy = gob;
            }
        }
        
        if (closestEnemy != null) {
            AUtils.attackGob(gui, closestEnemy);
            return;
        }
    }

    private boolean isPlayer(Gob gob){
        return gob.getres() != null && gob.getres().name != null && gob.getres().name.equals("gfx/borka/body");
    }
}
