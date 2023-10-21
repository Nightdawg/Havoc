package haven.automated;


import haven.GameUI;
import haven.Gob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import static haven.automated.AUtils.attackGob;

public class AggroNearestTarget implements Runnable {
    private final GameUI gui;

    public AggroNearestTarget(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        synchronized (gui.fv.lsrel) {

            if (gui.fv != null && gui.fv.lsrel.size() > 0) {//If we are in a fight already:
                //Check type of fight - Are we fighting players or just animals?
                HashSet<Long> fightgobs = gui.fv.lsrel.stream().map(rel -> rel.gobid).collect(Collectors.toCollection(HashSet::new));
                HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackableMap(gui);
                HashSet<Long> aggrodplayers = fightgobs.stream().filter(id -> allAttackableMap.get(id) != null && isPlayer(allAttackableMap.get(id))).collect(Collectors.toCollection(HashSet::new));

                Gob player = gui.map.player();
                if (player == null)
                    return;

                // If we are fighting players dont attack animals
                if (!aggrodplayers.isEmpty()) {
                    attackNearestNonAttackedPlayer(allAttackableMap, aggrodplayers, player);
                    return;
                } else if (attackClosestAttackablePlayer()){
                    return;
                } else {
                    attackNearestNonAttackedAnimal(fightgobs, allAttackableMap, player);
                    return;
                }
            } else {//If we are not in a fight:
                    //Otherwise just attack closest attackable object
                    attackClosestAttackable();
                    return;

            }
        }
    }

    private void attackNearestNonAttackedAnimal(HashSet<Long> fightgobs, HashMap<Long, Gob> allAttackableMap, Gob player) {
        //If we are fighting animals, try to attack second closest animal
        Gob closestEnemy = null;
        OUTER_LOOP: for (Gob gob : allAttackableMap.values()) {
            //if friend, skip it... skip it
            if (isPlayer(gob) && gob.isFriend())
                continue;
            if (gob.getres().name.equals("gfx/kritter/horse/horse") && gob.occupants.size() > 0){ // ND: Wild horse special case. Tamed horses are never attacked anyway
                for (Gob occupant : gob.occupants) {
                    if (occupant.isFriend() || occupant.isMe()){
                        continue OUTER_LOOP;
                    }
                }
            }
            if (!fightgobs.contains(gob.id)) {
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

    private boolean attackClosestAttackablePlayer() {
        Gob player = gui.map.player();
        if (player == null)
            return false;
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
            return true;
        }
        return false;
    }

    private void attackClosestAttackable() {
        Gob player = gui.map.player();
        if (player == null)
            return;
        HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackableMap(gui);

        //If theres no last attacked gob:
        // try and find the closest animal or player to attack
        Gob closestEnemy = null;
        OUTER_LOOP: for (Gob gob : allAttackableMap.values()) {
            if (isPlayer(gob) && gob.isFriend()) {
                continue;
            }
            if (gob.getres().name.equals("gfx/kritter/horse/horse") && gob.occupants.size() > 0){ // ND: Wild horse special case. Tamed horses are never attacked anyway
                for (Gob occupant : gob.occupants) {
                    if (occupant.isFriend() || occupant.isMe()){
                        continue OUTER_LOOP;
                    }
                }
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
