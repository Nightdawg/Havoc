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

import java.util.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.util.function.Function;
import java.util.stream.Collectors;

import static haven.CharWnd.attrf;
import static haven.Window.wbox;
import static haven.Inventory.invsq;

public class FightWnd extends Widget {
    public final int nsave;
    public int maxact;
    public final Actions actlist;
	public final ActionTypes acttypes;
	public Dropbox<Pair<Text, Integer>> schoolsDropdown;
	public List<Action> ALL = new ArrayList<>();
	private List<Action> acts = ALL;
	private ActionType selectedType = ActionType.All;
    public final Action[] order;
    public int usesave;
    private final Text[] saves;
    private final ImageInfoBox info;
	private Tex count2;
    private final Map<Indir<Resource>, Object[]> actrawinfo = new HashMap<>();
	private boolean needFilter = false;

    public static interface IconInfo {
	public void draw(BufferedImage img, Graphics g);
    }

    private static final OwnerContext.ClassResolver<FightWnd> actxr = new OwnerContext.ClassResolver<FightWnd>()
	.add(FightWnd.class, wdg -> wdg)
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
	public static final Text.Foundry keybindsFoundry = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 14);
    public static final Text.Foundry namef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true);
    public class Action implements ItemInfo.ResOwner {
	public final Indir<Resource> res;
	private final int id;
	public int a, u;
	private Text rnm, ra;
	private Tex ri, ru2;
	private String name;

	public Action(Indir<Resource> res, int id, int a, int u) {this.res = res; this.id = id; this.a = a; this.u = u;}

	public String rendertext() {
	    StringBuilder buf = new StringBuilder();
	    Resource res = this.res.get();
	    buf.append("$img[" + res.name + "]\n\n");
	    buf.append("$b{$font[serif,16]{" + res.flayer(Resource.tooltip).t + "}}\n\n");
	    Resource.Pagina pag = res.layer(Resource.pagina);
	    if(pag != null)
		buf.append(pag.text);
	    return(buf.toString());
	}

	private void a(int a) {
		if (this.a != a) {
			this.a = a;
			this.ru2 = null;
			this.ra = null;
		}
	}

	private void u(int u) {
		if (this.u != u && u <= a) {
			this.u = u;
			this.ru2 = null;
			recount();
		}
	}

	public Resource resource() {return(res.get());}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		Object[] rawinfo = actrawinfo.get(this.res);
		if(rawinfo != null)
		    info = ItemInfo.buildinfo(this, rawinfo);
		else
		    info = Arrays.asList(new ItemInfo.Name(this, res.get().flayer(Resource.tooltip).t));
	    }
	    return(info);
	}
	public <T> T context(Class<T> cl) {return(actxr.context(cl, FightWnd.this));}

	public BufferedImage rendericon() {
	    BufferedImage ret = res.get().flayer(Resource.imgc).scaled();
	    Graphics g = null;
	    for(ItemInfo inf : info()) {
		if(inf instanceof IconInfo) {
		    if(g == null) {
			BufferedImage buf = TexI.mkbuf(PUtils.imgsz(ret));
			g = buf.getGraphics();
			ret = buf;
		    }
		    ((IconInfo)inf).draw(ret, g);
		}
	    }
	    if(g != null)
		g.dispose();
	    return(ret);
	}

	private Tex icon = null;
	public Tex icon() {
	    if(icon == null)
		icon = new TexI(rendericon());
	    return(icon);
	}

	public BufferedImage renderinfo(int width) {
	    ItemInfo.Layout l = new ItemInfo.Layout();
	    l.width = width;
	    List<ItemInfo> info = info();
	    l.cmp.add(rendericon(), Coord.z);
	    ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, info);
	    l.cmp.add(namef.render(nm.str.text).img, new Coord(0, l.cmp.sz.y + UI.scale(10)));
	    l.cmp.sz = l.cmp.sz.add(0, UI.scale(10));
	    for(ItemInfo inf : info) {
		if((inf != nm) && (inf instanceof ItemInfo.Tip)) {
		    l.add((ItemInfo.Tip)inf);
		}
	    }
	    Resource.Pagina pag = res.get().layer(Resource.pagina);
	    if(pag != null)
		l.add(new ItemInfo.Pagina(this, pag.text));
	    return(l.render());
	}
    }

    private void recount() {
	int u = 0;
	for(Action act : ALL)
	    u += act.u;
	count2 = PUtils.strokeTex(Text.num12boldFnd.render(String.format("= %d/%d", u, maxact), (u > maxact) ? Color.RED : Color.WHITE));
    }

    public static class ImageInfoBox extends Widget {
	private Tex img;
	private Indir<Tex> loading;
	private final Scrollbar sb;

	public ImageInfoBox(Coord sz) {
	    super(sz);
	    sb = adda(new Scrollbar(sz.y, 0, 1), sz.x, 0, 1, 0);
	}

	public void drawbg(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}

	public Coord marg() {return(new Coord(10, 10));}

	public void tick(double dt) {
	    if(loading != null) {
		try {
		    set(loading.get());
		    loading = null;
		} catch(Loading e) {
			CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
		}
	    }
	    super.tick(dt);
	}

	public void draw(GOut g) {
	    drawbg(g);
	    if(img != null)
		g.image(img, marg().sub(0, sb.val));
	    super.draw(g);
	}

	public void set(Tex img) {
	    this.img = img;
	    if(img != null) {
		sb.max = img.sz().y + (marg().y * 2) - sz.y;
		sb.val = 0;
	    } else {
		sb.max = sb.val = 0;
	    }
	}
	public void set(Indir<Tex> loading) {
	    this.loading = loading;
	}

	public boolean mousewheel(Coord c, int amount) {
	    sb.ch(amount * 20);
	    return(true);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    sb.c = new Coord(sz.x - sb.sz.x, 0);
	    sb.resize(sz.y);
	    set(img);
	}
    }

	private static final Tex[] add = {Resource.loadtex("gfx/hud/buttons/addu"),
			Resource.loadtex("gfx/hud/buttons/addd")};
	private static final Tex[] sub = {Resource.loadtex("gfx/hud/buttons/subu"),
			Resource.loadtex("gfx/hud/buttons/subd")};

    public class Actions extends SListBox<Action, Widget> {
	private boolean loading = false;
	private Action drag = null;
	private UI.Grab grab;

	public Actions(Coord sz) {
	    super(sz, attrf.height() + UI.scale(2));
	}

	protected Action listitem(int n) {return (acts.get(n));}

	protected int listitems() {return (acts.size());}

	protected List<Action> items() {return(acts);}

	protected Widget makeitem(Action act, int idx, Coord sz) {
	    return(new Item(sz, act));
	}

	public class Item extends Widget implements DTarget {
	    public final Action item;
	    private final Label use;
	    private int u = -1, a = -1;
	    private UI.Grab grab;
	    private Coord dp;

	    public Item(Coord sz, Action act) {
		super(sz);
		this.item = act;
		Widget prev;
		prev = adda(new IButton("gfx/hud/buttons/add", "u", "d", "h").action(() -> setu(item.u + 1)), sz.x - UI.scale(2), sz.y / 2, 1.0, 0.5);
		prev = adda(new IButton("gfx/hud/buttons/sub", "u", "d", "h").action(() -> setu(item.u - 1)), prev.c.x - UI.scale(2), sz.y / 2, 1.0, 0.5);
		prev = use = adda(new Label("0/0", attrf), prev.c.x - UI.scale(5), sz.y / 2, 1.0, 0.5);
		add(IconText.of(Coord.of(prev.c.x - UI.scale(2), sz.y), act::rendericon, () -> act.res.get().flayer(Resource.tooltip).t), Coord.z);
	    }

	    public void tick(double dt) {
		if((item.u != this.u) || (item.a != this.a))
		    use.settext(String.format("%d/%d", this.u = item.u, this.a = item.a));
		super.tick(dt);
	    }

	    public boolean mousewheel(Coord c, int am) {
		if(ui.modshift) {
		    setu(item.u - am);
		    return(true);
		}
		return(super.mousewheel(c, am));
	    }

	    public boolean mousedown(Coord c, int button) {
		if(super.mousedown(c, button))
		    return(true);
		if(button == 1) {
		    change(item);
		    grab = ui.grabmouse(this);
		    dp = c;
		}
		return(true);
	    }

	    public void mousemove(Coord c) {
		super.mousemove(c);
		if((grab != null) && (c.dist(dp) > 5)) {
		    grab.remove();
		    grab = null;
		    drag(item);
		}
	    }

	    public boolean mouseup(Coord c, int button) {
		if((grab != null) && (button == 1)) {
		    grab.remove();
		    grab = null;
		    return(true);
		}
		return(super.mouseup(c, button));
	    }

	    public boolean setu(int u) {
		u = Utils.clip(u, 0, item.a);
		int s;
		for(s = 0; s < order.length; s++) {
		    if(order[s] == item)
			break;
		}
		if(u > 0) {
		    if(s == order.length) {
			for(s = 0; s < order.length; s++) {
			    if(order[s] == null)
				break;
			}
			if(s == order.length)
			    return(false);
			order[s] = item;
		    }
		} else {
		    if(s < order.length)
			order[s] = null;
		}
		item.u(u);
		return(true);
	    }

	    public boolean drop(Coord cc, Coord ul) {
		return(false);
	    }

	    public boolean iteminteract(Coord cc, Coord ul) {
		FightWnd.this.wdgmsg("itemact", item.id, ui.modflags());
		return(true);
	    }
	}

	public void change(Action act) {
	    if(act != null)
		info.set(() -> new TexI(act.renderinfo(info.sz.x - UI.scale(20))));
	    else if(sel != null)
		info.set((Tex)null);
	    super.change(act);
	}

	public void tick(double dt) {
	    if(loading) {
		loading = false;
		for(Action act : acts) {
		    try {
			act.name = act.res.get().flayer(Resource.tooltip).t;
		    } catch(Loading l) {
			act.name = "...";
			loading = true;
		    }
		}
		Collections.sort(acts, Comparator.comparing(act -> act.name));
	    }
	    super.tick(dt);
	}

	public void draw(GOut g) {
	    if(drag != null) {
		try {
		    Tex dt = drag.res.get().flayer(Resource.imgc).tex();
		    ui.drawafter(ag -> ag.image(dt, ui.mc.sub(dt.sz().div(2))));
		} catch(Loading l) {
		}
	    }
	    super.draw(g);
	}

	public void drag(Action act) {
	    if(grab == null)
		grab = ui.grabmouse(this);
	    drag = act;
	}

	public boolean mouseup(Coord c, int button) {
	    if((grab != null) && (button == 1)) {
		grab.remove();
		grab = null;
		if(drag != null) {
		    ui.dropthing(ui.root, c.add(rootpos()), drag);
		    drag = null;
		}
		return(true);
	    }
	    return(super.mouseup(c, button));
	}
    }

    public int findorder(Action a) {
	for(int i = 0; i < order.length; i++) {
	    if(order[i] == a)
		return(i);
	}
	return(-1);
    }
	public class BView extends Widget implements DropTarget {
		private int subp = UI.scale(-1);
		private int addp = UI.scale(-1);
		private final int subOffX = UI.scale(3);
		private final int addOffX = UI.scale(16);
		private final int subOffY = invsq.sz().y + UI.scale(10) + UI.scale(10);
		private UI.Grab d = null;
		private Action drag = null;
		private Coord dp;
		private final Coord[] animoff = new Coord[order.length];
		private final double[] animpr = new double[order.length];
		private boolean anim = false;

		private BView() {
			super(new Coord(((invsq.sz().x + UI.scale(2)) * UI.scale((order.length - 1))) + (UI.scale(10) * UI.scale(((order.length - 1) / 5))) + UI.scale(60), 0).add(invsq.sz().x, invsq.sz().y + UI.scale(35)));
		}

		private Coord itemc(int i) {
			return (new Coord(((invsq.sz().x + 2) * i) + (UI.scale(10) * UI.scale((i / 5))), 0));
		}

		private int citem(Coord c) {
			for (int i = 0; i < order.length; i++) {
				if (c.isect(itemc(i), invsq.sz()))
					return (i);
			}
			return (-1);
		}

		private int csub(Coord c) {
			for (int i = 0; i < order.length; i++) {
				if (c.isect(itemc(i).add(subOffX, subOffY), sub[0].sz()))
					return (i);
			}
			return (-1);
		}

		private int cadd(Coord c) {
			for (int i = 0; i < order.length; i++) {
				if (c.isect(itemc(i).add(addOffX, subOffY), add[0].sz()))
					return (i);
			}
			return (-1);
		}

		public void draw(GOut g) {
			int pcy = invsq.sz().y;

			int[] reo;
			if (anim) {
				reo = new int[order.length];
				for (int i = 0, a = 0, b = order.length - 1; i < order.length; i++) {
					if (animoff[i] == null)
						reo[a++] = i;
					else
						reo[b--] = i;
				}
			}

			for (int i = 0; i < order.length; i++) {
				Coord c = itemc(i);
				g.image(invsq, c);
				Action act = order[i];
				try {
					if (act != null) {
						Coord ic = c.add(1, 1);
						if (animoff[i] != null)
							ic = ic.add(animoff[i].mul(Math.pow(1.0 - animpr[i], 3)));

						g.image(act.res.get().layer(Resource.imgc).tex(), ic);

						if (act.ru2 == null)
							act.ru2 = PUtils.strokeTex(Text.num12boldFnd.render(String.format("%d/%d", act.u, act.a)));

						g.image(act.ru2, c.add(invsq.sz().x / 2 - act.ru2.sz().x / 2, pcy));
						g.chcolor();

						g.image(sub[subp == i ? 1 : 0], c.add(subOffX, subOffY));
						g.image(add[addp == i ? 1 : 0], c.add(addOffX, subOffY));
					}
				} catch (Loading e) {
					CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
				}
				g.chcolor(255, 255, 255, 255);
				String keybindString = Fightsess.kb_acts[i].key().name();
				g.aimage(new TexI(Utils.outline2(keybindsFoundry.render(keybindString).img, Color.BLACK, true)), c.add(invsq.sz().sub(2, 0)), 0.95, 0.95);
				g.chcolor();
			}

			g.image(count2, new Coord(UI.scale(370), pcy));

			if ((drag != null) && (dp == null)) {
				try {
					final Tex dt = drag.res.get().layer(Resource.imgc).tex();
					ui.drawafter(new UI.AfterDraw() {
						public void draw(GOut g) {
							g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
						}
					});
				} catch (Loading e) {
					CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
				}
			}
		}

		public boolean mousedown(Coord c, int button) {
			int s = citem(c);

			if (button == 3) {
				if (s >= 0) {
					if (order[s] != null)
						order[s].u(0);
					order[s] = null;
					return (true);
				}
			} else if (button == 1) {
				int acti = csub(c);
				if (acti >= 0) {
					subp = acti;
					return true;
				}
				acti = cadd(c);
				if (acti >= 0) {
					addp = acti;
					return true;
				}

				if (s >= 0) {
					Action act = order[s];
					actlist.change(act);
					actlist.display();

					d = ui.grabmouse(this);
					drag = order[s];
					dp = c;
					return true;
				}
			}
			return (super.mousedown(c, button));
		}

		public void mousemove(Coord c) {
			super.mousemove(c);
			if (drag != null && dp != null) {
				if (c.dist(dp) > 5)
					dp = null;
			}
		}

		public boolean mouseup(Coord c, int button) {
			subp = -1;
			addp = -1;

			int s = csub(c);
			if (s >= 0) {
				Action act = order[s];
				if (act != null) {
					if (act.u == 1) {
						if (order[s] != null)
							order[s].u(0);
						order[s] = null;
					} else {
						act.u(act.u - 1);
					}
					return true;
				}
			}

			s = cadd(c);
			if (s >= 0) {
				Action act = order[s];
				if (act != null) {
					act.u(act.u + 1);
					return true;
				}
			}

			if (d != null && button == 1) {
				d.remove();
				d = null;
				if (drag != null) {
					if (dp == null)
						ui.dropthing(ui.root, c.add(rootpos()), drag);
					drag = null;
				}
				return true;
			}

			return (super.mouseup(c, button));
		}

		private void animate(int s, Coord off) {
			animoff[s] = off;
			animpr[s] = 0.0;
			anim = true;
		}

		public boolean dropthing(Coord c, Object thing) {
			if (thing instanceof Action) {
				Action act = (Action) thing;
				int s = citem(c);
				if (s < 0)
					return (false);
				if (order[s] != act) {
					int cp = findorder(act);
					if (cp >= 0)
						order[cp] = order[s];
					if (order[s] != null) {
						if (cp >= 0) {
							animate(cp, itemc(s).sub(itemc(cp)));
						} else {
							order[s].u(0);
						}
					}
					order[s] = act;
					if (act.u < 1)
						act.u(1);
				}
				return (true);
			}
			return (false);
		}

		public void tick(double dt) {
			if (anim) {
				boolean na = false;
				for (int i = 0; i < order.length; i++) {
					if (animoff[i] != null) {
						if ((animpr[i] += (dt * 3)) > 1.0)
							animoff[i] = null;
						else
							na = true;
					}
				}
				anim = na;
			}
		}
	}
    public class Savelist extends Listbox<Integer> implements ReadLine.Owner {
	private int edit = -1;
	private Text.Line redit = null;
	private ReadLine nmed;
	private double focusstart;

	public Savelist(int w, int h) {
	    super(w, h, attrf.height() + 2);
	    setcanfocus(true);
	    sel = Integer.valueOf(0);
	}

	protected Integer listitem(int idx) {return(idx);}
	protected int listitems() {return(nsave);}

	protected void drawbg(GOut g) {}

	protected void drawitem(GOut g, Integer save, int n) {
	    g.chcolor((n % 2 == 0)?CharWnd.every:CharWnd.other);
	    g.frect(Coord.z, g.sz());
	    g.chcolor();
	    if(n == edit) {
		if(redit == null)
		    redit = attrf.render(nmed.line());
		g.aimage(redit.tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
		if(hasfocus && (((Utils.rtime() - focusstart) % 1.0) < 0.5)) {
		    int cx = redit.advance(nmed.point());
		    g.chcolor(255, 255, 255, 255);
		    Coord co = new Coord(UI.scale(20) + cx + UI.scale(1), (g.sz().y - redit.sz().y) / 2);
		    g.line(co, co.add(0, redit.sz().y), 1);
		    g.chcolor();
		}
	    } else {
		g.aimage(saves[n].tex(), new Coord(UI.scale(20), itemh / 2), 0.0, 0.5);
	    }
	    if(n == usesave)
		g.aimage(CheckBox.smark, new Coord(itemh / 2, itemh / 2), 0.5, 0.5);
	}

	private Coord lc = null;
	private double lt = 0;
	public boolean mousedown(Coord c, int button) {
	    boolean ret = super.mousedown(c, button);
	    if(ret && (button == 1)) {
		double now = Utils.rtime();
		if(((now - lt) < 0.5) && (c.dist(lc) < 10) && (sel != null) && (saves[sel] != unused)) {
		    if(sel == usesave) {
			edit = sel;
			nmed = ReadLine.make(this, saves[sel].text);
			redit = null;
			parent.setfocus(this);
			focusstart = now;
		    } else {
			load(sel);
			use(sel);
		    }
		} else {
		    lt = now;
		    lc = c;
		}
	    }
	    return(ret);
	}

	public void done(ReadLine buf) {
	    saves[edit] = attrf.render(buf.line());
	    edit = -1;
	    nmed = null;
	}

	public void changed(ReadLine buf) {
	    redit = null;
	}

	public void change(Integer sel) {
	    super.change(sel);
	    if((edit != -1) && (edit != sel)) {
		edit = -1;
		redit = null;
		nmed = null;
	    }
	}

	public boolean keydown(KeyEvent ev) {
	    if(edit != -1) {
		if(key_esc.match(ev)) {
		    edit = -1;
		    redit = null;
		    nmed = null;
		    return(true);
		} else {
		    return(nmed.key(ev));
		}
	    }
	    return(super.keydown(ev));
	}
    }

    @RName("fmg")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new FightWnd((Integer)args[0], (Integer)args[1], (Integer)args[2]));
	}
    }

    public void load(int n) {
	wdgmsg("load", n);
    }

    public void save(int n) {
	List<Object> args = new LinkedList<Object>();
	args.add(n);
	if(saves[n] != unused)
	    args.add(saves[n].text);
	for(int i = 0; i < order.length; i++) {
	    if(order[i] == null) {
		args.add(null);
	    } else {
		args.add(order[i].id);
		args.add(order[i].u);
	    }
	}
	wdgmsg("save", args.toArray(new Object[0]));
    }

    public void use(int n) {
	wdgmsg("use", n);
    }

    private Text unused = new Text.Foundry(attrf.font.deriveFont(java.awt.Font.ITALIC)).aa(true).render("Unused save");
	Window renwnd = null;
    public FightWnd(int nsave, int nact, int max) {
	super(Coord.z);
	this.nsave = nsave;
	this.maxact = max;
	this.order = new Action[nact];
	this.saves = new Text[nsave];
	for(int i = 0; i < nsave; i++)
	    saves[i] = unused;

	add(CharWnd.settip(new Img(CharWnd.catf.render("Martial Arts & Combat Schools").tex()), "gfx/hud/chr/tips/combat"), 0, 0);


	Widget p;
	info = add(new ImageInfoBox(UI.scale(new Coord(286, 208))), UI.scale(new Coord(5, 43)).add(wbox.btloff())); // ND: Left panel
	Frame.around(this, Collections.singletonList(info));

	actlist = add(new Actions(UI.scale(286, 208)), UI.scale(new Coord(310, 43)).add(wbox.btloff())); // ND: Right panel
	Frame.around(this, Collections.singletonList(actlist));

	acttypes = add(new ActionTypes(this::actionTypeSelected), actlist.pos("ul").adds(60,-28));
	acttypes.setSelectedColor(new Color(100, 100, 100, 128));
	acttypes.select(0);

	p = add(new BView(), info.pos("bl").adds(112,20));
	p = add(schoolsDropdown = new Dropbox<Pair<Text, Integer>>(UI.scale(400), saves.length, saves[0].sz().y) {
		@Override
		protected Pair<Text, Integer> listitem(int i) {
			return new Pair<>(saves[i], i);
		}
		@Override
		protected int listitems() {
			return saves.length;
		}
		@Override
		protected void drawitem(GOut g, Pair<Text, Integer> item, int i) {
			g.image(item.a.tex(), Coord.z);
		}
		@Override
		public void change2(Pair<Text, Integer> item) {
			if (renwnd != null){
				renwnd.remove();
				renwnd = null;
			}
			acttypes.select(0); // ND: when switching decks, reset the combat moves category to "all", to avoid total cards used bug
			super.change2(item);
		}
		@Override
		public void change(Pair<Text, Integer> item) {
			if (renwnd != null){
				renwnd.remove();
				renwnd = null;
			}
			acttypes.select(0); // ND: when switching decks, reset the combat moves category to "all", to avoid total cards used bug
			super.change2(item);
			load(item.b);
			use(item.b);
		}
	}, p.pos("bl").adds(0, 10).x(info.c.x).add(wbox.btloff()));
	Frame.around(this, Collections.singletonList(schoolsDropdown));

		p = add(new Button(UI.scale(90), "Save & Use", false) {
			public void click() {
				Pair<Text, Integer> sel = schoolsDropdown.sel;
				if (sel != null) {
					save(sel.b);
					use(sel.b);
					schoolsDropdown.change(sel);
				}
			}
		}, p.pos("ur").adds(20, 0));

		add(new Button(UI.scale(70), "Rename", false) {
			public void click() {
				Pair<Text, Integer> sel = schoolsDropdown.sel;
				if (sel == null)
					return;
				if (renwnd != null){
					renwnd.remove();
					renwnd = null;
				}
				renwnd = new Window(UI.scale(new Coord(225, 100)), "Rename Deck") {
					{
						final TextEntry txtname = new TextEntry(UI.scale(200), sel.a.text);
						add(txtname, UI.scale(new Coord(15, 20)));

						Button add = new Button(UI.scale(80), "Save") {
							@Override
							public void click() {
								saves[sel.b] = attrf.render(txtname.text());
								schoolsDropdown.sel = new Pair<>(saves[sel.b], sel.b);
								save(sel.b);
								parent.reqdestroy();
							}
						};
						add(add, UI.scale(new Coord(15, 60)));

						Button cancel = new Button(UI.scale(80), "Cancel") {
							@Override
							public void click() {
								parent.reqdestroy();
							}
						};
						add(cancel, UI.scale(new Coord(135, 60)));
					}

					@Override
					public void wdgmsg(Widget sender, String msg, Object... args) {
						if (msg.equals("close"))
							reqdestroy();
						else
							super.wdgmsg(sender, msg, args);
					}

				};
				ui.gui.add(renwnd, new Coord((ui.gui.sz.x - renwnd.sz.x) / 2, (ui.gui.sz.y - renwnd.sz.y*3) / 2));
				renwnd.show();
			}
		}, p.pos("ur").adds(6, 0));


	pack();
    }
	public void changebutton(Integer index) {
		try {
			if (!saves[index].text.equals("Unused save")) {
				schoolsDropdown.change(new Pair(saves[index], index));
				ui.gui.msg("Switched to deck No." + (index+1) + ": " + saves[index].text, Color.orange);
				ui.sfx(RootWidget.msgsfx);
			} else {
				ui.gui.msg("This is not a saved deck, not switching.", Color.red);
				ui.sfx(RootWidget.errsfx);
			}
		} catch (Exception e) {
			ui.gui.msg("Exception switching combat decks, exception ignored to avoid crash.", Color.white);
			ui.sfx(RootWidget.errsfx);
		}
	}

	private Void actionTypeSelected(TabStrip.Button<ActionType> button) {
		selectedType = button.tag;
		needFilter = true;
		return null;
	}

	@Override
	public void tick(double dt) {
		super.tick(dt);
		if(needFilter) {
			doFilter();
		}
	}

	private void doFilter() {
		try {
			if(ALL != null) {
				acts = ALL.stream().filter(selectedType::matches).collect(Collectors.toList());
				acts.sort(Comparator.comparing(a -> a.res.get().layer(Resource.tooltip).t));
				actlist.change(actlist.listitems() > 0 ? actlist.listitem(0) : null);
				actlist.showsel();
				needFilter = false;
			}
		} catch (Resource.Loading e) {
			CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));
		}
	}

    public Action findact(int resid) {
	for(Action act : ALL) {
	    if(act.id == resid)
		return(act);
	}
	return(null);
    }

    public void uimsg(String nm, Object... args) {
	if(nm == "avail") {
	    List<Action> acts = new ArrayList<Action>();
	    int a = 0;
	    while(true) {
		int resid = (Integer)args[a++];
		if(resid < 0)
		    break;
		int av = (Integer)args[a++];
		Action pact = findact(resid);
		if(pact == null) {
		    acts.add(new Action(ui.sess.getres(resid), resid, av, 0));
		} else {
		    acts.add(pact);
		    pact.a(av);
		}
	    }
		this.ALL = acts;
	    actlist.loading = true;
		needFilter = true;
	} else if(nm == "tt") {
	    Indir<Resource> res = ui.sess.getres((Integer)args[0]);
	    Object[] rawinfo = (Object[])args[1];
	    actrawinfo.put(res, rawinfo);
	} else if(nm == "used") {
	    int a = 0;
	    for(Action act : acts)
		act.u(0);
	    for(int i = 0; i < order.length; i++) {
		int resid = (Integer)args[a++];
		if(resid < 0) {
		    order[i] = null;
		    continue;
		}
		int us = (Integer)args[a++];
		(order[i] = findact(resid)).u(us);
	    }
	} else if(nm == "saved") {
	    int fl = (Integer)args[0];
	    for(int i = 0; i < nsave; i++) {
		if((fl & (1 << i)) != 0) {
		    if(args[i + 1] instanceof String)
			saves[i] = attrf.render((String)args[i + 1]);
		    else
			saves[i] = attrf.render(String.format("Saved school %d", i + 1));
		} else {
		    saves[i] = unused;
		}
	    }
	} else if(nm == "use") {
		int i = (int) args[0];
		if (i >= 0 && i < saves.length)
			schoolsDropdown.change2(new Pair<>(saves[i], i));
	} else if(nm == "max") {
	    maxact = (Integer)args[0];
	    recount();
	} else {
	    super.uimsg(nm, args);
	}
    }

	static class ActionTypes extends TabStrip<ActionType> {
		private final Function<Button<ActionType>, Void> selected;

		ActionTypes(Function<Button<ActionType>, Void> selected) {
			super();
			this.selected = selected;
			ActionType[] types = ActionType.values();
			for (int i = 0; i < types.length; i++) {
				insert(i, types[i].icon(), "", types[i].name()).tag = types[i];
			}
		}

		@Override
		protected void selected(Button<ActionType> button) {
			if(selected != null) {
				selected.apply(button);
			}
		}
	}

	enum ActionType {
		All("gfx/hud/tab/combat/all", null),
		Attacks("gfx/hud/tab/combat/attack", new HashSet<>(Arrays.asList(
				"paginae/atk/pow",
				"paginae/atk/lefthook",
				"paginae/atk/lowblow",
				"paginae/atk/oppknock",
				"paginae/atk/ripapart",
				"paginae/atk/fullcircle",
				"paginae/atk/cleave",
				"paginae/atk/barrage",
				"paginae/atk/sideswipe",
				"paginae/atk/sting",
				"paginae/atk/sos",
				"paginae/atk/knockteeth",
				"paginae/atk/kick",
				"paginae/atk/haymaker",
				"paginae/atk/chop",
				"paginae/atk/gojug",
				"paginae/atk/uppercut",
				"paginae/atk/punchboth",
				"paginae/atk/stealthunder",
				"paginae/atk/ravenbite",
				"paginae/atk/takedown",
				"paginae/atk/flex"
		))),
		Defences("gfx/hud/tab/combat/restore", new HashSet<>(Arrays.asList(
				"paginae/atk/regain",
				"paginae/atk/dash",
				"paginae/atk/zigzag",
				"paginae/atk/yieldground",
				"paginae/atk/watchmoves",
				"paginae/atk/sidestep",
				"paginae/atk/qdodge",
				"paginae/atk/jump",
				"paginae/atk/fdodge",
				"paginae/atk/artevade",
				"paginae/atk/flex"
		))),
		Maneuvers("gfx/hud/tab/combat/maneuver", new HashSet<>(Arrays.asList(
				"paginae/atk/toarms",
				"paginae/atk/shield",
				"paginae/atk/parry",
				"paginae/atk/oakstance",
				"paginae/atk/dorg",
				"paginae/atk/chinup",
				"paginae/atk/bloodlust",
				"paginae/atk/combmed"
		))),
		Moves("gfx/hud/tab/combat/move", new HashSet<>(Arrays.asList(
				"paginae/atk/think",
				"paginae/atk/takeaim",
				"paginae/atk/dash",
				"paginae/atk/oppknock"
		))),
		Other("gfx/invobjs/missing");

		private final String res;
		private final Set<String> list;
		private Tex icon;
		private boolean inverted = false;

		Tex icon() {
			if(icon == null) {
				icon = new TexI(PUtils.convolvedown(Resource.loadimg(res), UI.scale(20, 20), CharWnd.iconfilter));
			}
			return icon;
		}

		boolean matches(Action action) {
			if(inverted) {
				for(ActionType actionType : ActionType.values()) {
					if(actionType.list != null && actionType.list.contains(action.res.get().name)) {
						return false;
					}
				}
				return true;
			} else {
				return list == null || list.contains(action.res.get().name);
			}
		}

		ActionType(String res, Set<String> list) {
			this.res = res;
			this.list = list;
		}

		ActionType(String res) {
			this.res = res;
			this.list = null;
			inverted = true;
		}
	}

}
