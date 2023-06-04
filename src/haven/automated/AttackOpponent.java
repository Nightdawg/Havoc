package haven.automated;


import haven.GameUI;
import haven.Gob;

import static haven.automated.AUtils.attackGob;

public class AttackOpponent implements Runnable {
    private GameUI gui;
    private final long gobid;

    public AttackOpponent(GameUI gui, long gobid) {
        this.gui = gui;
        this.gobid = gobid;
    }

    @Override
    public void run() {
        Gob gob = gui.map.glob.oc.getgob(gobid);
        attackGob(gui, gob);
    }
}
