package haven.automated;

import haven.*;
import haven.sprites.ClueSprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class PointerTriangulation extends Window {
    public static double pointerAngle = 0;
    private final GameUI gui;
    private List<LineData> lines;

    public PointerTriangulation(GameUI gui) {
        super(UI.scale(100, 100), "Triangulate");
        this.gui = gui;
        this.lines = new ArrayList<>();

        Button getCheckpoint = new Button(UI.scale(50), "Get") {
            @Override
            public void click() {
                saveCheckpoint();
            }
        };
        add(getCheckpoint, UI.scale(10, 10));

        Button drawLines = new Button(UI.scale(50), "Draw") {
            @Override
            public void click() {
                drawLines();
            }
        };
        add(drawLines, UI.scale(10, 40));

        Button clear = new Button(UI.scale(50), "Clear") {
            @Override
            public void click() {
                lines = new ArrayList<>();
            }
        };
        add(clear, UI.scale(10, 70));
    }

    public void drawLines() {
        if(gui.map.player() == null){
            return;
        }
        try {
            for (LineData lineData : lines) {
                Coord playerCoord = ui.gui.map.player().rc.floor(tilesz);
                MCache.Grid grid = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(grid.id);
                MapFile.Segment segment = ui.gui.mapfile.file.segments.get(info.seg);
                if (segment.id == lineData.segmentId) {
                    Coord gridCoords = null;
                    Coord curGridCoords = null;
                    for (Map.Entry<Coord, Long> segGrid : segment.map.entrySet()) {
                        if (Objects.equals(segGrid.getValue(), lineData.gridId)) {
                            gridCoords = segGrid.getKey();
                        }
                        if (segGrid.getValue().equals(grid.id)) {
                            curGridCoords = segGrid.getKey();
                        }
                        if (gridCoords != null && curGridCoords != null) {
                            break;
                        }
                    }
                    double calcX = gridCoords.x - curGridCoords.x;
                    double calcY = gridCoords.y - curGridCoords.y;
                    Coord2d firstCoord = new Coord2d(calcX * 1100 + grid.gc.x * 1100 + lineData.initCoords.x, calcY * 1100 + grid.gc.y * 1100 + lineData.initCoords.y);
                    gui.mapfile.view.addSprite(new ClueSprite(firstCoord, lineData.angle, lineData.angle, 1, 10000));

                } else {
                    gui.error("You have to be in the same segment for each pointer");
                    return;
                }
            }
        } catch (Exception e) {
            gui.error("Something went wrong.");
        }
    }

    public void saveCheckpoint() {
        if(gui.map.player() == null){
            return;
        }
        try {
            Gob player = gui.map.player();
            if(player == null){
                return;
            }
            Coord2d playerCoord = player.rc;
            Coord initialCoord = playerCoord.floor(MCache.tilesz);
            MCache.Grid grid = ui.sess.glob.map.getgrid(initialCoord.div(cmaps));
            MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(grid.id);
            Long segment = info.seg;
            double xValue = Math.floor((playerCoord.x - (grid.gc.x * 1100)) * 100) / 100;
            double yValue = Math.floor((playerCoord.y - (grid.gc.y * 1100)) * 100) / 100;
            lines.add(new LineData(segment, grid.id, new Coord2d(xValue, yValue), pointerAngle));
        } catch (Exception ignored) {}
    }

    private static class LineData {
        private final Long segmentId;
        private final Long gridId;
        private final Coord2d initCoords;
        private final double angle;

        public LineData(Long segmentId, Long gridId, Coord2d initCoords, double angle) {
            this.segmentId = segmentId;
            this.gridId = gridId;
            this.initCoords = initCoords;
            this.angle = angle;
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            reqdestroy();
            gui.pointerTriangulation = null;
        } else
            super.wdgmsg(sender, msg, args);
    }
}
