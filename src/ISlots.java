/* Preprocessed source code */
/* $use: lib/tspec */

import haven.*;
import haven.res.lib.tspec.Spec;
import haven.res.ui.tt.attrmod.AttrMod;

import static haven.PUtils.*;
import java.awt.image.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

/* >tt: Fac */
@haven.FromResource(name = "ui/tt/slots", version = 28)
public class ISlots extends ItemInfo.Tip implements GItem.NumberInfo {
	public static final Text ch = Text.render("Gildings:");
	public static final Text.Foundry progf = new Text.Foundry(Text.dfont.deriveFont(Font.ITALIC), 10, new Color(0, 169, 224));
	public final Collection<SItem> s = new ArrayList<SItem>();
	public final int left;
	public final double pmin, pmax;
	public final Resource[] attrs;
	private UI ui = null;

	public ISlots(Owner owner, int left, double pmin, double pmax, Resource[] attrs) {
		super(owner);
		this.left = left;
		this.pmin = pmin;
		this.pmax = pmax;
		this.attrs = attrs;
		if (owner instanceof GItem)
			this.ui = ((GItem) owner).ui;
	}

	public static final String chc = "192,192,255";
	public void layout(Layout l) {
		boolean extendedView = ui != null && ui.modshift;
		l.cmp.add(ch.img, new Coord(0, l.cmp.sz.y));

		if(attrs.length > 0) {
			BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%} to $col[%s]{%d%%}", chc, Math.round(100 * pmin), chc, Math.round(100 * pmax)), 0).img;
			int h = head.getHeight();
			int x = 10, y = l.cmp.sz.y;
			l.cmp.add(head, new Coord(x, y));
			x += head.getWidth() + 10;
			for(int i = 0; i < attrs.length; i++) {
				BufferedImage icon = convolvedown(attrs[i].layer(Resource.imgc).img, new Coord(h, h), CharWnd.iconfilter);
				l.cmp.add(icon, new Coord(x, y));
				x += icon.getWidth() + 2;
			}
		} else {
			BufferedImage head = RichText.render(String.format("Chance: $col[%s]{%d%%}", chc, (int)Math.round(100 * pmin)), 0).img;
			l.cmp.add(head, new Coord(10, l.cmp.sz.y));
		}
		Map<Resource, Integer> totalAttr = new HashMap<>();
		for(SItem si : s) {
			if (extendedView)
				si.layout(l);
			for (ItemInfo ii : si.info) {
				if (ii instanceof AttrMod) {
					for (AttrMod.Mod mod : ((AttrMod) ii).mods) {
						boolean exist = false;
						for (Map.Entry<Resource, Integer> entry : totalAttr.entrySet()) {
							if (entry.getKey().equals(mod.attr)) {
								exist = true;
								entry.setValue(entry.getValue() + mod.mod);
								break;
							}
						}
						if (!exist)
							totalAttr.put(mod.attr, mod.mod);
					}
				}
			}
		}
		if (!extendedView)
			if (totalAttr.size() > 0) {
				List<AttrMod.Mod> lmods = new ArrayList<>();
				List<Map.Entry<Resource, Integer>> sortAttr = totalAttr.entrySet().stream().sorted(this::BY_PRIORITY).collect(Collectors.toList());
				for (Map.Entry<Resource, Integer> entry : sortAttr) {
					lmods.add(new AttrMod.Mod(entry.getKey(), entry.getValue()));
				}
				l.cmp.add(AttrMod.modimg(lmods), new Coord(10, l.cmp.sz.y));
			}

		if(left > 0)
			l.cmp.add(progf.render((left > 1)?String.format("Gildable \u00d7%d", left):"Gildable").img, new Coord(10, l.cmp.sz.y));
		if (ui != null)
			l.cmp.add(RichText.render(extendedView ? "$col[218,163,0]{<Showing Each Individual Gilding>}" : "$col[185,185,185]{<Hold Shift for Individual Gildings>}", 0).img, new Coord(0, l.cmp.sz.y));
	}

	private int BY_PRIORITY(Map.Entry<Resource, Integer> o1, Map.Entry<Resource, Integer> o2) {
		Resource r1 = o1.getKey();
		Resource r2 = o2.getKey();
		return Integer.compare(Config.statsAndAttributesOrder.indexOf(r2.layer(Resource.tooltip).t), Config.statsAndAttributesOrder.indexOf(r1.layer(Resource.tooltip).t));
	}

	public static final Object[] defn = {Loading.waitfor(Resource.classres(ISlots.class).pool.load("ui/tt/defn", 6))};
	public class SItem {
		public final Resource res;
		public final GSprite spr;
		public final List<ItemInfo> info;
		public final String name;

		public SItem(ResData sdt, Object[] raw) {
			this.res = sdt.res.get();
			Spec spec1 = new Spec(sdt, owner, Utils.extend(new Object[] {defn}, raw));
			this.spr = spec1.spr();
			this.name = spec1.name();
			Spec spec2 = new Spec(sdt, owner, raw);
			this.info = spec2.info();
		}

		private BufferedImage img() {
			if(spr instanceof GSprite.ImageSprite)
				return(((GSprite.ImageSprite)spr).image());
			return(res.layer(Resource.imgc).img);
		}

		public void layout(Layout l) {
			BufferedImage icon = PUtils.convolvedown(img(), new Coord(16, 16), CharWnd.iconfilter);
			BufferedImage lbl = Text.render(name).img;
			BufferedImage sub = longtip(info);
			int x = 10, y = l.cmp.sz.y;
			l.cmp.add(icon, new Coord(x, y));
			l.cmp.add(lbl, new Coord(x + 16 + 3, y + ((16 - lbl.getHeight()) / 2)));
			if(sub != null)
				l.cmp.add(sub, new Coord(x + 16, y + 16));
		}
	}

	public int order() {
		return(200);
	}

	public int itemnum() {
		return(s.size());
	}

	public static final Color avail = new Color(128, 192, 255);
	public Color numcolor() {
		return((left > 0) ? avail : Color.WHITE);
	}

	public static BufferedImage longtip(List<ItemInfo> info) { // ND: Added this here to overwrite method from ItemInfo and prevent an extra text stroke on contents tooltip
		Layout l = new Layout();
		for(ItemInfo ii : info) {
			if(ii instanceof Tip) {
				Tip tip = (Tip)ii;
				l.add(tip);
			}
		}
		if(l.tips.size() < 1)
			return(null);
		return(l.render());
	}
}
