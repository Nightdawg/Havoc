package haven;


import java.awt.*;
import java.util.*;

public class PartyHighlight {
    public static final Color MEMBER_OL_COLOR = new Color(0, 160, 0, 128);
    public static final Color LEADER_OL_COLOR = new Color(0, 74, 208, 128);
    public static final Color MYSELF_OL_COLOR = new Color(255, 255, 255, 64);

    private final Party party;
    private final long playerId;
    private final HashMap<Gob, GobHighlightParty> overlays;

    public PartyHighlight(Party party, long playerId) {
        this.party = party;
        this.playerId = playerId;
        this.overlays = new HashMap<Gob, GobHighlightParty>();
    }

    public void update() {
        if (party.memb.size() > 1) {
            for (Party.Member m : party.memb.values()) {
                Gob gob = m.getgob();
                if (gob == null)
                    continue;
                if (OptWnd.partyMembersHighlightCheckBox.a && m == party.leader)
                    highlight(gob, LEADER_OL_COLOR);
                else if (OptWnd.partyMembersHighlightCheckBox.a && m.gobid == playerId && m != party.leader)
                    highlight(gob, MYSELF_OL_COLOR);
                else if (OptWnd.partyMembersHighlightCheckBox.a && m != party.leader)
                    highlight(gob, MEMBER_OL_COLOR);
                else
                    unhighlight(gob);
            }
        } else { // ND: This iterator stuff is a lil confusing to me atm, but it works.
            Iterator<Map.Entry<Gob, GobHighlightParty>> iter = overlays.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Gob, GobHighlightParty> new_Map = (Map.Entry<Gob, GobHighlightParty>) iter.next();
                new_Map.getKey().delattr(GobHighlightParty.class);
                iter.remove();
            }
        }
    }

    private void highlight(Gob gob, Color color) {
        if (overlays.containsKey(gob) && overlays.get(gob).c == color)
            return;
        GobHighlightParty overlay = new GobHighlightParty(gob, color);
        gob.setattr(overlay);
        overlays.put(gob, overlay);
    }

    private void unhighlight(Gob gob) {
        GobHighlightParty overlay = overlays.remove(gob);
        if (overlay != null)
            gob.delattr(GobHighlightParty.class);
    }
}