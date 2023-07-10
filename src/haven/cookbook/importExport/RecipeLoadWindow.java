package haven.cookbook.importExport;

import haven.*;

public class RecipeLoadWindow extends Window {
    private Thread th;
    private volatile String prog = "Calculating... please wait";
    private double sprog = -1;

    public RecipeLoadWindow(String word) {
        super(UI.scale(new Coord(300, 65)), word, true);
        adda(new Button(UI.scale(100), "Cancel", false, this::cancel), csz().x / 2, UI.scale(40), 0.5, 0.0);
    }

    public void run(Thread th) {
        (this.th = th).start();
    }

    public void cdraw(GOut g) {
        g.text(prog, UI.scale(new Coord(10, 10)));
    }

    public void cancel() {
        th.interrupt();
    }

    public void tick(double dt) {
        if(!th.isAlive())
            destroy();
    }

    public void prog(String prog) {
        this.prog = prog;
    }
}
