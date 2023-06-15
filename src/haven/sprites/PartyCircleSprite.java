package haven.sprites;

import haven.Gob;
import haven.sprites.baseSprite.ColoredCircleSprite;

import java.awt.*;

public class PartyCircleSprite extends ColoredCircleSprite {

    public PartyCircleSprite(final Gob g, final Color col) {
        super(g, col, 3.5f, 5.6f, 0.5f);
        super.partyMemberColor = col;
    }
}