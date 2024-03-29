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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Color;
import haven.render.*;

public class KinInfo extends GAttrib implements RenderTree.Node, PView.Render2D {
    public static final BufferedImage vlg = Resource.loadimg("gfx/hud/vilind");
	public static final Text.Foundry nfnd = new Text.Foundry(Text.dfont, 12); // ND: Changed the size from 10 to 12
    public String name;
	public static final Color unknowncol = new Color(180, 180, 180);
	public static final Color villrealmcol = new Color(255, 255, 255);
    public int group, type;
    public double seen = 0;
    private Tex rnm = null;
	public boolean unknown = false;
    
    public KinInfo(Gob g, String name, int group, int type) {
		super(g);
		this.name = name;
		if (name.equals("Unknown") && !isVillager()) {
			unknown = true;
		}
		this.group = group;
		this.type = type;
    }
    
    public void update(String name, int group, int type) {
		if (name != null || !name.equals("Unknown")) {
			this.name = name;
			unknown = false;
		}
		this.group = group;
		this.type = type;
		rnm = null;
    }
    
    public Tex rendered() {
	if(rnm == null) {
	    boolean hv = (type & 2) != 0;
	    BufferedImage nm = null;
		if (name != null && name.equals("Village/Realm Member") && isVillager()) {
			unknown = false;
			nm = Utils.outline2(nfnd.renderstroked("Village/Realm Member", BuddyWnd.gc[group], Color.BLACK).img, Color.BLACK, true);
		} else if (name != null && name.length() > 0 && !name.equals("Unknown")) {
			unknown = false;
			nm = Utils.outline2(nfnd.renderstroked(name, BuddyWnd.gc[group], Color.BLACK).img, Color.BLACK, true);
		} else {
			unknown = true;
			nm = Utils.outline2(nfnd.renderstroked("Unknown", unknowncol, Color.BLACK).img, Color.BLACK, true);
		}
//	    if(name.length() > 0)
//		//nm = AUtils.outline2(nfnd.render(name, BuddyWnd.gc[group]).img, AUtils.contrast(BuddyWnd.gc[group]));
//		nm = Utils.outline2(nfnd.renderstroked(name, BuddyWnd.gc[group], Color.BLACK).img, Color.BLACK, true); // ND: Changed this for better name visibility
	    int w = 0, h = 0;
	    if(nm != null) {
		w += nm.getWidth();
		if(nm.getHeight() > h)
		    h = nm.getHeight();
	    }
	    if(hv) {
		w += vlg.getWidth() + 1;
		if(vlg.getHeight() > h)
		    h = vlg.getHeight();
	    }
	    if(w == 0) {
		rnm = new TexI(TexI.mkbuf(new Coord(1, 1)));
	    } else {
		BufferedImage buf = TexI.mkbuf(new Coord(w, h));
		Graphics g = buf.getGraphics();
		int x = 0;
		if(hv) {
		    g.drawImage(vlg, x, (h / 2) - (vlg.getHeight() / 2), null);
		    x += vlg.getWidth() + 1;
		}
		if(nm != null) {
		    g.drawImage(nm, x, (h / 2) - (nm.getHeight() / 2), null);
		    x += nm.getWidth();
		}
		g.dispose();
		rnm = new TexI(buf);
	    }
	}
	return(rnm);
    }
    
    public void draw(GOut g, Pipe state) {
		if (GameUI.showUI) {
		Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 15), state, Area.sized(g.sz())).round2();
		if (sc.isect(Coord.z, g.sz())) {
			double now = Utils.rtime();
			if (seen == 0)
				seen = now;
			double tm = now - seen;
			Color show = null;
			boolean auto = (type & 1) == 0;
			if (false) {
				/* XXX: QQ, RIP in peace until constant
				 * mouse-over checks can be had. */
				if (auto && (tm < 7.5)) {
					show = Utils.clipcol(255, 255, 255, (int) (255 - ((255 * tm) / 7.5)));
				}
			} else {
				show = Color.WHITE;
			}
			if (show != null) {
				Tex t = rendered();
				if (t != null) {
					if (gob != null && gob.glob != null && gob.glob.map != null && gob.glob.map.sess != null && gob.glob.map.sess.ui != null
							&& gob.glob.map.sess.ui.gui != null && gob.glob.map.sess.ui.gui.map != null) { // ND: Probably overkill. I have no clue if anything can break here, but just to be safe I guess?
						final Double angle = gob.glob.map.sess.ui.gui.map.screenangle(gob.rc, true);
						if (angle.equals(Double.NaN)) {
							g.chcolor(show);
							g.aimage(t, sc, 0.5, 1.0);
							g.chcolor();
						}
					}
				}
			}
		} else {
			seen = 0;
		}
	}
    }

    @OCache.DeltaType(OCache.OD_BUDDY)
    public static class $buddy implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    String name = msg.string();
	    if(name.length() > 0) {
		int group = msg.uint8();
		int btype = msg.uint8();
		KinInfo b = g.getattr(KinInfo.class);
		if (name.equals(" ") && ((btype & 2) != 0))
			name = "Village/Realm Member";
		GameUI.gobIdToKinName.put(g.id, name);
		if(b == null) {
		    g.setattr(new KinInfo(g, name, group, btype));
		} else {
		    b.update(name, group, btype);
		}
	    } else {
		g.delattr(KinInfo.class);
	    }
	}
    }

	public boolean isVillager() {return (type & 2) != 0;}
}
