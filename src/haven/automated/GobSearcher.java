package haven.automated;

import haven.*;

import java.util.Objects;

public class GobSearcher extends Window {
    public static String gobHighlighted = "";
    private final GameUI gui;

    public GobSearcher(GameUI gui) {
        super(UI.scale(140, 35), "Search for gobs:");
        this.gui = gui;
        gobHighlighted = "";
        TextEntry entry = new TextEntry(UI.scale(120), gobHighlighted) {
            @Override
            protected void changed() {
                setSearchValue(this.buf.line());
                updateOverlays();
            }
        };
        add(entry, UI.scale(10, 10));
    }

    public void updateOverlays() {
        synchronized (gui.ui.sess.glob.oc) {
            for (Gob gob : gui.ui.sess.glob.oc) {
                gob.setGobSearchOverlay();
            }
        }
    }

    public void setSearchValue(String value) {
        gobHighlighted = value;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            gobHighlighted = "";
            updateOverlays();
            reqdestroy();
            gui.gobSearcher = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
