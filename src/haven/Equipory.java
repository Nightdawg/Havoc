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
 *  MERCHANTABILITY or FITNESS FOR A ULAR PURPOSE.  See the
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

import haven.automated.DropItemsFromKnockedEnemy;
import haven.automated.YoinkGoodStuffFromKnockedEnemy;
import haven.res.ui.tt.armor.Armor;
import haven.res.ui.tt.wear.Wear;

import java.awt.*;
import java.util.*;

import static haven.Inventory.invsq;

public class Equipory extends Widget implements DTarget {
    private static final Tex bg = Resource.loadtex("gfx/hud/equip/bg");
    private static final int
	rx = invsq.sz().x + bg.sz().x,
	yo = Inventory.sqsz.y;
    public static final Coord bgc = new Coord(invsq.sz().x, 0);
	private static final Text.Foundry acf = new Text.Foundry(Text.sans, 12);
	public boolean updateBottomText = false;
	public final boolean player;
	long delayedUpdateTime;
	private Tex Detection = null;
	private Tex Subtlety = null;
	private Tex ArmorClass = null;
    public static final Coord ecoords[] = {
		new Coord( 0, 0 * yo),
		new Coord( 0, 1 * yo),
		new Coord( 0, 2 * yo),
		new Coord(rx, 2 * yo),
		new Coord( 0, 3 * yo),
		new Coord(rx, 3 * yo),
		new Coord( 0, 4 * yo),
		new Coord(rx, 4 * yo),
		new Coord( 0, 5 * yo),
		new Coord(rx, 5 * yo),
		new Coord( 0, 6 * yo),
		new Coord(rx, 6 * yo),
		new Coord( 0, 7 * yo),
		new Coord(rx, 7 * yo),
		new Coord( 0, 8 * yo),
		new Coord(rx, 8 * yo),
		new Coord(invsq.sz().x, 0 * yo),
		new Coord(rx, 0 * yo),
		new Coord(rx, 1 * yo),
    };
	public enum SLOTS {
		HEAD(0),       //00: Headgear
		ACCESSORY(1),  //01: Main Accessory
		SHIRT(2),      //02: Shirt
		ARMOR_BODY(3), //03: Torso Armor
		GLOVES(4),     //04: Gloves
		BELT(5),       //05: Belt
		HAND_LEFT(6),  //06: Left Hand
		HAND_RIGHT(7), //07: Right Hand
		RING_LEFT(8),  //08: Left Hand Ring
		RING_RIGHT(9), //09: Right Hand Ring
		ROBE(10),      //10: Cloaks & Robes
		BACK(11),      //11: Backpack
		PANTS(12),     //12: Pants
		ARMOR_LEG(13), //13: Leg Armor
		CAPE(14),      //14: Cape
		BOOTS(15),     //15: Shoes
		STORE_HAT(16), //16: Hat from store
		EYES(17),      //17: Eyes
		MOUTH(18);     //18: Mouth

		public final int idx;
		SLOTS(int idx) {
			this.idx = idx;
		}
	}
    public static final Tex[] ebgs = new Tex[ecoords.length];
    public static final Text[] etts = new Text[ecoords.length];
    static Coord isz;
    static {
		isz = new Coord();
		for(Coord ec : ecoords) {
			if(ec.x + invsq.sz().x > isz.x)
				isz.x = ec.x + invsq.sz().x;
			if(ec.y + invsq.sz().y > isz.y)
				isz.y = ec.y + invsq.sz().y;
		}
		for(int i = 0; i < ebgs.length; i++) {
			Resource bgres = Resource.local().loadwait("gfx/hud/equip/ep" + i);
			Resource.Image img = bgres.layer(Resource.imgc);
			if(img != null) {
			ebgs[i] = img.tex();
			etts[i] = Text.render(bgres.flayer(Resource.tooltip).t);
			}
		}
    }
	Map<GItem, WItem[]> wmap = new HashMap<GItem, WItem[]>();
    private final Avaview ava;
	AttrBonusesWdg bonuses;
	public WItem[] slots = new WItem[ecoords.length];
	private static final int btnw = UI.scale(80);
	boolean checkForLeeches = false;

