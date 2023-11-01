package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GobGrowthInfo extends GobInfo {
    private static final int TREE_START = 10;
    private static final int BUSH_START = 30;
    private static final double TREE_MULT = 100.0 / (100.0 - TREE_START);
    private static final double BUSH_MULT = 100.0 / (100.0 - BUSH_START);
    private static final Color BG = new Color(0, 0, 0, 0);

    protected GobGrowthInfo(Gob owner) {
	super(owner);
    }

    @Override
	protected boolean enabled() {
		return OptWnd.toggleGobGrowthInfoCheckBox.a;
	}

    @Override
    protected Tex render() {
	if(gob == null || gob.getres() == null) { return null;}

	BufferedImage growth = growth();

	if(growth == null) {
	    return null;
	}

	return new TexI(ItemInfo.catimgsh(3, 0, BG, growth));
    }
    
    @Override
    public void dispose() {
	super.dispose();
    }

    private BufferedImage growth() {
	Text.Line line = null;
	Resource res = gob.getres();
	if(isSpriteKind(gob, "GrowingPlant", "TrellisPlant") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.removeCropsCheckbox.a)) {
	    int maxStage = 0;
	    for (FastMesh.MeshRes layer : gob.getres().layers(FastMesh.MeshRes.class)) {
		if(layer.id / 10 > maxStage) {
		    maxStage = layer.id / 10;
		}
	    }
	    Message data = getDrawableData(gob);
	    if(data != null) {
		int stage = data.uint8();
		if(stage > maxStage) {stage = maxStage;}
		Color c = Utils.blendcol((double) stage / maxStage, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN);
		line = Text.std.renderstroked(String.format("%d/%d", stage, maxStage), c, Color.BLACK);
	    }
	} else if(isSpriteKind(gob, "Tree")) {
		boolean isHidden = true;
	    Message data = getDrawableData(gob);
	    if(data != null && !data.eom()) {
		data.skip(1);
		int growth = data.eom() ? -1 : data.uint8();
		if(growth < 100 && growth >= 0) {
			if(res.name.contains("gfx/terobjs/trees") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.hideTreesCheckbox.a)) {
			growth = (int) (TREE_MULT * (growth - TREE_START));
			isHidden = false;
		    } else if(res.name.startsWith("gfx/terobjs/bushes") && !(OptWnd.toggleGobHidingCheckBox.a && OptWnd.hideBushesCheckbox.a)) {
			growth = (int) (BUSH_MULT * (growth - BUSH_START));
			isHidden = false;
		    }
			if (!isHidden) {
				Color c = Utils.blendcol(growth / 100.0, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN);
				line = Text.std.renderstroked(String.format("%d%%", growth), c, Color.BLACK);
			}
		}
	    }
	}

	if(line != null) {
	    return line.img;
	}
	return null;
    }

    private static Message getDrawableData(Gob gob) {
	Drawable dr = gob.getattr(Drawable.class);
	ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
	if(d != null)
	    return d.sdt.clone();
	else
	    return null;
    }
    
    private static boolean isSpriteKind(Gob gob, String... kind) {
	List<String> kinds = Arrays.asList(kind);
	boolean result = false;
	Class spc;
	Drawable d = gob.getattr(Drawable.class);
	Resource.CodeEntry ce = gob.getres().layer(Resource.CodeEntry.class);
	if(ce != null) {
	    spc = ce.get("spr");
	    result = spc != null && (kinds.contains(spc.getSimpleName()) || kinds.contains(spc.getSuperclass().getSimpleName()));
	}
	if(!result) {
	    if(d instanceof ResDrawable) {
		Sprite spr = ((ResDrawable) d).spr;
		if(spr == null) {throw new Loading();}
		spc = spr.getClass();
		result = kinds.contains(spc.getSimpleName()) || kinds.contains(spc.getSuperclass().getSimpleName());
	    }
	}
	return result;
    }

    @Override
    public String toString() {
	Resource res = gob.getres();
	return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }
}