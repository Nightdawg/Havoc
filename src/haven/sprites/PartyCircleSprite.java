package haven.sprites;

import haven.Gob;
import haven.sprites.baseSprite.ColoredCircleSprite;

import java.awt.*;

public class PartyCircleSprite extends ColoredCircleSprite {

    public PartyCircleSprite(final Gob g, final Color col) {
        super(g, col, 3.8f, 5.25f, 0.6f);
        super.partyMemberColor = col;
    }
}