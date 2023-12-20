/* Preprocessed source code */
package haven.res.ui.tt.q.quality;

/* $use: ui/tt/q/qbuff */
import haven.*;
import haven.res.ui.tt.q.qbuff.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/* >tt: Quality */
@haven.FromResource(name = "ui/tt/q/quality", version = 26)
public class Quality extends QBuff implements GItem.OverlayInfo<Tex> {
    public static final BufferedImage qualityWorkaround = Resource.remote().loadwait("ui/tt/q/quality").layer(Resource.imgc, 0).scaled();
    public static Color BG = new Color(0, 0, 0, 200);
    public Quality(Owner owner, double q) {
	//super(owner, Resource.classres(Quality.class).layer(Resource.imgc, 0).scaled(), "Quality", q);
        super(owner, qualityWorkaround, "Quality", q); //ND: workaround suggested by loftar
    }
    public Quality(Owner owner, double q, boolean goFuckYourself) {
        //super(owner, Resource.classres(Quality.class).layer(Resource.imgc, 0).scaled(), "Quality", q);
        super(owner, qualityWorkaround, "Quality", q); //ND: workaround suggested by loftar
        if (goFuckYourself){
            qtex = overlay();
        }
    }


    public static ItemInfo mkinfo(Owner owner, Object... args) {
	return(new Quality(owner, ((Number)args[1]).doubleValue()));
    }

    public boolean equals(double quality) {
        final double TOLERANCE = 1;
        return Math.abs(this.q - quality) < TOLERANCE;
    }

    private static final String[] EMPTY_INDICATOR = { // ND: Only show the quality as "Empty" for these specific containers
            "Birchbark Kuksa", "Bucket", "Waterskin", "Waterflask", "Glass Jug",
    };

    // TODO: Use a linked hashmap for content name and color, instead of hardcoding it.
    // ND: I just don't know what other liquids should have unique colors. For now just hardcoded water and tea lul

    public Tex overlay() {
        boolean irrelevantQuality = false; // ND: Use this to show the quality of some containers quality as "Empty", rather than their actual quality.
        Color qualityColor;
        if(OptWnd.enableCustomQualityColors.a){
            qualityColor = findHighestTextEntryValueLessThanQ(q);
        } else {
            qualityColor = new Color(255,255,255,255);
        }
        try {
            for (ItemInfo info : owner.info()) {
                if (info instanceof Name) {
                    if (Arrays.stream(EMPTY_INDICATOR).anyMatch(((Name) info).str.text::contains)) {
                        irrelevantQuality = true;
                    }
                }
                if (info instanceof Contents) {
                    for (ItemInfo info2 : ((Contents) info).sub) {
                        if (info2 instanceof QBuff) {
                            if ((((Contents) info).content != null) && (((Contents) info).content.name != null)) {
                                if (((Contents) info).content.name.equals("Water")) {
                                    if (OptWnd.roundedQualityCheckBox.a)
                                        return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(54, 175, 255, 255), true)))))
                                                : (new TexI(GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(54, 175, 255, 255), true)));
                                    else
                                        return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(54, 175, 255, 255), true)))))
                                                : (new TexI(GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(54, 175, 255, 255), true)));
                                } else if (((Contents) info).content.name.equals("Tea")) {
                                    if (OptWnd.roundedQualityCheckBox.a)
                                        return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(83, 161, 0, 255), true)))))
                                                : (new TexI(GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(83, 161, 0, 255), true)));
                                    else
                                        return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(83, 161, 0, 255), true)))))
                                                : (new TexI(GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(83, 161, 0, 255), true)));
                                }
                            }
                            if (OptWnd.roundedQualityCheckBox.a)
                                return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(255, 255, 255, 255), true)))))
                                        : (new TexI(GItem.NumberInfo.numrenderStroked((int) Math.round(((QBuff) info2).q), new Color(255, 255, 255, 255), true)));
                            else
                                return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(255, 255, 255, 255), true)))))
                                        : (new TexI(GItem.NumberInfo.numrenderStrokedDecimal(((QBuff) info2).q, new Color(255, 255, 255, 255), true)));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!irrelevantQuality) {
            if (OptWnd.roundedQualityCheckBox.a)
                return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStroked((int) Math.round(q), qualityColor, true))))) : (new TexI(GItem.NumberInfo.numrenderStroked((int) Math.round(q), qualityColor, true)));
            else
                return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.numrenderStrokedDecimal(q, qualityColor, true))))) : (new TexI(GItem.NumberInfo.numrenderStrokedDecimal(q, qualityColor, true)));
        } else {
            return OptWnd.showQualityBackgroundCheckBox.a ? (new TexI(ItemInfo.catimgsh(0, 0, BG, (GItem.NumberInfo.textrenderStroked("Empty", new Color(255, 78, 0, 255), true))))) : (new TexI(GItem.NumberInfo.textrenderStroked("Empty", new Color(255, 78, 0, 255), true)));
        }
    }

    public Color findHighestTextEntryValueLessThanQ(double q) {
        TextEntry[] textEntries = {OptWnd.firstQualityColorTextEntry, OptWnd.secondQualityColorTextEntry, OptWnd.thirdQualityColorTextEntry, OptWnd.fourthQualityColorTextEntry, OptWnd.fifthQualityColorTextEntry, OptWnd.sixthQualityColorTextEntry, OptWnd.seventhQualityColorTextEntry};
        int highestValue = Integer.MIN_VALUE;
        int indexOfHighest = -1;

        for (int i = 0; i < textEntries.length; i++) {
            try {
                int value = Integer.parseInt(textEntries[i].text());
                if (value < q && value > highestValue) {
                    highestValue = value;
                    indexOfHighest = i;
                }
            } catch (NumberFormatException ignored) {}
        }

        return switch (indexOfHighest) {
            case 0 ->
                    OptWnd.firstQualityColorOptionWidget.currentColor;
            case 1 ->
                    OptWnd.secondQualityColorOptionWidget.currentColor;
            case 2 ->
                    OptWnd.thirdQualityColorOptionWidget.currentColor;
            case 3 ->
                    OptWnd.fourthQualityColorOptionWidget.currentColor;
            case 4 ->
                    OptWnd.fifthQualityColorOptionWidget.currentColor;
            case 5 ->
                    OptWnd.sixthQualityColorOptionWidget.currentColor;
            case 6 ->
                    OptWnd.seventhQualityColorOptionWidget.currentColor;
            default -> new Color(255, 255, 255, 255);
        };
    }

    public void drawoverlay(GOut g, Tex ol) {
        if (OptWnd.toggleQualityDisplayCheckBox.a)
            g.aimage(ol, new Coord(g.sz().x, 0), 1, 0.2);
    }
}
