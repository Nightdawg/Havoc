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

package haven.resutil;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;

import java.util.*;
import java.awt.Color;
import java.awt.image.*;

public class FoodInfo extends ItemInfo.Tip {
    public final double end, glut, cons;
    public final Event[] evs;
    public final Effect[] efs;
    public final int[] types;
	private UI ui = null;

    public FoodInfo(Owner owner, double end, double glut, double cons, Event[] evs, Effect[] efs, int[] types) {
	super(owner);
	this.end = end;
	this.glut = glut;
	this.cons = cons;
	this.evs = evs;
	this.efs = efs;
	this.types = types;
	if (owner instanceof GItem){
		this.ui = ((GItem) owner).ui;
	}
    }

    public FoodInfo(Owner owner, double end, double glut, Event[] evs, Effect[] efs, int[] types) {
	this(owner, end, glut, 0, evs, efs, types);
    }

    public static class Event {
	public static final Coord imgsz = new Coord(Text.std.height(), Text.std.height());
	public final CharWnd.FoodMeter.Event ev;
	public final BufferedImage img;
	public final double a;

	public Event(Resource res, double a) {
	    this.ev = res.flayer(CharWnd.FoodMeter.Event.class);
	    this.img = PUtils.convolve(res.flayer(Resource.imgc).img, imgsz, CharWnd.iconfilter);
	    this.a = a;
	}
    }

    public static class Effect {
	public final List<ItemInfo> info;
	public final double p;

	public Effect(List<ItemInfo> info, double p) {this.info = info; this.p = p;}
    }

    public BufferedImage tipimg() {
	String head = null;
	boolean matchFound = false;
	double efficiency = 100;
	boolean calculateEfficiency = ui != null && ui.modshift;
	if (ui != null)
		for (CharWnd.Constipations.El el : ui.gui.chrwdg.cons.els) {
			if (el.t.res.get().name.equals(((GItem) this.owner).resname())) {
				Color c = (el.a > 1.0)? CharWnd.Constipations.buffed:Utils.blendcol(CharWnd.Constipations.none, CharWnd.Constipations.full, el.a);
				efficiency = 100 * (1.0 - el.a);
				head = String.format("\nFood Efficiency: $col["+ c.getRed() +","+ c.getGreen() +","+ c.getBlue() +"]{%s%%}", Utils.odformat2(efficiency, 2));
				matchFound = true;
				break;
			}
		}
	if (!matchFound && ui != null)
		head = String.format("\nFood Efficiency: $col[49,255,39]{%s%%}", Utils.odformat2(efficiency, 2));
	else if (ui == null)
		head = "";
	head += String.format("\nEnergy: $col[128,128,255]{%s%%}  |  Hunger: $col[255,192,128]{%s\u2030}", Utils.odformat2(end * 100, 2), Utils.odformat2(calculateEfficiency ? (glut * 1000 * (efficiency/100)) : (glut * 1000), 2));
	head += String.format("\nEnergy/Hunger: $col[128,128,255]{%s}", Utils.odformat2((end * 100) / (glut * 1000), 2));
	double totalFeps = 0;
	for (int i = 0; i < evs.length; i++) {
		totalFeps += evs[i].a;
	}

	if (evs.length > 0) {
		head += "\n\nFood Event Points:";
	}
	BufferedImage base = RichText.render(head, 0).img;
	Collection<BufferedImage> imgs = new LinkedList<BufferedImage>();
	imgs.add(base);
	for(int i = 0; i < evs.length; i++) {
	    Color col = Utils.blendcol(evs[i].ev.col, Color.WHITE, 0.5);
	    imgs.add(catimgsh(5, UI.scale(15), null, evs[i].img, RichText.render(String.format("%s: $col[%d,%d,%d]{%s}", evs[i].ev.nm, col.getRed(), col.getGreen(), col.getBlue(), Utils.odformat2(calculateEfficiency ? (evs[i].a * (efficiency/100)) : evs[i].a, 2)), 0).img));
	}
	for(int i = 0; i < efs.length; i++) {
	    BufferedImage efi = ItemInfo.longtip(efs[i].info);
	    if(efs[i].p != 1)
		efi = catimgsh(5, efi, RichText.render(String.format("$i{($col[192,192,255]{%d%%} chance)}", (int)Math.round(efs[i].p * 100)), 0).img);
	    imgs.add(efi);
	}
		imgs.add(RichText.render(String.format("\nTotal FEPs: $col[0,180,0]{%s}", Utils.odformat2(calculateEfficiency ? (totalFeps * (efficiency/100)) : totalFeps, 2)), 0).img);
		imgs.add(RichText.render(String.format("FEPs/Hunger: $col[0,180,0]{%s}", Utils.odformat2(totalFeps / (1000 * glut), 2)), 0).img);
		if (ui != null)
			imgs.add(RichText.render(calculateEfficiency ? "$col[218,163,0]{<Calculated with Efficiency>}" : "$col[185,185,185]{<Hold Shift for Efficiency>}", 300).img);
	return(catimgs(0, imgs.toArray(new BufferedImage[0])));
    }
}
