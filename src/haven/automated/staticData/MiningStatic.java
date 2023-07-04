package haven.automated.staticData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MiningStatic {
    public static final Set<String> ROCKS = new HashSet<>(Arrays.asList(
            "alabaster", "apatite", "arkose", "basalt", "blackcoal", "breccia", "cassiterite", "chalcopyrite", "chert",
            "cinnabar", "cuprite", "diabase", "diorite", "dolomite", "eclogite", "feldspar", "fluorospar", "flint", "galena", "gabbro", "gneiss", "granite", "graywacke",
            "greenschist", "hornblende", "ilmenite", "jasper", "korund", "kyanite", "limestone", "malachite", "marble",
            "mica", "microlite", "olivine", "orthoclase", "peacockore", "pegmatite", "porphyry", "pumice", "quartz",
            "rhyolite", "sandstone", "schist", "serpentine", "slate", "sodalite", "soapstone", "stone", "sunstone", "wineglance", "zincspar"
    ));

    public static final Set<String> MINE_WALKABLE_TILES = new HashSet<>(Arrays.asList(
            "mine", "gleamgrotto", "wildcavern", "warmdepth", "gloomdark"
    ));
}
