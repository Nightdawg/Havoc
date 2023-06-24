/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

import haven.ItemInfo.AttrCache;
import haven.resutil.Curiosity;

import static haven.Inventory.sqsz;

public class WItem extends Widget implements DTarget {
    public static final Resource missing = Resource.local().loadwait("gfx/invobjs/missing");
    public final GItem item;
    private Resource cspr = null;
    private Message csdt = Message.nil;

    public WItem(GItem item) {
	super(sqsz);
	this.item = item;
    }

    public void drawmain(GOut g, GSprite spr) {
	spr.draw(g);
    }

    public class ItemTip implements Indir<Tex>, ItemInfo.InfoTip {
	private final List<ItemInfo> info;
	private final TexI tex;

	public ItemTip(List<ItemInfo> info, BufferedImage img) {
	    this.info = info;
	    if(img == null)
		throw(new Loading());
	    tex = new TexI(img);
	}

	public GItem item() {return(item);}
	public List<ItemInfo> info() {return(info);}
	public Tex get() {return(tex);}
    }

    public class ShortTip extends ItemTip {
	public ShortTip(List<ItemInfo> info) {super(info, ItemInfo.shorttip(info));}
    }

    public class LongTip extends ItemTip {
	public LongTip(List<ItemInfo> info) {super(info, ItemInfo.longtip(info));}
    }

    private double hoverstart; //ND: Skip this crap
    private ItemTip shorttip = null, longtip = null;
    private List<ItemInfo> ttinfo = null;
    public Object tooltip(Coord c, Widget prev) {
	double now = Utils.rtime();
	if(prev == this) {
	} else if(prev instanceof WItem) {
	    double ps = ((WItem)prev).hoverstart;
	    if(now - ps < 1.0)
		hoverstart = now;
	    else
		hoverstart = ps;
	} else {
	    hoverstart = now;
	}
	try {
	    List<ItemInfo> info = item.info();
	    if(info.size() < 1)
		return(null);
	    if(info != ttinfo) {
		shorttip = longtip = null;
		ttinfo = info;
	    }
//	    if(now - hoverstart < 1.0) {
//		if(shorttip == null)
//		    shorttip = new ShortTip(info);
//		return(shorttip);
//	    } else {
		if(longtip == null)
		    longtip = new LongTip(info);
		return(longtip);
//	    }
	} catch(Loading e) {
	    return("...");
	}
    }

