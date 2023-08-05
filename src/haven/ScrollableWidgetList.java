package haven;

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
    public boolean mousedown(Coord c, int button) {
        int row = c.y / rowHeight + sb.val;
        if (row >= items.size())
            return super.mousedown(c, button);
        if (items.get(row).mousedown(c.sub(UI.scale(15), c.y / rowHeight * rowHeight), button))
            return true;
        return super.mousedown(c, button);
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        int row = c.y / rowHeight + sb.val;
        if (row >= items.size())
            return super.mouseup(c, button);
        if (items.get(row).mouseup(c.sub(UI.scale(15), c.y / rowHeight * rowHeight), button))
            return true;
        return super.mouseup(c, button);
    }

    @Override
    public void draw(GOut g) {
        sb.max = items.size() - rows;
        for (int i = 0; i < rows; i++) {
            if (i + sb.val >= items.size())
                break;
            GOut ig = g.reclip(new Coord(UI.scale(15), i * rowHeight), UI.scale(w - UI.scale(15), rowHeight));
            items.get(i + sb.val).draw(ig);
        }
        super.draw(g);
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
