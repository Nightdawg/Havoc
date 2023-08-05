package haven.customUI;

import haven.*;

import java.util.ArrayList;

public class ScrollableWidgetList<T extends Widget> extends Widget {
    Class<T> type;
    ArrayList<T> items = new ArrayList<>();
    Scrollbar sb;
    int rowHeight = UI.scale(30);
    int rows, w;

    public ScrollableWidgetList(int w, int rows, Class<T> type) {
        this.type = type;
        this.rows = rows;
        this.w = w;
        this.sz = new Coord(UI.scale(w), rowHeight * rows);
        sb = new Scrollbar(rowHeight * rows, 0, 100);
        add(sb, UI.scale(0, 0));
    }

    public T getItem(int index) {
        return items.get(index);
    }

    public void addItem(T item) {
        add(item);
        items.add(item);
    }

    public void deleteItem(T item) {
        item.dispose();
        items.remove(item);
        if (sb.max - 1 < sb.min) {
            sb.val = 0;
        } else if (sb.max - 1 < sb.val) {
            sb.val = sb.max - 1;
        }
    }

    public void removeAllItems() {
        for (T item : items) {
            item.dispose();
        }
        items.clear();
        sb.val = 0;
    }

    public int countAll() {
        return items.size();
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
        sb.ch(amount);
        return true;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (msg.equals("delete") && type.isInstance(sender)) {
            deleteItem(type.cast(sender));
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }
}
