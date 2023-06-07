package haven;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;


public class CheckpointManager extends Window implements Runnable {
    private static int delayMs = 250;
    private GameUI gui;
    private boolean stop = false;
    private boolean paused = true;
    private Label estimatedArrivalTime;
    public CheckpointList checkpointList;
    private int notMovingCounter = 0;
    private Button pause;
    private Coord2d lastPlayerCoord;


    public CheckpointManager(GameUI gui) {
        super(UI.scale(300, 200), "Checkpoint Route");
        this.gui = gui;
        this.lastPlayerCoord = gui.map.player().rc;
        checkpointList = new CheckpointManager.CheckpointList(390, 5);
        add(checkpointList, UI.scale(5, 5));
        pause = add(new Button(UI.scale(100), "Continue") {
            @Override
            public void click() {
                paused = !paused;
                this.change(paused ? "Continue" : "Pause");
            }
        }, UI.scale(25, 170));
        this.c = new Coord(100, 100);

        add(estimatedArrivalTime = new Label(""), UI.scale(150, 180));
    }

    public void setLastPlayerCoord(Coord2d lastPlayerCoord) {
        this.lastPlayerCoord = lastPlayerCoord;
    }

    @Override
    public void run() {
        while (!stop) {
            if (!paused) {
                if (checkpointList.listitems() > 0) {
                    Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
                    if (gui.map.player().rc.equals(lastPlayerCoord)) {
                        tryToMove(posres);
                        notMovingCounter++;
                    } else {
                        tryToMove(posres);
                        notMovingCounter = 0;
                    }
                    if (notMovingCounter == 10) {
                        gui.ui.error("Im stuck");
                        //TODO here should insert Alarm for being stuck but temporarily gui error.
                        notMovingCounter = 0;
                        paused = true;
                        pause.change("Continue");
                    }
                }
                try {
                    double distance = getWholeDistance();
                    if (distance > 0 && gui.map.player().gobSpeed > 0) {
                        estimatedArrivalTime.settext("est time ~ " + formatTime(Math.floor(distance / gui.map.player().gobSpeed)) + "");
                    }
                } catch (Exception ignored) {
                }
                setLastPlayerCoord(gui.map.player().rc);
            } else {
                notMovingCounter = 0;
                if (!estimatedArrivalTime.text.equals("")) {
                    estimatedArrivalTime.settext("");
                }
            }
            if (checkpointList.listitems() < 1) {
                this.hide();
                paused = true;
                pause.change("Continue");
            } else {
                this.show();
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void tryToMove(Coord2d posres) {
        gui.map.wdgmsg("click", Coord.z, checkpointList.items.get(0).coord.floor(posres), 1, 0);
        if(checkpointList.listitems() == 1 && gui.map.player().rc.dist(checkpointList.items.get(checkpointList.listitems()-1).coord) < 3){
            gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
            gui.msg("Destination reached.");
            checkpointList.deleteItem(checkpointList.items.get(0));
        } else if (checkpointList.listitems() > 0 && gui.map.player().rc.dist(checkpointList.items.get(0).coord) < 3) {
            checkpointList.deleteItem(checkpointList.items.get(0));
            if(checkpointList.listitems() > 0) {
                gui.map.wdgmsg("click", Coord.z, checkpointList.items.get(0).coord.floor(posres), 1, 0);
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            for (Widget w : this.children()) {
                if (w instanceof CheckpointList) {
                    if (w.children().size() > 0) {
                        w.mousewheel(Coord.z, -(w.children().size()));
                    }
                }
            }
            checkpointList.removeAllItems();
            this.hide();
            paused = true;
            pause.change("Continue");
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void addCoord(Coord2d coord) {
        synchronized (checkpointList) {
            checkpointList.addItem(new CheckPoint(coord));
        }
    }

    public double getWholeDistance() {
        double distance = 0;
        synchronized (checkpointList) {
            if (checkpointList.listitems() > 1) {
                Iterator<CheckPoint> iterator = checkpointList.items.iterator();
                CheckPoint currentCheckPoint = iterator.next();
                distance += currentCheckPoint.getCoord().dist(gui.map.player().rc);
                while (iterator.hasNext()) {
                    CheckPoint nextCheckPoint = iterator.next();
                    distance += currentCheckPoint.getCoord().dist(nextCheckPoint.getCoord());
                    currentCheckPoint = nextCheckPoint;
                }
            } else if (checkpointList.listitems() == 1) {
                Coord2d coord2d = checkpointList.items.get(0).coord;
                return coord2d.dist(gui.map.player().rc);
            }
        }
        return distance;
    }

    public static String formatTime(double timeInSeconds) {
        int hours = (int) (timeInSeconds / 3600);
        int remainder = (int) (timeInSeconds - hours * 3600);
        int minutes = remainder / 60;
        int seconds = remainder - minutes * 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }


    public List<Coord2d> getAllCoords() {
        List<Coord2d> coords = new ArrayList<>();
        synchronized (checkpointList) {
            for (CheckPoint checkPoint : checkpointList.items) {
                coords.add(checkPoint.getCoord());
            }
        }
        return coords;
    }

    //    Scrollable list of Checkpoints
    public class CheckpointList extends Widget {
        ArrayList<CheckPoint> items = new ArrayList<>();
        Scrollbar sb;
        int rowHeight = UI.scale(30);
        int rows, w;
        int currentIndex = -1;

        public CheckpointList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = UI.scale(w, rowHeight * rows);
            sb = new Scrollbar(rowHeight * rows, 0, 100);
            add(sb, UI.scale(0, 0));
        }

        public CheckPoint listitem(int i) {
            return items.get(i);
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public void addItem(CheckPoint item) {
            add(item);
            if (currentIndex < 0 || currentIndex > listitems()) {
                items.add(item);
                setCurrentIndex(-1);
            } else {
                items.add(currentIndex, item);
                setCurrentIndex(-1);
            }


        }

        public void deleteItem(CheckPoint item) {
            item.dispose();
            items.remove(item);
        }

        public void removeAllItems() {
            for (CheckPoint item : items) {
                item.dispose();
            }
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
            if (msg.equals("delete") && sender instanceof CheckPoint) {
                deleteItem((CheckPoint) sender);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }
    }


    public static class CheckPoint extends Widget {
        private final Label coordText;
        private final Coord2d coord;

        public Coord2d getCoord() {
            return coord;
        }

        public CheckPoint(Coord2d coord) {
            this.coord = coord;
            Widget prev;
            this.coordText = new Label(coord.floor().toString(), 100);
            prev = add(this.coordText, UI.scale(10, 4));

            prev = add(new Button(UI.scale(60), "+Prev") {
                @Override
                public boolean mousedown(Coord c, int button) {
                    if (button != 1) {
                        return true;
                    }
                    gameui().map.checkpointManager.checkpointList.setCurrentIndex(gameui().map.checkpointManager.checkpointList.items.indexOf(this.parent));
                    return super.mousedown(c, button);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ul").adds(100, -5));

            prev = add(new Button(UI.scale(60), "Preview") {
                @Override
                public boolean mousedown(Coord c, int button) {
                    if (button != 1) {
                        return true;
                    }
                    try {
                        Coord playerCoord = gameui().map.player().rc.floor(tilesz);
                        Coord actualCoord = coord.floor(tilesz);
                        MCache.Grid obg = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                        MapFile.GridInfo info = gameui().mapfile.file.gridinfo.get(obg.id);
                        Coord sc = actualCoord.add(info.sc.sub(obg.gc).mul(cmaps));
                        gameui().mapfile.view.center(new MiniMap.SpecLocator(info.seg, sc));
                    } catch (Exception ignored) {
                    }
                    return super.mousedown(c, button);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ul").adds(60, 0));

            prev = add(new Button(UI.scale(26), "X") {
                @Override
                public boolean mousedown(Coord c, int button) {
                    if (button != 1) {
                        return super.mousedown(c, button);
                    }
                    wdgmsg(this.parent, "delete");
                    return super.mousedown(c, button);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ul").adds(60, 0));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }

        @Override
        public void mousemove(Coord c) {
            if (c.x > 470)
                super.mousemove(c.sub(UI.scale(15), 0));
            else
                super.mousemove(c);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (super.mousedown(c, button))
                return true;
            return false;
        }
    }
}
