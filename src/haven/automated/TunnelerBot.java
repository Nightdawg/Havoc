package haven.automated;

import haven.*;
import haven.automated.helpers.TileStatic;

import java.util.*;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class TunnelerBot extends Window implements Runnable {
    private final Button mineButton;
    private boolean autoMineActive;
    private boolean autoRoadActive;

    private final CheckBox mineToTheLeftCheckbox;
    private boolean mineToTheLeft = true;
    private final CheckBox mineToTheRightCheckbox;
    private boolean mineToTheRight = true;

    private final Label miningDirectionLabel;
    private Coord direction = new Coord(0, -1);
    private Coord directionPerpendicular = new Coord(-1, 0);
    private int milestoneRot = 19115;

    private MCache map;
    private final GameUI gui;
    private boolean stop;
    private ArrayList<Gob> columns = new ArrayList<>();

    private int stage;
    private Coord currentAnchorColumn;

    public TunnelerBot(GameUI gui) {
        super(new Coord(120, 185), "Auto Tunneler");
        this.gui = gui;
        stop = false;
        map = gui.map.glob.map;
        currentAnchorColumn = gui.map.player().rc.floor();
        stage = 0;
        autoMineActive = false;

        Button northDirection = new Button(25, "N") {
            @Override
            public void click() {
                changeDirection(1);
            }
        };
        add(northDirection, new Coord(45, 10));
        Button eastDirection = new Button(25, "E") {
            @Override
            public void click() {
                changeDirection(2);
            }
        };
        add(eastDirection, new Coord(75, 35));
        Button southDirection = new Button(25, "S") {
            @Override
            public void click() {
                changeDirection(3);
            }
        };
        add(southDirection, new Coord(45, 60));
        Button westDirection = new Button(25, "W") {
            @Override
            public void click() {
                changeDirection(4);
            }
        };
        add(westDirection, new Coord(15, 35));

        miningDirectionLabel = new Label("N");
        add(miningDirectionLabel, new Coord(52, 38));
        miningDirectionLabel.tooltip = RichText.render("Choose mining direction N-E-S-W (on map)", UI.scale(300));

        mineToTheLeftCheckbox = new CheckBox("Left") {
            {
                a = mineToTheLeft;
            }

            public void set(boolean val) {
                mineToTheLeft = val;
                a = val;
                resetParams();
            }
        };
        add(mineToTheLeftCheckbox, new Coord(15, 95));
        mineToTheLeftCheckbox.tooltip = RichText.render("Mine left branch near every column.\nIf disabled but other option (right) is\nenabled still need to mine 1 tile this way.", UI.scale(300));

        mineToTheRightCheckbox = new CheckBox("Right") {
            {
                a = mineToTheRight;
            }

            public void set(boolean val) {
                mineToTheRight = val;
                a = val;
                resetParams();
            }
        };
        add(mineToTheRightCheckbox, new Coord(60, 95));
        mineToTheRightCheckbox.tooltip = RichText.render("Mine right branch near every column.", UI.scale(300));

        CheckBox roadBox = new CheckBox("Autoroad") {
            {
                a = autoRoadActive;
            }

            public void set(boolean val) {
                autoRoadActive = val;
                a = val;
            }
        };
        add(roadBox, new Coord(15, 125));

        mineButton = new Button(100, "Start Mining") {
            @Override
            public void click() {
                autoMineActive = !autoMineActive;
                if (autoMineActive) {
                    this.change("Stop Mining");
                } else {
                    this.change("Start Mining");
                }
            }
        };
        add(mineButton, new Coord(10, 145));
    }

    @Override
    public void run() {
        try {
            sleep(2000);
            miningLoop:
            while (!stop) {
                if (gui.fv != null && gui.fv.current != null) {
                    fleeInOppositeDirection();
                    stage = -1;
                } else if (autoMineActive) {
                    if (gui.getmeter("stam", 0).a < 0.40) {
                        clearhand();
                        AUtils.drinkTillFull(gui, 0.99, 0.99);
                    }
                    clearhand();

                    List<Gob> looseRocks = AUtils.getGobsPartial("looserock", gui);
                    for (Gob rock : looseRocks) {
                        if (rock.rc.dist(gui.map.player().rc) < 125) {
                            ui.root.wdgmsg("gk", 27);
                            resetParams();
                            gui.msg("Loose rock in dangerous distance. Mining stopped.");
                        }
                    }

                    List<Gob> boulders = AUtils.getGobsPartial("bumlings", gui);
                    for (Gob boulder : boulders) {
                        if (boulder.rc.dist(gui.map.player().rc) < 20) {
                            clearhand();
                            AUtils.rightClickGobAndSelectOption(gui, boulder, 0);
                            AUtils.clickUiButton("paginae/act/mine", gui);
                            sleep(2000);
                            continue miningLoop;
                        }
                    }

                    if (stage == 0) {
                        columns = AUtils.getGobs("gfx/terobjs/column", gui);
                        if (columns.isEmpty()) {
                            gui.error("No column nearby.");
                            resetParams();
                            continue;
                        }

                        Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc.floor());
                        if (centerColumn == null) {
                            continue;
                        }
                        currentAnchorColumn = centerColumn.rc.floor().add(new Coord(direction).add(directionPerpendicular).mul(11));

                        //Check lines from column
                        int nextLine = checkLinesMined();
                        switch (nextLine) {
                            case 0:
                                stage = 4;
                                break;
                            case 1:
                                stage = 1;
                                break;
                            case 2:
                                stage = 2;
                                break;
                            case 3:
                                stage = 3;
                                break;
                        }

                        if (stage != 4 && !goToNearestColumn()) {
                            sleep(1000);
                            continue;
                        }

                    } else if (stage == 1) {
                        //mine forward
                        if (mineLine(currentAnchorColumn, direction, 10, true))
                            stage = 0;
                    } else if (stage == 2) {
                        //mine to the side
                        if (mineLine(currentAnchorColumn, directionPerpendicular, mineToTheLeft ? 10 : 1, false))
                            stage = 0;
                    } else if (stage == 3) {
                        //mine to the other side
                        if (mineLine(currentAnchorColumn, directionPerpendicular.inv(), 12, false))
                            stage = 0;
                    } else if (stage == 4) {
                        //building phase

                        //check if we need to build milestone
                        ArrayList<Gob> milestones = AUtils.getGobs("gfx/terobjs/road/milestone-stone-m", gui);
                        Coord2d playercood = gui.map.player().rc;
                        Gob closestMilestone = AUtils.closestGob(milestones, playercood.floor());
                        if (autoRoadActive && closestMilestone != null && closestMilestone.rc.floor().dist(currentAnchorColumn) > 19 * 11) {
                            stage = 5;
                        } else {
                            Coord nextColumnAdd = direction.mul(11).mul(10);
                            Coord nextColumnPos = currentAnchorColumn.add(nextColumnAdd);
                            if (checkForNearbyColumn(nextColumnPos)) {
                                gui.map.pfLeftClick(nextColumnPos, null);
                                AUtils.waitPf(gui);
                                stage = 0;
                            } else {
                                System.out.println("build next");
                                buildNextColumn(currentAnchorColumn);
                            }
                        }
                    } else if (stage == 5) {
                        buildMilestone();
                    } else if (stage == -1) {
                        fleeInOppositeDirection();
                    }
                }
                sleep(500);
            }
        } catch (InterruptedException e) {
            System.out.println("Tunneler interrupted..");
        }

    }

    private void buildMilestone() throws InterruptedException {
        ArrayList<Gob> milestones = AUtils.getGobs("gfx/terobjs/road/milestone-stone-m", gui);
        Coord2d playercood = gui.map.player().rc;
        Gob closestMilestone = AUtils.closestGob(milestones, playercood.floor());
        Coord addcoord = new Coord(0,0).sub(directionPerpendicular).mul(11);
        Coord newMilestonePos = currentAnchorColumn.add(addcoord);


        if (closestMilestone.rc.floor().dist(currentAnchorColumn) < 5 * 11) {
            stage = 0;
        }
        //mine tile where we put milestone
        else if (!AUtils.getTileName(newMilestonePos, map).equals("mine")) {
            gui.map.pfLeftClick(currentAnchorColumn.add(direction.mul(11)), null);
            AUtils.waitPf(gui);
            //Mine spot
            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", newMilestonePos.div(11), newMilestonePos.div(11), 0);
            int timeout = 0;
            while (timeout < 100 && !AUtils.getTileName(newMilestonePos, map).equals("mine")) {
                timeout++;
                sleep(100);
            }
        }
        //find rocks
        else if (!hasRocksInInv(5)) {
            findRocks();
        } else if (isClearPath(playercood, closestMilestone.rc)) {
            gui.map.wdgmsg("click", Coord.z, closestMilestone.rc.floor(posres), 3, 0, 0, (int) closestMilestone.id, closestMilestone.rc.floor(posres), 0, -1);
            sleep(500);
            Button extendButton = null;
            try {
                Window milestoneWindow = gui.getwnd("Milestone");
                if (milestoneWindow != null) {
                    for (Widget wi = milestoneWindow.lchild; wi != null; wi = wi.prev) {
                        if (wi instanceof Button) {
                            if (((Button) wi).text.text.equals("Extend"))
                                extendButton = (Button) wi;
                        }
                    }
                }
            } catch (NullPointerException e) {
            }
            if (extendButton != null) {
                extendButton.click();
                sleep(500);

                //Walk to CCC with milestone on cursor
                gui.map.wdgmsg("place", new Coord2d(currentAnchorColumn.x, currentAnchorColumn.y).floor(posres), milestoneRot, 1, 2);
                int timeout = 0;
                while (gui.map.player().rc.floor().dist(currentAnchorColumn) > 11 && timeout < 100) {
                    timeout++;
                    sleep(100);
                }

                Coord2d buildPos = new Coord2d(newMilestonePos.x, newMilestonePos.y);

                gui.map.wdgmsg("place", buildPos.floor(posres), milestoneRot, 1, 0);
                sleep(1000);
                AUtils.activateSign("Milestone", gui);
                waitBuildingConstruction("gfx/terobjs/road/milestone-stone-m");
                gui.map.wdgmsg("click", Coord.z, playercood.floor(posres), 3, 0);

            } else {
                gui.error("error when trying to extend road, the closest milestone cannot be extended!");
            }


        } else {
            Coord milestonevision = closestMilestone.rc.floor().add(directionPerpendicular.x*11, directionPerpendicular.y*11);
            gui.map.pfLeftClick(milestonevision, null);
            AUtils.waitPf(gui);
            AUtils.leftClick(gui, milestonevision);
            Thread.sleep(100);
            while (gui.map.player().getv() > 0 && !isClearPath(playercood, closestMilestone.rc.add(new Coord2d(directionPerpendicular).mul(5)))) {
                Thread.sleep(100);
            }
        }
    }

    private void clearhand() {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            }
        }
        AUtils.rightClick(gui);
    }

    private boolean checkForNearbyColumn(Coord pos) {
        columns = AUtils.getGobs("gfx/terobjs/column", gui);
        for (Gob gob : columns) {
            if (gob.rc.floor().dist(pos) < 44) {
                return true;
            }
        }
        return false;
    }

    private void buildNextColumn(Coord fromCenter) throws InterruptedException {
        AUtils.rightClick(gui);
        Coord addCoord = direction.mul(11).mul(10);
        Coord columnCoord = fromCenter.add(addCoord);
        Coord columnOffset = directionPerpendicular.inv().mul(11);
        ArrayList<Gob> constructions = AUtils.getGobs("gfx/terobjs/consobj", gui);
        try {
            constructions.sort((gob1, gob2) -> (int) (gob1.rc.dist(gui.map.player().rc) - gob2.rc.dist(gui.map.player().rc)));
        } catch (Exception ignored) {
        }
        if (!AUtils.getTileName(columnCoord.add(columnOffset), map).equals("mine")) {
            gui.map.pfLeftClick(columnCoord, null);
            AUtils.waitPf(gui);

            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", columnCoord.add(columnOffset).div(11), columnCoord.add(columnOffset).div(11), 0);
            int timeout = 0;
            while (timeout < 100 && !AUtils.getTileName(columnCoord.add(columnOffset), map).equals("mine")) {
                timeout++;
                sleep(100);
            }
        } else if (!hasRocksInInv(15)) {
            findRocks();
        } else if (!constructions.isEmpty()) {
            gui.map.pfLeftClick(columnCoord, null);
            AUtils.waitPf(gui);
            Gob closeConstr = constructions.get(0);
            gui.map.wdgmsg("click", Coord.z, closeConstr.rc.floor(posres), 3, 0, 0, (int) closeConstr.id, closeConstr.rc.floor(posres), 0, -1);
            sleep(1000);
            AUtils.activateSign("Stone Column", gui);
            waitBuildingConstruction("gfx/terobjs/column");
        } else {
            gui.map.pfLeftClick(columnCoord, null);
            AUtils.waitPf(gui);
            AUtils.clickUiButton("paginae/bld/column", gui);
            sleep(300);
            Coord2d buildPos = new Coord2d(columnCoord.add(columnOffset).x, columnCoord.add(columnOffset).y);
            buildPos = buildPos
                    .sub(buildPos.x % MCache.tilesz.x, buildPos.y % MCache.tilesz.y)
                    .add(MCache.tilesz.x / 2, MCache.tilesz.y / 2).sub(MCache.tilesz);
            gui.map.wdgmsg("place", buildPos.floor(posres), 0, 1, 0);
            sleep(1000);
            AUtils.activateSign("Stone Column", gui);
            waitBuildingConstruction("gfx/terobjs/column");
        }
    }

    private void waitBuildingConstruction(String name) throws InterruptedException {
        int timeout = 0;
        while (timeout < 30 && hasRocksInInv(0) && !checkIfConstructed(name)) {
            sleep(200);
            timeout++;
        }
    }

    private boolean checkIfConstructed(String name) {
        ArrayList<Gob> colmns = AUtils.getGobs(name, gui);
        return AUtils.closestGob(colmns, gui.map.player().rc.floor()).rc.dist(gui.map.player().rc) < 20;
    }

    private void findRocks() throws InterruptedException {
        ArrayList<Gob> gobs = AUtils.getAllGobs(gui);
        Coord2d playerC = gui.map.player().rc;
        try {
            gobs.sort((gob1, gob2) -> (int) (gob1.rc.dist(playerC) - gob2.rc.dist(playerC)));
        } catch (Exception e) {
            //Illegal argument exception wtf?
        }
        for (Gob gob : gobs) {
            if (TileStatic.SUPPORT_MATERIALS.contains(gob.getres().basename())) {
                gui.map.pfLeftClick(gob.rc.floor(), null);
                AUtils.waitPf(gui);

                gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                sleep(2000);
                return;
            }
        }

        for (WItem wItem : gui.maininv.getAllItems()) {
            try {
                if (TileStatic.SUPPORT_MATERIALS.contains(wItem.item.getres().basename())) {
                    //do something with rock
                }
            } catch (Loading e) {
            }
        }
    }

    private boolean hasRocksInInv(int num) {
        int rocksAmount = 0;
        for (WItem wItem : gui.maininv.getAllItems()) {
            if (TileStatic.SUPPORT_MATERIALS.contains(wItem.item.getres().basename())) {
                rocksAmount++;
            }
        }
        for (WItem wItem : gui.getAllContentsWindows()) {
            if (TileStatic.SUPPORT_MATERIALS.contains(wItem.item.getres().basename())) {
                rocksAmount++;
            }
        }
        return rocksAmount > num || gui.maininv.getFreeSpace() == 0;
    }

    private Integer checkLinesMined() {
        Coord dir1 = direction; //forward
        Coord dir2 = directionPerpendicular; //to the side
        Coord dir3 = directionPerpendicular.inv(); //to the other side

        if ((!checkLineMined(currentAnchorColumn, dir2, mineToTheLeft ? 10 : 1)) &&(mineToTheRight || mineToTheLeft)) {
            return 2;
        } else if ((!checkLineMined(currentAnchorColumn, dir3, 12)) && mineToTheRight) {
            return 3;
        } else if (!checkLineMined(currentAnchorColumn, dir1, 10)) {
            return 1;
        }

        return 0;
    }

    private boolean checkLineMined(Coord place, Coord dir, int length) {
        for (int i = 0; i <= length; i++) {
            Coord dirmul = dir.mul(11).mul(i);
            if (!AUtils.getTileName(place.add(dirmul), map).equals("mine")) {
                return false;
            }
        }
        return true;
    }

    private boolean mineLine(Coord place, Coord dir, int length, boolean last) throws InterruptedException {
        Coord end = dir.mul(11).mul(length);
        Coord dirmul;
        Coord mineplace = new Coord(0, 0);
        int tilesToMine = 0;
        for (int i = 0; i <= length; i++) {
            dirmul = dir.mul(11).mul(i);
            mineplace = place.add(dirmul);
            if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(mineplace, map))) {
                tilesToMine++;
            }
        }
        if (tilesToMine > 0) {
            AUtils.clickUiButton("paginae/act/mine", gui);
            if(!((gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("choppan")) || gui.map.player().getPoses().contains("drinkan"))){
                gui.map.wdgmsg("sel", place.div(11), place.add(end).div(11), 0);
            }
            sleep(500);
            return false;
        } else {
            if (!last) {
                gui.map.pfLeftClick(currentAnchorColumn, null);
            }
            AUtils.waitPf(gui);
            return true;
        }
    }

    private boolean isClearPath(Coord2d fromd, Coord2d tod) {
        Coord2d direction = tod.sub(fromd);
        double dirLen = fromd.dist(tod);
        if (dirLen < 21) {
            return true;
        }
        Coord2d directionNorm = direction.div(dirLen);
        for (int i = 1; i < dirLen / 11; i++) {
            Coord2d addCoord = directionNorm.mul(11).mul(i);
            if (!TileStatic.MINE_WALKABLE_TILES.contains(AUtils.getTileName(fromd.add(addCoord).floor(), map))) {
                return false;
            }
        }
        return true;
    }

    private boolean goToNearestColumn() throws InterruptedException {
        if (AUtils.getTileName(currentAnchorColumn, map).equals("mine")) {
            gui.map.pfLeftClick(currentAnchorColumn, null);
            AUtils.waitPf(gui);
            return true;
        } else if (currentAnchorColumn.dist(gui.map.player().rc.floor()) < 22) {
            AUtils.clickUiButton("paginae/act/mine", gui);
            gui.map.wdgmsg("sel", gui.map.player().rc.floor().div(11), currentAnchorColumn.div(11), 0);
            sleep(500);
            return true;
        } else {
            gui.error("cannot not walk to nearest column, try to mine around it");
            return false;
        }
    }

    private void fleeInOppositeDirection() {
        try {
            columns = AUtils.getGobs("gfx/terobjs/column", gui);
            Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc.floor());
            currentAnchorColumn = centerColumn.rc.floor().add(new Coord(direction).add(directionPerpendicular).mul(11));
            if (AUtils.getTileName(currentAnchorColumn, map).equals("mine")) {
                Thread.sleep(500);
                gui.map.pfLeftClick(currentAnchorColumn.sub(direction.mul(2 * 11)), null);
                AUtils.waitPf(gui);
                Coord addDirection = direction.inv().mul(11).mul(12);

                centerColumn = AUtils.closestGob(columns, currentAnchorColumn.add(addDirection));

                currentAnchorColumn = centerColumn.rc.floor().add(new Coord(direction).add(directionPerpendicular).mul(11));
                gui.map.pfLeftClick(currentAnchorColumn.sub(direction.mul(2 * 11)), null);
                AUtils.waitPf(gui);
            } else {
                gui.error("PANIC! Cannot find a path to flee.");
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void changeDirection(int dir) {
        if (dir == 1) {
            resetParams();
            miningDirectionLabel.settext("N");
            direction = new Coord(0, -1);
            directionPerpendicular = new Coord(-1, 0);
            milestoneRot = 19115;
        } else if (dir == 2) {
            resetParams();
            miningDirectionLabel.settext("E");
            direction = new Coord(1, 0);
            directionPerpendicular = new Coord(0, -1);
            milestoneRot = -30037;
        } else if (dir == 3) {
            resetParams();
            miningDirectionLabel.settext("S");
            direction = new Coord(0, 1);
            directionPerpendicular = new Coord(1, 0);
            milestoneRot = -13653;
        } else if (dir == 4) {
            resetParams();
            miningDirectionLabel.settext("W");
            direction = new Coord(-1, 0);
            directionPerpendicular = new Coord(0, 1);
            milestoneRot = 2731;
        }
    }

    private void resetParams() {
        map = gui.map.glob.map;
        stage = 0;
        columns = AUtils.getGobs("gfx/terobjs/column", gui);
        Gob centerColumn = AUtils.closestGob(columns, gui.map.player().rc.floor());
        if (centerColumn != null) {
            currentAnchorColumn = centerColumn.rc.floor().add(new Coord(direction).add(directionPerpendicular).mul(11));
        }
        mineButton.change("Start Mining");
        autoMineActive = false;
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.tunnelerBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        if (gui.tunnelerBotThread != null) {
            gui.tunnelerBotThread.interrupt();
            gui.tunnelerBotThread = null;
        }
        this.destroy();
    }
}


