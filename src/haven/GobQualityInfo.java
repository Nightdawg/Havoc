package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GobQualityInfo extends GobInfo {
	public static boolean showGobQualityInfo = Utils.getprefb("showGobQualityInfo", true);
    private static final Color Q_COL = new Color(235, 252, 255, 255);
    private static final Color BG = new Color(0, 0, 0, 0);
    public static Pattern GOB_Q = Pattern.compile("Quality: (\\d+)");
    private static final Map<Long, Integer> gobQ = new LinkedHashMap<Long, Integer>() {
	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
	    return size() > 50;
	}
    };
    int q;

    protected GobQualityInfo(Gob owner) {
	super(owner);
	q = gobQ.getOrDefault(gob.id, 0);
    }
    
    
    public void setQ(int q) {
	gobQ.put(gob.id, q);
	this.q = q;
    }
    
    @Override
	protected boolean enabled() {
		return showGobQualityInfo;
	}

    @Override
    protected Tex render() {
	if(gob == null || gob.getres() == null) { return null;}

	BufferedImage quality = quality();

	if(quality == null) {
	    return null;
	}

	return new TexI(ItemInfo.catimgsh(3, 0, BG, quality));
    }
    
    @Override
    public void dispose() {
	super.dispose();
    }

    private BufferedImage quality() {
	if(q != 0) {
	    return Text.std.renderstroked(String.format("Q: %d", q), Q_COL, Color.BLACK).img;
	}
	return null;
    }

    @Override
    public String toString() {
	Resource res = gob.getres();
	return String.format("GobInfo<%s>", res != null ? res.name : "<loading>");
    }
}