    private List<ItemInfo> info() {return(item.info());}
    public final AttrCache<Color> olcol = new AttrCache<>(this::info, info -> {
	    ArrayList<GItem.ColorInfo> ols = new ArrayList<>();
	    for(ItemInfo inf : info) {
		if(inf instanceof GItem.ColorInfo)
		    ols.add((GItem.ColorInfo)inf);
	    }
	    if(ols.size() == 0)
		return(() -> null);
	    if(ols.size() == 1)
		return(ols.get(0)::olcol);
	    ols.trimToSize();
	    return(() -> {
		    Color ret = null;
		    for(GItem.ColorInfo ci : ols) {
			Color c = ci.olcol();
			if(c != null)
			    ret = (ret == null) ? c : Utils.preblend(ret, c);
		    }
		    return(ret);
		});
	});
    public final AttrCache<GItem.InfoOverlay<?>[]> itemols = new AttrCache<>(this::info, info -> {
	    ArrayList<GItem.InfoOverlay<?>> buf = new ArrayList<>();
	    for(ItemInfo inf : info) {
		if(inf instanceof GItem.OverlayInfo)
		    buf.add(GItem.InfoOverlay.create((GItem.OverlayInfo<?>)inf));
	    }
	    GItem.InfoOverlay<?>[] ret = buf.toArray(new GItem.InfoOverlay<?>[0]);
	    return(() -> ret);
	});
    public final AttrCache<Double> itemmeter = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));

    private Widget contparent() {
	/* XXX: This is a bit weird, but I'm not sure what the alternative is... */
	Widget cont = getparent(GameUI.class);
	return((cont == null) ? cont = ui.root : cont);
    }

    private GSprite lspr = null;
    private Widget lcont = null;
    public void tick(double dt) {
	/* XXX: This is ugly and there should be a better way to
	 * ensure the resizing happens as it should, but I can't think
	 * of one yet. */
	GSprite spr = item.spr();
	if((spr != null) && (spr != lspr)) {
	    Coord sz = new Coord(spr.sz());
	    if((sz.x % sqsz.x) != 0)
		sz.x = sqsz.x * ((sz.x / sqsz.x) + 1);
	    if((sz.y % sqsz.y) != 0)
		sz.y = sqsz.y * ((sz.y / sqsz.y) + 1);
	    resize(sz);
	    lspr = spr;
	}
    }

    public void draw(GOut g) {
	GSprite spr = item.spr();
	if(spr != null) {
	    Coord sz = spr.sz();
	    g.defstate();
	    if(olcol.get() != null)
		g.usestate(new ColorMask(olcol.get()));
	    drawmain(g, spr);
	    g.defstate();
	    GItem.InfoOverlay<?>[] ols = itemols.get();
	    if(ols != null) {
		for(GItem.InfoOverlay<?> ol : ols)
		    ol.draw(g);
	    }
		drawmeter(g, sz);
	} else {
	    g.image(missing.layer(Resource.imgc).tex(), Coord.z, sz);
	}
    }
	private void drawmeter(GOut g, Coord sz) {
		double meter = meter();
		if(meter > 0) {
			Tex studyTime = getStudyTime();
			if(studyTime == null) {
				Tex tex = Text.renderstroked(String.format("%d%%", Math.round(100 * meter))).tex();
				g.aimage(tex, sz.div(2), 0.5, 0.5);
				tex.dispose();
			}
			// ND: This following commented code is the curio circle overlay. I removed it and added the actual time at the bottom.
//			g.chcolor(255, 255, 255, 64);
//			Coord half = sz.div(2);
//			g.prect(half, half.inv(), half, meter * Math.PI * 2);
//			g.chcolor();
			if(studyTime != null) {
				g.chcolor(0, 0, 0, 150);
				int h = studyTime.sz().y;
				g.frect(new Coord(0, sz.y - h+4), new Coord(sz.x+2, h));
				g.chcolor();
				g.aimage(studyTime, new Coord(sz.x / 2, sz.y), 0.5, 0.9);
			}
		}
	}

	public double meter() {
		Double meter = (item.meter > 0) ? (Double) (item.meter / 100.0) : itemmeter.get();
		return meter == null ? 0 : meter;
	}

	private String cachedStudyValue = null;
	private String cachedTipValue = null;
	private Tex cachedStudyTex = null;
	private Tex getStudyTime() {
		Pair<String, String> data = study.get();
		String value = data == null ? null : data.a;
		String tip = data == null ? null : data.b;
		if(!Objects.equals(tip, cachedTipValue)) {
			cachedTipValue = tip;
			longtip = null;
		}
		if(value != null) {
			if(!Objects.equals(value, cachedStudyValue)) {
				if(cachedStudyTex != null) {
					cachedStudyTex.dispose();
					cachedStudyTex = null;
				}
			}

			if(cachedStudyTex == null) {
				cachedStudyValue = value;
				cachedStudyTex = Text.renderstroked(value).tex();
			}
			return cachedStudyTex;
		}
		return null;
	}

	public final AttrCache<Pair<String, String>> study = new AttrCache<Pair<String, String>>(this::info, AttrCache.map1(Curiosity.class, curio -> curio::remainingTip));

    public boolean mousedown(Coord c, int btn) {
	boolean inv = parent instanceof Inventory;
	if(btn == 1) {
		if (ui.modmeta && !ui.modctrl) {
			if (inv) {
				wdgmsg("transfer-identical", item, false);
				return true;
			}
		}
	    if (ui.modshift) {
			item.wdgmsg("transfer", c, 1);
			return(true);
	    } else if (ui.modctrl) {
			int n = ui.modmeta ? -1 : 1;
			item.wdgmsg("drop", c, n);
			return(true);
	    } else {
			item.wdgmsg("take", c);
	    }
	    return(true);
	} else if(btn == 3) {
		// TODO: Make this a script Action Button instead
//		if(OptWnd.massSplitCtrlShiftAlt && ui.modctrl && ui.modmeta && ui.modshift){
//			String name = item.getname();
//			if(name.contains("Block of") || name.equals("Pumpkin") || name.contains("gfx/invobjs/fish-")){
//				if(name.contains("Block of")){
//					name = "Block of";
//				} else if (name.contains("gfx/invobjs/fish-")){
//					name = "gfx/invobjs/fish-";
//				}
//				String finalName = name;
//				List<WItem> wItems = gameui().getAllInventories().stream()
//						.flatMap(inventory -> inventory.getItemsPartial(finalName).stream())
//						.collect(Collectors.toList());
//				int last = ui.lastid;
//				for(WItem wItem: wItems){
//					wItem.item.wdgmsg("iact", c, 0);
//					ui.rcvr.rcvmsg((last+1), "cl", 0, 0);
//					last += 6;
//				}
//			}
//		}
		if (ui.modmeta && !ui.modctrl) {
			if (inv) {
				wdgmsg("transfer-identical", item, true);
				return true;
			}
		}
		//System.out.println(item.getname());
		if (ui.modctrl && OptWnd.instantFlowerMenuCTRL) {
			String itemname = item.getname();
			int option = 0;
			if (itemname.toLowerCase().contains("lettuce")) {
				option = 1;
			}
			item.wdgmsg("iact", c, ui.modflags());
			ui.rcvr.rcvmsg(ui.lastid+1, "cl", option, 0);
		} else {
			item.wdgmsg("iact", c, ui.modflags());
		}
		return(true);
	}
	return(false);
    }

    public boolean drop(Coord cc, Coord ul) {
	return(false);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	item.wdgmsg("itemact", ui.modflags());
	return(true);
    }

    public boolean mousehover(Coord c, boolean on) {
	boolean ret = super.mousehover(c, on);
	if(on && (item.contents != null)) {
	    item.hovering(this);
	    return(true);
	}
	return(ret);
    }
}