    @RName("epry")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    long gobid;
	    if(args.length < 1)
		gobid = -2;
	    else if(args[0] == null)
		gobid = -1;
	    else
		gobid = Utils.uint32((Integer)args[0]);
		// line below change to != for testing on your own eq
		if (gobid == GameUI.playerId){
			return(new Equipory(gobid, true));
		} else {
			return(new Equipory(gobid));
		}
	}
    }

    protected void added() {
	if(ava.avagob == -2)
	    ava.avagob = getparent(GameUI.class).plid;
	if(parent instanceof Window) {
		boolean ignoredEquipory = "Mannequin".equals(((Window) parent).caption()) || "Wardrobe".equals(((Window) parent).caption()) || "Equipment".equals(((Window) parent).caption());
		if(!ignoredEquipory) {
			Equipory enemyEquipory = this;
			Button button = new Button(btnw, "Yoink") {
				@Override
				public void click() {
					new Thread(new YoinkGoodStuffFromKnockedEnemy(enemyEquipory, ui.gui), "DropItemsFromEnemy").start();
				}
			};
			button.c = UI.scale(74, 0);
			add(button);
			Button button2 = new Button(btnw, "Drop") {
				@Override
				public void click() {
					new Thread(new DropItemsFromKnockedEnemy(enemyEquipory, ui.gui), "DropItemsFromEnemy").start();
				}
			};

			button2.c = UI.scale(170, 0);
			add(button2);
		}
	}
	super.added();
    }


	public Equipory(long gobid) {
		super(isz);
		this.player = false;
		ava = add(new Avaview(bg.sz(), gobid, "equcam") {
			public boolean mousedown(Coord c, int button) {
				return (false);
			}
			public void draw(GOut g) {
				g.image(bg, Coord.z);
				super.draw(g);
			}
			{
				basic.add(new Outlines(false));
			}
			final FColor cc = new FColor(0, 0, 0, 0);
			protected FColor clearcolor() {
				return (cc);
			}
		}, bgc);
		bonuses = add(new AttrBonusesWdg(isz.y), isz.x + UI.scale(14), 0);
		pack();
	}

	public Equipory(long gobid, boolean player) {
		super(isz);
		this.player = player;
		ava = add(new Avaview(bg.sz(), gobid, "equcam") {
			public boolean mousedown(Coord c, int button) {
				return (false);
			}

			public void draw(GOut g) {
				g.image(bg, Coord.z);
				super.draw(g);
			}

			{
				basic.add(new Outlines(false));
			}

			final FColor cc = new FColor(0, 0, 0, 0);

			protected FColor clearcolor() {
				return (cc);
			}
		}, bgc);
		ava.drawv = false;
		bonuses = add(new AttrBonusesWdg(isz.y), isz.x + UI.scale(14), 0);
		add(autoDropLeechesCheckBox, isz.x + UI.scale(14), isz.y - UI.scale(24));
		pack();
	}

	public static CheckBox autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
		{a = Utils.getprefb("autoDropLeeches", false);}
		public void set(boolean val) {
			if (OptWnd.autoDropLeechesCheckBox != null)
				OptWnd.autoDropLeechesCheckBox.set(val);
			a = val;
		}
	};

    public static interface SlotInfo {
	public int slots();
    }

    public void addchild(Widget child, Object... args) {
		if(child instanceof GItem) {
			add(child);
			GItem g = (GItem)child;
			WItem[] v = new WItem[args.length];
			for (int i = 0; i < args.length; i++) {
				int ep = (Integer) args[i];
				v[i] = slots[ep] = add(new WItem(g), ecoords[ep].add(1, 1));
			}
			g.sendttupdate = true;
			wmap.put(g, v);
			delayedUpdateTime = System.currentTimeMillis();
			updateBottomText = true;
			checkForLeeches = true;
			try {
				if (g.resname().equals("gfx/invobjs/batcape")) {
					Gob.batsLeaveMeAlone = true;
					ui.sess.glob.oc.gobAction(Gob::toggleBeastDangerRadii);
				}
			} catch (Exception e){CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}
		} else {
			super.addchild(child, args);
		}
    }

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender instanceof GItem && wmap.containsKey(sender) && msg.equals("ttupdate")) {
			bonuses.update(slots);
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
		final WItem[] witms = wmap.remove(i);
		for (WItem v : witms) {
			ui.destroy(v);
			for (int s = 0; s < slots.length; ++s) {
				if (slots[s] == v)
					slots[s] = null;
			}
		}
		bonuses.update(slots);
		updateBottomText = true;
		checkForLeeches = true;
		try {
			if (i.resname().equals("gfx/invobjs/batcape")){
				Gob.batsLeaveMeAlone = false;
				ui.sess.glob.oc.gobAction(Gob::toggleBeastDangerRadii);
			}
		} catch (Exception ignored){}
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "pop") {
	    ava.avadesc = Composited.Desc.decode(ui.sess, args);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public int epat(Coord c) {
	for(int i = 0; i < ecoords.length; i++) {
	    if(c.isect(ecoords[i], invsq.sz()))
			return(i);
	}
	return(-1);
    }

    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop", epat(cc));
	return(true);
    }

    public void drawslots(GOut g) {
	int slots = 0;
	GameUI gui = getparent(GameUI.class);
	if((gui != null) && (gui.vhand != null)) {
	    try {
		SlotInfo si = ItemInfo.find(SlotInfo.class, gui.vhand.item.info());
		if(si != null)
		    slots = si.slots();
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < ecoords.length; i++) {
	    if((slots & (1 << i)) != 0) {
		g.chcolor(0, 200, 0, 96);
		g.frect(ecoords[i].add(1, 1), invsq.sz().sub(2, 2));
		g.chcolor();
	    }
	    g.image(invsq, ecoords[i]);
	    if(ebgs[i] != null)
			g.image(ebgs[i], ecoords[i]);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
		Object tt = super.tooltip(c, prev);
		if(tt != null)
			return(tt);
		int sl = epat(c);
		if(sl >= 0)
			return(etts[sl]);
		return(null);
    }

	public void draw(GOut g) {
		drawslots(g);
		super.draw(g);
		GameUI gui = ui.gui;
		if (updateBottomText) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 100){ // ND: Hopefully 100ms is enough? I can't reproduce it any more on my PC at least. This is client-sided, so ping should not affect it (SHOULD, BUT GOD KNOWS WITH LOFTAR)
				// ND: I genuinely don't know any other workaround to this crap not updating when you add a new item. For some reason this doesn't happen in Ardennes' (old render)
				//     In Ardennes', it looks like the UI freezes for a second when you try to add the new item sometimes. Maybe these weird hiccups are different in new render? For now, I have no clue.
				int prc = 0, exp = 0, det, intl = 0, ste = 0, snk, aHard = 0, aSoft = 0;
				CharWnd chrwdg = null;
				try {
					chrwdg = gui.chrwdg;
					for (CharWnd.Attr attr : chrwdg.base) {
						if (attr.attr.nm.contains("prc"))
							prc = attr.attr.comp;
						if (attr.attr.nm.contains("int"))
							intl = attr.attr.comp;
					}
					for (CharWnd.SAttr attr : chrwdg.skill) {
						if (attr.attr.nm.contains("exp"))
							exp = attr.attr.comp;
						if (attr.attr.nm.contains("ste"))
							ste = attr.attr.comp;
					}
					for (int i = 0; i < slots.length; i++) {
						WItem itm = slots[i];
						boolean isBroken = false;
						if (itm != null) {
							for (ItemInfo info : itm.item.info()) {
								if (info instanceof Wear) {
									if (((Wear) info).m-((Wear) info).d == 0)
										isBroken = true;
									break;
								}
							}

							for (ItemInfo info : itm.item.info()) {
								if (info instanceof Armor) {
									if (!isBroken){
										aHard += ((Armor) info).hard;
										aSoft += ((Armor) info).soft;
									}
									break;
								}
							}
						}
					}
					det = prc * exp;
					snk = intl * ste;
					String DetectionString = String.format("%,d", det).replace(',', '.');
					String SubtletyString = String.format("%,d", snk).replace(',', '.');
					//Subtlety = Text.renderstroked2("Subtlety: " + x, Color.WHITE, Color.BLACK, acf).tex();
					Detection = new TexI(Utils.outline2(Text.renderstroked2("Detection (Prc*Exp):  " + DetectionString, Color.WHITE, Color.BLACK, acf).img, Color.BLACK));
					Subtlety = new TexI(Utils.outline2(Text.renderstroked2("Subtlety (Int*Ste):  " + SubtletyString, Color.WHITE, Color.BLACK, acf).img, Color.BLACK));
					ArmorClass = new TexI(Utils.outline2(Text.renderstroked2("Armor Class:  " + (aHard + aSoft) + " (" + aHard + " + " + aSoft + ")", Color.WHITE, Color.BLACK, acf).img, Color.BLACK));
					updateBottomText = false;
				} catch (Exception e) { // fail silently
					e.printStackTrace();// Ignored
				}
			}
		}
		if (Detection != null && player)
			g.image(Detection, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - Detection.sz().x / 2, bg.sz().y - UI.scale(56)));
		if (Subtlety != null && player)
			g.image(Subtlety, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - Subtlety.sz().x / 2, bg.sz().y - UI.scale(40)));
		if (ArmorClass != null)
			g.image(ArmorClass, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - ArmorClass.sz().x / 2, bg.sz().y - UI.scale(20)));
	}

	public void tick(double dt) {
		super.tick(dt);
		if (OptWnd.autoDropLeechesCheckBox.a && player && checkForLeeches) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 100){
				for (SLOTS slot : SLOTS.values()) {
					WItem equippedItem = slots[slot.idx];
					if (equippedItem != null && equippedItem.item != null && equippedItem.item.getname() != null && equippedItem.item.getname().contains("Leech")){
						equippedItem.item.wdgmsg("drop", new Coord(equippedItem.sz.x / 2, equippedItem.sz.y / 2));
					}
				}
				checkForLeeches = false;
			}
		}
	}

    public boolean iteminteract(Coord cc, Coord ul) {
		return(false);
    }
}
