package haven;

public class QuickSlotsWdg extends Widget implements DTarget {
    private static final Tex sbg = Resource.loadtex("gfx/hud/quickslots");
    public static final Coord lc = UI.scale(new Coord(6, 6));
    public static final Coord rc = UI.scale(new Coord(56, 6));
    private static final Coord ssz = UI.scale(new Coord(44, 44));
    private UI.Grab dragging;
    private Coord dc;

    public QuickSlotsWdg() {
        super(UI.scale(new Coord(44 + 44 + 6, 44)));
    }

    @Override
    public void draw(GOut g) {
        Equipory e = gameui().getequipory();
        if (e != null) {
            g.image(sbg, Coord.z);
            WItem left = e.slots[6];
            if (left != null) {
                drawitem(g.reclipl(lc, g.sz()), left);
            }
            WItem right = e.slots[7];
            if (right != null) {
                drawitem(g.reclipl(rc, g.sz()), right);
            }
        }
    }

    private void drawitem(GOut g, WItem witem) {
        GItem item = witem.item;
        GSprite spr = item.spr();
        if (spr != null) {
            g.defstate();
            witem.drawmain(g, spr);
            g.defstate();
        } else {
            g.image(WItem.missing.layer(Resource.imgc).tex(), Coord.z, ssz);
        }
    }


    @Override
    public boolean drop(Coord cc, Coord ul) {
        Equipory e = gameui().getequipory();
        if (e != null) {
            e.wdgmsg("drop", cc.x <= 47 ? 6 : 7);
            return true;
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = gameui().getequipory();
        if (e != null) {
            WItem w = e.slots[cc.x <= 47 ? 6 : 7];
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }
        return false;
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return true;
        if (ui.modmeta)
            return true;
        if (((ui.modctrl || ui.modshift) && button == 1) || button == 2) {
            if((dragging != null)) { // ND: I need to do this extra check and remove it in case you do another click before the mouseup. Idk why it has to be done like this, but it solves the issue.
                dragging.remove();
                dragging = null;
            }
            dragging = ui.grabmouse(this);
            dc = c;
            return true;
        }
        Equipory e = gameui().getequipory();
        if (e != null) {
            WItem w = e.slots[c.x <= 47 ? 6 : 7];
            if (w != null) {
                w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                return true;
            } else if (button == 1) {
                if((dragging != null)) { // ND: Same thing as above
                    dragging.remove();
                    dragging = null;
                }
                dragging = ui.grabmouse(this);
                dc = c;
                return true;
            }
        }
        return false;
    }

    public void simulateclick(Coord c) {
        Equipory e = gameui().getequipory();
        if (e != null) {
            WItem w = e.slots[c.x <= 47 ? 6 : 7];
            if (w != null)
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
        }
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        checkIfOutsideOfUI(); // ND: Prevent the widget from being dragged outside the current window size
        if((dragging != null)) {
            dragging.remove();
            dragging = null;
            Utils.setprefc("wndc-quickslots", this.c);
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

    public void checkIfOutsideOfUI() {
        if (this.c.x < 0)
            this.c.x = 0;
        if (this.c.y < 0)
            this.c.y = 0;
        if (this.c.x > (gameui().sz.x - this.sz.x))
            this.c.x = gameui().sz.x - this.sz.x;
        if (this.c.y > (gameui().sz.y - this.sz.y))
            this.c.y = gameui().sz.y - this.sz.y;
    }
}