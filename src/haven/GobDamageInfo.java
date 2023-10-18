package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GobDamageInfo extends GobInfo {
    private static final int SHP = 61455;
    private static final int HHP = 64527;
    private static final int ARM = 36751;
    private static final int PAD = UI.scale(3);

    public static Color BG = new Color(0, 0, 0, 0);
    private static final Color SHP_C = Utils.col16(SHP);
    private static final Color HHP_C = Utils.col16(HHP);
    private static final Color ARM_C = Utils.col16(ARM);
    public static void setDamageBackgroundColor(boolean enableBackground){
        if (enableBackground) BG = new Color(0, 0, 0, 80);
        else BG = new Color(0, 0, 0, 0);
    }

    private static final Map<Long, DamageVO> gobDamage = new LinkedHashMap<Long, DamageVO>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 50;
        }
    };

    private final DamageVO damage;

    public GobDamageInfo(Gob owner) {
        super(owner);
        up(15); // ND: Default was 12.0 // ND: For each 3.4 added here, add 1.0 at "b:" in the pair below. It's probably not 100% correct, but it's super close.
        center = new Pair<>(0.5, 1.0); // Default was 0.5, 1.0
        if(gobDamage.containsKey(gob.id)) {
            damage = gobDamage.get(gob.id);
        } else {
            damage = new DamageVO();
            gobDamage.put(gob.id, damage);
        }
    }

    @Override
    protected boolean enabled() {
        return OptWnd.toggleGobDamageInfoCheckBox.a;
    }

    @Override
    protected Tex render() {
        if(damage.isEmpty()) {return null;}

        BufferedImage hhp = null, shp = null, arm = null;
        if (damage.shp >= 0) { // ND: Show 0 hp damage in case the user disables armor damage. If they deal only armor damage, the image will be null, and client will crash
            hhp = Text.std.renderstroked(String.format("%d", damage.shp), SHP_C, Color.BLACK).img;
        }
        if (OptWnd.toggleGobDamageWoundInfoCheckBox.a) {
            if (damage.hhp > 0) {
                shp = Text.std.renderstroked(String.format("%d", damage.hhp), HHP_C, Color.BLACK).img;
            }
        }
        if (OptWnd.toggleGobDamageArmorInfoCheckBox.a) {
            if (damage.armor > 0) {
                arm = Text.std.renderstroked(String.format("%d", damage.armor), ARM_C, Color.BLACK).img;
            }
        }
        return new TexI(Utils.outline2(ItemInfo.catimgsh(PAD, PAD, BG, hhp, shp, arm), Color.BLACK, true));
    }

    public void update(int c, int v) {
//	Debug.log.println(String.format("Number %d, c: %d", v, c));
        //35071 - Initiative
        if(c == SHP) {
            damage.shp += v;
            update();
        } else if(c == HHP) {
            damage.hhp += v;
            update();
        } else if(c == ARM) {
            damage.armor += v;
            update();
        }
    }

    public void update() {
        gobDamage.put(gob.id, damage);
        clean();
    }

    public static boolean has(Gob gob) {
        return gobDamage.containsKey(gob.id);
    }

    private static void clearDamage(Gob gob, long id) {
        if(gob != null) {
            gob.clearDmg();
        }
        gobDamage.remove(id);
    }

    public static void clearPlayerDamage(GameUI gui) {
        clearDamage(gui.ui.sess.glob.oc.getgob(gui.plid), gui.plid);
    }

    public static void clearAllDamage(GameUI gui) {
        try {
            ArrayList<Long> gobIds = new ArrayList<>(gobDamage.keySet());
            for (Long id : gobIds) {
                if (id == null) {
                    continue;
                }
                clearDamage(gui.ui.sess.glob.oc.getgob(id), id);
            }
        } catch (ArrayIndexOutOfBoundsException ignored){}
    }

    private static class DamageVO {
        int shp = 0, hhp = 0, armor = 0;

        boolean isEmpty() {return shp == 0 && hhp == 0 && armor == 0;}

    }
}
