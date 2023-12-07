package haven.automated.helpers;

import java.util.*;

public class TileStatic {
    public static final Set<String> SUPPORT_MATERIALS = new HashSet<>(Arrays.asList(
            "alabaster", "apatite", "arkose", "basalt", "blackcoal", "breccia", "cassiterite", "chalcopyrite", "chert",
            "cinnabar", "cuprite", "diabase", "diorite", "dolomite", "eclogite", "feldspar", "fluorospar", "flint", "galena", "gabbro", "gneiss", "granite", "graywacke",
            "greenschist", "hornblende", "ilmenite", "jasper", "korund", "kyanite", "limestone", "malachite", "marble",
            "mica", "microlite", "olivine", "orthoclase", "peacockore", "pegmatite", "porphyry", "pumice", "quartz",
            "rhyolite", "sandstone", "schist", "slate", "serpentine", "slate", "sodalite", "soapstone", "stone", "sunstone", "wineglance", "zincspar"
    ));

    public static final Set<String> MINE_WALKABLE_TILES = new HashSet<>(Arrays.asList(
            "mine", "gleamgrotto", "wildcavern", "warmdepth", "gloomdark"
    ));

    public static final Set<String> ORE = new HashSet<>(Arrays.asList(
            "gfx/tiles/rocks/argentite", "gfx/tiles/rocks/blackcoal", "gfx/tiles/rocks/cassiterite", "gfx/tiles/rocks/chalcopyrite", "gfx/tiles/rocks/cinnabar",
            "gfx/tiles/rocks/cuprite", "gfx/tiles/rocks/galena", "gfx/tiles/rocks/hematite", "gfx/tiles/rocks/hornsilver", "gfx/tiles/rocks/ilmenite",
            "gfx/tiles/rocks/leadglance", "gfx/tiles/rocks/limonite", "gfx/tiles/rocks/magnetite", "gfx/tiles/rocks/malachite", "gfx/tiles/rocks/nagyagite",
            "gfx/tiles/rocks/petzite", "gfx/tiles/rocks/sylvanite"
    ));

    public static final Set<String> ROCKS = new HashSet<>(Arrays.asList(
            "gfx/tiles/rocks/alabaster", "gfx/tiles/rocks/apatite", "gfx/tiles/rocks/arkose", "gfx/tiles/rocks/basalt", "gfx/tiles/rocks/breccia",
            "gfx/tiles/rocks/chert", "gfx/tiles/rocks/diabase", "gfx/tiles/rocks/diorite", "gfx/tiles/rocks/dolomite", "gfx/tiles/rocks/eclogite",
            "gfx/tiles/rocks/feldspar", "gfx/tiles/rocks/flint", "gfx/tiles/rocks/fluorospar", "gfx/tiles/rocks/gabbro", "gfx/tiles/rocks/gneiss",
            "gfx/tiles/rocks/granite", "gfx/tiles/rocks/graywacke", "gfx/tiles/rocks/greenschist", "gfx/tiles/rocks/hornblende", "gfx/tiles/rocks/jasper",
            "gfx/tiles/rocks/korund", "gfx/tiles/rocks/limestone", "gfx/tiles/rocks/marble", "gfx/tiles/rocks/mica", "gfx/tiles/rocks/microlite",
            "gfx/tiles/rocks/olivine", "gfx/tiles/rocks/orthoclase", "gfx/tiles/rocks/pegmatite", "gfx/tiles/rocks/porphyry", "gfx/tiles/rocks/pumice",
            "gfx/tiles/rocks/quartz", "gfx/tiles/rocks/rhyolite", "gfx/tiles/rocks/sandstone", "gfx/tiles/rocks/schist", "gfx/tiles/rocks/serpentine",
            "gfx/tiles/rocks/slate", "gfx/tiles/rocks/soapstone", "gfx/tiles/rocks/sodalite", "gfx/tiles/rocks/sunstone", "gfx/tiles/rocks/zincspar"
    ));

    public static final Map<String, String> ORE_NAMES = new HashMap<>();

    static {
        ORE_NAMES.put("argentite", "silvershine");
        ORE_NAMES.put("blackcoal", "coal");
        ORE_NAMES.put("cuprite", "wine glance");
        ORE_NAMES.put("cassiterite", "cassiterite");
        ORE_NAMES.put("chalcopyrite", "chalcopyrite");
        ORE_NAMES.put("hematite", "bloodstone");
        ORE_NAMES.put("hornsilver", "horn silver");
        ORE_NAMES.put("ilmenite", "heavy earth");
        ORE_NAMES.put("leadglance", "lead glance");
        ORE_NAMES.put("limonite", "iron ochre");
        ORE_NAMES.put("malachite", "malachite");
        ORE_NAMES.put("magnetite", "black ore");
        ORE_NAMES.put("nagyagite", "leaf ore");
        ORE_NAMES.put("peacockore", "peacockore");
        ORE_NAMES.put("petzite", "direvein");
        ORE_NAMES.put("sylvanite", "schrifterz");
        ORE_NAMES.put("galena", "galena");
    }






}

