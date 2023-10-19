package haven;

import haven.res.ui.tt.armor.Armor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CustomMapIcons {

    private static final Map<String, String> gob2icon = new HashMap<String, String>(){{
		put("gfx/terobjs/vehicle/knarr", "customMapIcons/knarr");
		put("gfx/terobjs/vehicle/snekkja", "customMapIcons/snekkja");
		put("gfx/terobjs/vehicle/rowboat", "customMapIcons/rowboat");
		put("gfx/terobjs/vehicle/dugout", "customMapIcons/dugout");
		put("gfx/terobjs/vehicle/coracle", "customMapIcons/coracle");
		put("gfx/terobjs/vehicle/spark", "customMapIcons/kicksled");
		put("gfx/terobjs/vehicle/skis-wilderness", "customMapIcons/skis");
		put("gfx/terobjs/vehicle/wagon", "customMapIcons/wagon");

		put("gfx/terobjs/vehicle/wheelbarrow", "customMapIcons/wheelbarrow");
		put("gfx/terobjs/vehicle/cart", "customMapIcons/cart");
		put("gfx/terobjs/vehicle/plow", "customMapIcons/woodenplow");
		put("gfx/terobjs/vehicle/metalplow", "customMapIcons/metalplow");

		put("gfx/kritter/horse/stallion", "customMapIcons/tamedHorse");
		put("gfx/kritter/horse/mare", "customMapIcons/tamedHorse");

		put("gfx/terobjs/pclaim", "customMapIcons/pclaim");
		put("gfx/terobjs/villageidol", "customMapIcons/vclaim");

		put("gfx/terobjs/burrow", "customMapIcons/burrow");
		put("gfx/terobjs/minehole", "customMapIcons/minehole");
		put("gfx/terobjs/ladder", "customMapIcons/mineladder");
		put("gfx/terobjs/wonders/wellspring", "customMapIcons/wellspring");

		put("gfx/terobjs/items/mandrakespirited", "customMapIcons/mandrakespirited");
		put("gfx/kritter/opiumdragon/opiumdragon", "customMapIcons/opiumdragon");
		put("gfx/kritter/stalagoomba/stalagoomba", "customMapIcons/stalagoomba");
		put("gfx/kritter/dryad/dryad", "customMapIcons/dryad");
		put("gfx/kritter/ent/ent", "customMapIcons/treant");

		put("gfx/terobjs/vehicle/bram", "customMapIcons/bram");
		put("gfx/terobjs/vehicle/catapult", "customMapIcons/catapult");
		put("gfx/terobjs/vehicle/wreckingball", "customMapIcons/wreckingball");

		put("gfx/terobjs/trees/oldtrunk", "customMapIcons/mirkwoodlog");

		put ("gfx/kritter/midgeswarm/midgeswarm", "customMapIcons/midgeswarm");

		put("gfx/terobjs/map/cavepuddle", "customMapIcons/caveclaypuddle");

		put("gfx/terobjs/items/gems/gemstone", "customMapIcons/gem");
	}};
    
    public static boolean process(GobIcon icon) {
	try {
	    String gres = icon.gob.resid();
	    String ires = icon.res.get().name;
	    if(gres != null && ires != null) {
		if(!ires.equals(gob2icon.get(gres))) {
		    gob2icon.put(gres, ires);
		    //if(gres.contains("kritter")) Debug.log.printf("gob2icon.put(\"%s\", \"%s\");%n", gres, ires);
		}
		return true;
	    }
	} catch (Loading ignored) {}
	return false;
    }
    
    public static GobIcon getIcon(Gob gob) {
	String resname = gob2icon.get(gob.resid());
	if(resname != null) {
	    return new GobIcon(gob, Resource.remote().load(resname));
	}
	return null;
    }
    
    public static void addCustomSettings(Map<String, GobIcon.Setting> settings, UI ui) {
	gob2icon.forEach((key, value) -> {
		addSetting(settings, value, true);
	});
	ui.sess.glob.oc.gobAction(Gob::iconUpdated);
    }
    
    private static void addSetting(Map<String, GobIcon.Setting> settings, String res, boolean def) {
	if(!settings.containsKey(res)) {
	    GobIcon.Setting cfg = new GobIcon.Setting(new Resource.Spec(null, res));
	    cfg.show = cfg.defshow = def;
	    settings.put(res, cfg);
	}
    }

}
