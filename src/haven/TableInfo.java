package haven;

public class TableInfo extends Widget {

    public static CheckBox saveCutleryCheckBox = null;

    public TableInfo(int x, int y) {
        this.sz = new Coord(x, y);

        add(saveCutleryCheckBox = new CheckBox("Anti Cutlery Breakage"){
            {a = Utils.getprefb("antiCutleryBreakage", true);}
            public void set(boolean val) {
                if (OptWnd.saveCutleryCheckBox != null)
                    OptWnd.saveCutleryCheckBox.set(val);
                a = val;
            }
        }, 10, 0);

        saveCutleryCheckBox.tooltip = RichText.render("Enabling this will cause any cutlery that has 1 wear left to be instantly transferred from the table into your inventory.\n$col[185,185,185]{A warning message will be shown, to let you know that the item has been transferred.}", 300);
    }

}
