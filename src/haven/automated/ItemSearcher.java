package haven.automated;

import haven.*;

import java.util.Objects;

public class ItemSearcher extends Window {
    public static String itemHighlighted = "";
    private final GameUI gui;

    public ItemSearcher(GameUI gui) {
        super(UI.scale(140, 35), "Search for items:");
        this.gui = gui;
        itemHighlighted = "";
        TextEntry entry = new TextEntry(UI.scale(120), itemHighlighted) {
            @Override
            protected void changed() {
                setSearchValue(this.buf.line());
            }
        };
        add(entry, UI.scale(10, 10));
    }

    public void setSearchValue(String value) {
        itemHighlighted = value;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            itemHighlighted = "";
            reqdestroy();
            gui.itemSearcher = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
