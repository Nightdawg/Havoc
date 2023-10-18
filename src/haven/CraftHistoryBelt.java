package haven;

import static haven.Inventory.invsq;

public class CraftHistoryBelt extends Widget {
    private static final int SIZE = 8;
    private MenuGrid.Pagina[] belt = new MenuGrid.Pagina[SIZE];
    private UI.Grab dragging;
    private Coord dc;
    private static final Coord vsz = UI.scale(34, 290);
    private static final Coord hsz = UI.scale(290, 34);
    private boolean vertical;

    public CraftHistoryBelt(boolean vertical) {
        super(vertical ? vsz : hsz);
        this.vertical = vertical;
    }

    private Coord beltc(int i) {
        if (vertical)
            return new Coord(0, (invsq.sz().x + 2) * i);
        return new Coord((invsq.sz().x + 2) * i, 0);
    }

    private int beltslot(Coord c) {
        for (int i = 0; i < SIZE; i++) {
            if (c.isect(beltc(i), invsq.sz()))
                return i;
        }
        return -1;
    }

    @Override
    public void draw(GOut g) {
        for (int i = 0; i < SIZE; i++) {
            int slot = i;
            Coord c = beltc(i);
            g.image(invsq, c);
            if (belt[slot] != null)
                g.image(belt[slot].img, c.add(1, 1));
        }
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        int slot = beltslot(c);
        if (button == 1 && ui.modmeta && !ui.modshift && !ui.modctrl) {
            if (vertical) {
                sz = hsz;
                vertical = false;
            } else {
                sz = vsz;
                vertical = true;
            }
            Utils.setprefb("histbelt_vertical", vertical);
            checkIfOutsideOfUI();
            return true;
        }
        if (((ui.modctrl || ui.modshift) && button == 1) || button == 2) {
            if((dragging != null)) { // ND: I need to do this extra check and remove it in case you do another click before the mouseup. Idk why it has to be done like this, but it solves the issue.
                dragging.remove();
                dragging = null;
            }
            dragging = ui.grabmouse(this);
            dc = c;
            return true;
        }
        if (slot != -1) {
            if (button == 1 && belt[slot] != null) {
                String[] ad = belt[slot].act().ad;
                ui.gui.act(ad);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        checkIfOutsideOfUI();
        if (dragging != null) {
            dragging.remove();
            dragging = null;
            Utils.setprefc("histbelt_c", this.c);
            return true;
        }
        return super.mouseup(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        if (dragging != null) {
            this.c = this.c.add(c.x, c.y).sub(dc);
            return;
        }
        super.mousemove(c);
    }

    public void push(MenuGrid.Pagina pagina) {
        for (MenuGrid.Pagina p : belt) {
            if (p == pagina)
                return;
        }
        for (int i = SIZE - 2; i >= 0; i--)
            belt[i + 1] = belt[i];
        belt[0] = pagina;
    }
    public void checkIfOutsideOfUI() {
        if (this.c.x < 0)
            this.c.x = 0;
        if (this.c.y < 0)
            this.c.y = 0;
        if (this.c.x > (ui.gui.sz.x - this.sz.x))
            this.c.x = ui.gui.sz.x - this.sz.x;
        if (this.c.y > (ui.gui.sz.y - this.sz.y))
            this.c.y = ui.gui.sz.y - this.sz.y;
    }
}