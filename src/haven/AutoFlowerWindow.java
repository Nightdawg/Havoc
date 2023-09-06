package haven;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static haven.OCache.posres;

public class AutoFlowerWindow extends Window {
    private PetalList petalList;

    public AutoFlowerWindow() {
        super(UI.scale(300, 350), "Auto-Select Manager (Flower Menus)");

        add(new CheckBox("Enable Automated Selection") {
            {
                a = Utils.getprefb("autoFlowerMenuSelect", false);
            }
            public void set(boolean val) {
                a = val;
                GameUI.autoFlowerSelect = val;
                Utils.setprefb("autoFlowerMenuSelect", val);
            }
        }, UI.scale(10, 10));

        add(new Button(UI.scale(100), "Refresh List"){
            @Override
            public void click() {
                refresh();
            }
        }, UI.scale(210, 2));

        add(new Label("Auto-Select Options:", new Text.Foundry(Text.sans, 12)), UI.scale(106, 40));

        petalList = new PetalList(UI.scale(294), 12);
        add(petalList, UI.scale(25, 60));
        refresh();
        add(new Label("New items are added to this list as you discover them.", new Text.Foundry(Text.sans, 12)), UI.scale(10, 335));
        add(new Label("Don't forget to Refresh the list if you don't see a new item!"), UI.scale(20, 355));
        this.c = new Coord (200, 100);
        pack();
    }

    public void refresh() {
        petalList.clearItems();
        for(Map.Entry<String, Boolean> petal: FlowerMenu.autoChoose.entrySet()){
            petalList.addItem(new PetalItem(petal.getKey(), petal.getValue()));
        }
    }


    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (Objects.equals(msg, "close"))) {
            hide();
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public class PetalList extends Widget {

        ArrayList<PetalItem> items = new ArrayList<>();
        Scrollbar sb;
        int rowHeight = UI.scale(22);
        int rows, w;

        public PetalList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = new Coord(w, rowHeight * rows);
            sb = new Scrollbar(rowHeight * rows, 0, 100);
            add(sb, UI.scale(0, 0));
        }

        public PetalItem listitem(int i) {
            return items.get(i);
        }

        public void addItem(PetalItem item) {
            add(item);
            items.add(item);
        }

        public void clearItems() {
            this.children().forEach(w -> {if(w instanceof PetalItem)w.destroy();});
            items.clear();
        }

        public int listitems() {
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
            if(row >= items.size())
                return super.mousedown(c, button);
            if(items.get(row).mousedown(c.sub(UI.scale(15), c.y / rowHeight * rowHeight), button))
                return true;
            return super.mousedown(c, button);
        }

        @Override
        public boolean mouseup(Coord c, int button) {
            int row = c.y / rowHeight + sb.val;
            if(row >= items.size())
                return super.mouseup(c, button);
            if(items.get(row).mouseup(c.sub(UI.scale(15), c.y / rowHeight * rowHeight), button))
                return true;
            return super.mouseup(c, button);
        }

        @Override
        public void draw(GOut g) {
            sb.max = items.size()-rows;
            for(int i=0; i<rows; i++) {
                if(i+sb.val >= items.size())
                    break;
                GOut ig = g.reclip(new Coord(UI.scale(15), i*rowHeight), new Coord(w-UI.scale(15), rowHeight));
                items.get(i+sb.val).draw(ig);
            }
            super.draw(g);
        }

    }

    public static class PetalItem extends Widget {

        public String name;

        public PetalItem(String name, boolean value) {
            this.name = name;
            add(new CheckBox(name) {
                {
                    a = value;
                }
                public void set(boolean val) {
                    FlowerMenu.updateValue(name, val);
                    a = val;
                }
            }, UI.scale(0, 5));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }

        @Override
        public void mousemove(Coord c) {
            if(c.x > 470)
                super.mousemove(c.sub(UI.scale(15), 0));
            else
                super.mousemove(c);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if(super.mousedown(c, button))
                return true;
            return false;
        }
    }
}
