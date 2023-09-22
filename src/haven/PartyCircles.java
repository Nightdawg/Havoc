package haven;


import haven.sprites.PartyCircleSprite;

import java.awt.*;
import java.util.*;

public class PartyCircles {
    public static Color MEMBER_OL_COLOR = OptWnd.partyMemberColorOptionWidget.currentColor;
    public static Color LEADER_OL_COLOR = OptWnd.partyLeaderColorOptionWidget.currentColor;
    public static Color MYSELF_OL_COLOR = OptWnd.myselfColorOptionWidget.currentColor;

    private final Party party;
    private final long playerId;
    private final HashMap<Gob, Gob.Overlay> overlays;

    public PartyCircles(Party party, long playerId) {
        this.party = party;
        this.playerId = playerId;
        this.overlays = new HashMap<Gob, Gob.Overlay>();
    }

    public void update() {
        Collection<Gob> old = new HashSet<Gob>(overlays.keySet());
        if (party.memb.size() > 1) {
            for (Party.Member m : party.memb.values()) {
                Gob gob = m.getgob();
                if (gob == null)
                    continue;
                if (OptWnd.partyMembersCirclesCheckBox.a && m == party.leader)
                    highlight(gob, LEADER_OL_COLOR);
                else if (OptWnd.partyMembersCirclesCheckBox.a && m.gobid == playerId && m != party.leader)
                    highlight(gob, MYSELF_OL_COLOR);
                else if (OptWnd.partyMembersCirclesCheckBox.a && m != party.leader)
                    highlight(gob, MEMBER_OL_COLOR);
                else
                    unhighlight(gob);
                old.remove(gob);
            }
        } else { // ND: This iterator stuff is a lil confusing to me atm, but it works.
            for (Gob gob : old)
                unhighlight(gob);
        }
    }

    private void highlight(Gob gob, Color color) {
        if (overlays.containsKey(gob) && overlays.get(gob).spr.partyMemberColor == color)
            return;
        Gob.Overlay existingOverlay = overlays.remove(gob);
        if (existingOverlay != null)
            gob.removeOl(existingOverlay);
        Gob.Overlay overlay = new Gob.Overlay(gob, new PartyCircleSprite(gob, color));
        gob.addol(overlay);
        overlays.put(gob, overlay);
    }

    private void unhighlight(Gob gob) {
        Gob.Overlay overlay = overlays.remove(gob);
        if (overlay != null)
            gob.removeOl(overlay);
    }
}