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

import haven.render.*;
import haven.res.ui.tt.q.quality.Quality;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

public class OptWnd extends Window {
    public final Panel main;
	public final Panel advancedSettings;
    public Panel current;
	public static int cameraLmaoMessage = 1; // ND: Message for "cam" console command, idk where to put this lmao

    public void chpanel(Panel p) {
	if(current != null)
	    current.hide();
	(current = p).show();
	cresize(p);
    }

    public void cresize(Widget ch) {
	if(ch == current) {
	    Coord cc = this.c.add(this.sz.div(2));
	    pack();
	    move(cc.sub(this.sz.div(2)));
	}
    }

    public class PButton extends Button {
	public final Panel tgt;
	public final int key;
	public String newWindowTitle;

	public PButton(int w, String title, int key, Panel tgt) {
	    super(w, title, false);
	    this.tgt = tgt;
	    this.key = key;
	}
		public PButton(int w, String title, int key, Panel tgt, String newWindowTitle) {
			super(w, title, false);
			this.tgt = tgt;
			this.key = key;
			this.newWindowTitle = newWindowTitle;
		}

	public void click() {
		chpanel(tgt);
		OptWnd.this.cap = newWindowTitle;
	}

	public boolean keydown(java.awt.event.KeyEvent ev) {
	    if((this.key != -1) && (ev.getKeyChar() == this.key)) {
		click();
		return(true);
	    }
	    return(false);
	}
    }

    public class Panel extends Widget {
	public Panel() {
	    visible = false;
	    c = Coord.z;
	}
    }

    private void error(String msg) {
	GameUI gui = getparent(GameUI.class);
	if(gui != null)
	    gui.error(msg);
    }

    public class VideoPanel extends Panel {
	private final Widget back;
	private CPanel curcf;

	public VideoPanel(Panel prev) {
	    super();
	    back = add(new PButton(UI.scale(200), "Back", 27, prev, "Options            "));
	}

	public class CPanel extends Widget {
	    public GSettings prefs;

	    public CPanel(GSettings gprefs) {
		this.prefs = gprefs;
		Widget prev;
		int marg = UI.scale(5);
		prev = add(new CheckBox("Render shadows") {
			{a = prefs.lshadow.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.lshadow, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, Coord.z);
		prev = add(new Label("Render scale"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int steps = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), -2 * steps, 2 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
			       }
			       public void changed() {
				   try {
				       float val = (float)Math.pow(2, this.val / (double)steps);
				       ui.setgprefs(prefs = prefs.update(null, prefs.rscale, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new CheckBox("Vertical sync") {
			{a = prefs.vsync.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.vsync, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, prev.pos("bl").adds(0, 5));
		prev = add(new Label("Framerate limit (active window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.hz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Framerate limit (background window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.bghz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Lighting mode"), prev.pos("bl").adds(0, 5));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs
						 .update(null, prefs.lightmode, GSettings.LightMode.values()[btn])
						 .update(null, prefs.maxlights, 0));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				resetcf();
			    }
			};
		    prev = grp.add("Global", prev.pos("bl").adds(5, 2));
		    prev.settip("Global lighting supports fewer light sources, and scales worse in " +
				"performance per additional light source, than zoned lighting, but " +
				"has lower baseline performance requirements.", true);
		    prev = grp.add("Zoned", prev.pos("bl").adds(0, 2));
		    prev.settip("Zoned lighting supports far more light sources than global " +
				"lighting with better performance, but may have higher performance " +
				"requirements in cases with few light sources, and may also have " +
				"issues on old graphics hardware.", true);
		    grp.check(prefs.lightmode.val.ordinal());
		    done[0] = true;
		}
		prev = add(new Label("Light-source limit"), prev.pos("bl").adds(0, 5).x(0));
		{
		    Label dpy = new Label("");
		    int val = prefs.maxlights.val;
		    if(val == 0) {    /* XXX: This is just ugly. */
			if(prefs.lightmode.val == GSettings.LightMode.ZONED)
			    val = Lighting.LightGrid.defmax;
			else
			    val = Lighting.SimpleLights.defmax;
		    }
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, 32, val / 4) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(Integer.toString(this.val * 4));
			       }
			       public void changed() {dpy();}
			       public void fchanged() {
				   try {
				       ui.setgprefs(prefs = prefs.update(null, prefs.maxlights, this.val * 4));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			       {
				   settip("The light-source limit means different things depending on the " +
					  "selected lighting mode. For Global lighting, it limits the total "+
					  "number of light-sources globally. For Zoned lighting, it limits the " +
					  "total number of overlapping light-sources at any point in space.",
					  true);
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Frame sync mode"), prev.pos("bl").adds(0, 5).x(0));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs.update(null, prefs.syncmode, JOGLPanel.SyncMode.values()[btn]));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
			    }
			};
		    prev = add(new Label("\u2191 Better performance, worse latency"), prev.pos("bl").adds(5, 2));
		    prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
		/* XXXRENDER
		composer.add(new CheckBox("Antialiasing") {
			{a = cf.fsaa.val;}

			public void set(boolean val) {
			    try {
				cf.fsaa.set(val);
			    } catch(GLSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			    cf.dirty = true;
			}
		    });
		composer.add(new Label("Anisotropic filtering"));
		if(cf.anisotex.max() <= 1) {
		    composer.add(new Label("(Not supported)"));
		} else {
		    final Label dpy = new Label("");
		    composer.addRow(
			    new HSlider(UI.scale(160), (int)(cf.anisotex.min() * 2), (int)(cf.anisotex.max() * 2), (int)(cf.anisotex.val * 2)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(val < 2)
				    dpy.settext("Off");
				else
				    dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
			    }
			    public void changed() {
				try {
				    cf.anisotex.set(val / 2.0f);
				} catch(GLSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
				cf.dirty = true;
			    }
			},
			dpy
		    );
		}
		*/
		add(new Button(UI.scale(200), "Reset to defaults", false).action(() -> {
			    ui.setgprefs(GSettings.defaults());
			    curcf.destroy();
			    curcf = null;
		}), prev.pos("bl").adds(0, 5).x(0));
		pack();
	    }
	}

	public void draw(GOut g) {
	    if((curcf == null) || (ui.gprefs != curcf.prefs))
		resetcf();
	    super.draw(g);
	}

	private void resetcf() {
	    if(curcf != null)
		curcf.destroy();
	    curcf = add(new CPanel(ui.gprefs), 0, 0);
	    back.move(curcf.pos("bl").adds(0, 15));
	    pack();
	}
    }

    public class AudioPanel extends Panel {
	public AudioPanel(Panel back) {
	    prev = add(new Label("Master audio volume"), 0, 0);
	    prev = add(new HSlider(UI.scale(200), 0, 1000, (int)(Audio.volume * 1000)) {
		    public void changed() {
			Audio.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Interface sound volume"), prev.pos("bl").adds(0, 15));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.aui.volume * 1000);
		    }
		    public void changed() {
			ui.audio.aui.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("In-game event volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.pos.volume * 1000);
		    }
		    public void changed() {
			ui.audio.pos.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Ambient volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(200), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.amb.volume * 1000);
		    }
		    public void changed() {
			ui.audio.amb.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    add(new PButton(UI.scale(200), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 30));
	    pack();
	}
    }

	private CheckBox enableCornerFPSCheckBox;
	private Label granularityPositionLabel;
	private Label granularityAngleLabel;
	public static CheckBox toggleQualityDisplayCheckBox;
	public static CheckBox toggleGobHealthDisplayCheckBox;
	public static CheckBox toggleGobCollisionBoxesDisplayCheckBox;
	public static CheckBox alwaysOpenBeltCheckBox;
	public static CheckBox showQuickSlotsBar;
    public class InterfacePanel extends Panel {

	public InterfacePanel(Panel back) {

	    Widget prev = add(new Label("Interface scale (requires restart)"), 0, 0);
	    {
		Label dpy = new Label("");
		final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		final int steps = (int)Math.round((smax - smin) / 0.25);
		addhlp(prev.pos("bl").adds(0, 4), UI.scale(5),
		       prev = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", smin + (((double)this.val / steps) * (smax - smin))));
			       }
			       public void changed() {
				   double val = smin + (((double)this.val / steps) * (smax - smin));
				   Utils.setprefd("uiscale", val);
				   dpy();
			       }
			   },
		       dpy);
	    }
	    prev = add(new Label("Object fine-placement granularity"), prev.pos("bl").adds(0, 5));
	    {
		Label pos = add(granularityPositionLabel = new Label("Position"), prev.pos("bl").adds(5, 4));
		Label ang = add(granularityAngleLabel= new Label("Angle"), pos.pos("bl").adds(0, 4));
		int x = Math.max(pos.pos("ur").x, ang.pos("ur").x);
		{
		    Label dpy = new Label("");
		    final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		    final int steps = (int)Math.round((smax - smin) / 0.25);
		    int ival = (int)Math.round(MapView.plobpgran);
		    addhlp(Coord.of(x + UI.scale(5), pos.c.y), UI.scale(5),
			   prev = new HSlider(UI.scale(155 - x), 2, 65, (ival == 0) ? 65 : ival) {
				   protected void added() {
				       dpy();
				   }
				   void dpy() {
				       dpy.settext((this.val == 65) ? "\u221e" : Integer.toString(this.val));
				   }
				   public void changed() {
				       Utils.setprefd("plobpgran", MapView.plobpgran = ((this.val == 65) ? 0 : this.val));
				       dpy();
				   }
			       },
			   dpy);
		}
		{
		    Label dpy = new Label("");
		    final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		    final int steps = (int)Math.round((smax - smin) / 0.25);
		    int[] vals = {4, 5, 6, 8, 9, 10, 12, 15, 18, 20, 24, 30, 36, 40, 45, 60, 72, 90, 120, 180, 360};
		    int ival = 0;
		    for(int i = 0; i < vals.length; i++) {
			if(Math.abs((MapView.plobagran * 2) - vals[i]) < Math.abs((MapView.plobagran * 2) - vals[ival]))
			    ival = i;
		    }
		    addhlp(Coord.of(x + UI.scale(5), ang.c.y), UI.scale(5),
			   prev = new HSlider(UI.scale(155 - x), 0, vals.length - 1, ival) {
				   protected void added() {
				       dpy();
				   }
				   void dpy() {
				       dpy.settext(String.format("%d\u00b0", 360 / vals[this.val]));
				   }
				   public void changed() {
				       Utils.setprefd("plobagran", MapView.plobagran = (vals[this.val] / 2.0));
				       dpy();
				   }
			       },
			   dpy);
		}
	    }
		prev = add(new Label("Advanced Interface Settings"), prev.pos("bl").adds(0, 10).x(0));
		prev = add(enableCornerFPSCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("CornerFPSSettingBool", false));}
			public void set(boolean val) {
				GLPanel.Loop.enableCornerFPSSetting = val;
				Utils.setprefb("CornerFPSSettingBool", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 6));
		prev = add(alwaysOpenBeltCheckBox = new CheckBox("Always open belt on login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", false));}
			public void set(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 6));
		prev = add(showQuickSlotsBar = new CheckBox("Show Quick Slots Widget"){
			{a = (Utils.getprefb("showQuickSlotsBar", true));}
			public void set(boolean val) {
				Utils.setprefb("showQuickSlotsBar", val);
				if (gameui() != null && gameui().quickslots != null){
					if (val)
						gameui().quickslots.show();
					else
						gameui().quickslots.hide();
				}
				a = val;
			}
		}, prev.pos("bl").adds(0, 6));
		prev = add(toggleQualityDisplayCheckBox = new CheckBox("Display Quality on Items"){
			{a = (Utils.getprefb("qtoggle", true));}
			public void set(boolean val) {
				Utils.setprefb("qtoggle", val);
				Quality.show = val;
				a = val;
			}
		}, prev.pos("bl").adds(0, 6));
		prev = add(toggleGobHealthDisplayCheckBox = new CheckBox("Display Health Percentage on Objects"){
			{a = (Utils.getprefb("gobHealthDisplayToggle", true));}
			public void set(boolean val) {
				Utils.setprefb("gobHealthDisplayToggle", val);
				GobHealthInfo.displayHealthPercentage = val;
				a = val;
			}
		}, prev.pos("bl").adds(0, 16));
		prev = add(toggleGobCollisionBoxesDisplayCheckBox = new CheckBox("Display Collision Box on Objects"){
			{a = (Utils.getprefb("gobCollisionBoxesDisplayToggle", false));}
			public void set(boolean val) {
				Utils.setprefb("gobCollisionBoxesDisplayToggle", val);
				Gob.showCollisionBoxes = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated);
				a = val;
			}
		}, prev.pos("bl").adds(0, 6));

		add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 30).x(0));
		setTooltipsForInterfaceSettingsStuff();
	    pack();
	}
    }

    private static final Text kbtt = RichText.render("$col[255,255,0]{Escape}: Cancel input\n" +
						     "$col[255,255,0]{Backspace}: Revert to default\n" +
						     "$col[255,255,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public BindingPanel(Panel back) {
			super();
			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 380))), 0, 0);
			Widget cont = scroll.cont;
			Widget prev;
			int y = 0;
			y = cont.adda(new Label("Main menu"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Inventory", GameUI.kb_inv, y);
			y = addbtn(cont, "Equipment", GameUI.kb_equ, y);
			y = addbtn(cont, "Character sheet", GameUI.kb_chr, y);
			y = addbtn(cont, "Map window", GameUI.kb_map, y);
			y = addbtn(cont, "Kith & Kin", GameUI.kb_bud, y);
			y = addbtn(cont, "Options", GameUI.kb_opt, y);
			y = addbtn(cont, "Search actions", GameUI.kb_srch, y);
			y = addbtn(cont, "Toggle chat", GameUI.kb_chat, y);
			y = addbtn(cont, "Quick chat", ChatUI.kb_quick, y);
			y = addbtn(cont, "Take screenshot", GameUI.kb_shoot, y);
			y = addbtn(cont, "Minimap icons", GameUI.kb_ico, y);
			y = addbtn(cont, "Toggle UI", GameUI.kb_hide, y);
			y = addbtn(cont, "Log out", GameUI.kb_logout, y);
			y = addbtn(cont, "Switch character", GameUI.kb_switchchr, y);
			y = cont.adda(new Label("Map Buttons"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Reset view", MapWnd.kb_home, y);
			y = addbtn(cont, "Compact mode", MapWnd.kb_compact, y);
			y = addbtn(cont, "Show personal claims", MapWnd.kb_claim, y);
			y = addbtn(cont, "Show village claims", MapWnd.kb_vil, y);
			y = addbtn(cont, "Show provinces", MapWnd.kb_prov, y);
			y = addbtn(cont, "Hide markers", MapWnd.kb_hmark, y);
			y = addbtn(cont, "Add marker", MapWnd.kb_mark, y);
			y = cont.adda(new Label("World Toggles"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Display claims", GameUI.kb_claim, y);
			y = addbtn(cont, "Display villages", GameUI.kb_vil, y);
			y = addbtn(cont, "Display realms", GameUI.kb_rlm, y);
			y = addbtn(cont, "Display grid-lines", MapView.kb_grid, y);
			y = cont.adda(new Label("Camera control"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			//y = addbtn(cont, "Rotate left", MapView.kb_camleft, y);
			//y = addbtn(cont, "Rotate right", MapView.kb_camright, y);
			//y = addbtn(cont, "Zoom in", MapView.kb_camin, y);
			//y = addbtn(cont, "Zoom out", MapView.kb_camout, y);
			y = addbtn(cont, "Snap North", MapView.kb_camSnapNorth, y);
			y = addbtn(cont, "Snap South", MapView.kb_camSnapSouth, y);
			y = addbtn(cont, "Snap East", MapView.kb_camSnapEast, y);
			y = addbtn(cont, "Snap West", MapView.kb_camSnapWest, y);
			y = addbtn(cont, "Reset", MapView.kb_camreset, y);
			y = cont.adda(new Label("Walking speed"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Increase speed", Speedget.kb_speedup, y);
			y = addbtn(cont, "Decrease speed", Speedget.kb_speeddn, y);
			for (int i = 0; i < 4; i++)
				y = addbtn(cont, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i], y);
			y = cont.adda(new Label("Combat actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < Fightsess.kb_acts.length; i++)
				y = addbtn(cont, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i], y);
			y = addbtn(cont, "Switch targets", Fightsess.kb_relcycle, y);
			y = cont.adda(new Label("Other Custom features"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Drink Button", GameUI.kb_drinkButton, y);
			y = addbtn(cont, "'Attack!' Cursor", GameUI.kb_aggroButton, y);
			y = addbtn(cont, "Left Hand (Quick switch)", GameUI.kb_leftQuickSlotButton, y+6);
			y = addbtn(cont, "Right Hand (Quick switch)", GameUI.kb_rightQuickSlotButton, y);
			y = addbtn(cont, "Toggle Animal Autopeace", GameUI.kb_toggleCombatAutoPeace, y+6);
			y = addbtn(cont, "Peace Current Target", GameUI.kb_peaceCurrentTarget, y);
			y = addbtn(cont, "Toggle Collision Boxes", GameUI.kb_toggleCollisionBoxes, y+6);
			y = addbtn(cont, "Toggle Object Hiding", GameUI.kb_toggleHidingBoxes, y);
			y = addbtn(cont, "Click Nearest Non-Visitor Gate", GameUI.kb_clickNearestGate, y);
			prev = adda(new PointBind(UI.scale(200)), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
			prev = adda(new PButton(UI.scale(200), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
			pack();
		}
	}

	public class NDActionBarSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public NDActionBarSettingsPanel(Panel back) {
			Widget prev;
			prev = add(new CheckBox("Enable Horizontal Action Bar 1"){
				{a = Utils.getprefb("showActionBar1", true);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar1", val);
					if (gameui() != null && gameui().actionBar1 != null){
						if (val)
							gameui().actionBar1.show();
						else
							gameui().actionBar1.hide();
					}
					a = val;
				}
			}, 0, 0);
			prev = add(new CheckBox("Enable Horizontal Action Bar 2"){
				{a = Utils.getprefb("showActionBar2", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar2", val);
					if (gameui() != null && gameui().actionBar2 != null){
						if (val)
							gameui().actionBar2.show();
						else
							gameui().actionBar2.hide();
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			prev = add(new CheckBox("Enable Vertical Action Bar 1"){
				{a = Utils.getprefb("showActionBar3", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar3", val);
					if (gameui() != null && gameui().actionBar3 != null){
						if (val)
							gameui().actionBar3.show();
						else
							gameui().actionBar3.hide();
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			prev = add(new CheckBox("Enable Vertical Action Bar 2"){
				{a = Utils.getprefb("showActionBar4", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar4", val);
					if (gameui() != null && gameui().actionBar4 != null){
						if (val)
							gameui().actionBar4.show();
						else
							gameui().actionBar4.hide();
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(280, 380))), prev.pos("bl").adds(0,10));
			Widget cont = scroll.cont;

			int y = 0;
			y = cont.adda(new Label("Horizontal Action Bar 1 Keybinds"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar1.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar1[i], y);
			y = cont.adda(new Label("Horizontal Action Bar 2 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar2.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar2[i], y);
			y = cont.adda(new Label("Vertical Action Bar 1"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar3.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar3[i], y);
			y = cont.adda(new Label("Vertical Action Bar 2"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar4.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar4[i], y);
			adda(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
			pack();
		}
	}

	private static Label nightVisionLabel;
	private static HSlider nightVisionSlider;
	private static Button nightVisionResetButton;
	private static CheckBox simpleCropsCheckBox;
	public static boolean simplifiedCrops = Utils.getprefb("simplifiedCrops", false);
	private static CheckBox simpleForageablesCheckBox;
	public static boolean simplifiedForageables = Utils.getprefb("simplifiedForageables", false);
	private static CheckBox disableWeatherEffectsCheckBox;
	private static CheckBox disableFlavourObjectsCheckBox;
	private static CheckBox flatWorldCheckBox;
	private static CheckBox tileSmoothingCheckBox;
	private static CheckBox tileTransitionsCheckBox;
	public static boolean disableFlavourObjects = Utils.getprefb("disableFlavourObjects", false);
	public static boolean flatWorldSetting = Utils.getprefb("flatWorld", false);
	public static boolean noTileSmoothing = Utils.getprefb("noTileSmoothing", false);
	public static boolean noTileTransitions = Utils.getprefb("noTileTransitions", false);
	public class NDGraphicsSettingsPanel extends Panel {
		public NDGraphicsSettingsPanel(Panel back) {
			Widget prev;
			add(new Label(""), 278, 0); // To fix window width

			prev = add(nightVisionLabel = new Label("Night Vision / Brighter World:"), 0, 0);
			Glob.nightVisionBrightness = Utils.getprefd("nightVisionSetting", 0.0);
			prev = add(nightVisionSlider = new HSlider(UI.scale(200), 0, 650, (int)(Glob.nightVisionBrightness*1000)) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = (int)(Glob.nightVisionBrightness*1000);
				}
				public void changed() {
					Glob.nightVisionBrightness = val/1000.0;
					Utils.setprefd("nightVisionSetting", val/1000.0);
					if(ui.sess != null && ui.sess.glob != null) {
						ui.sess.glob.brighten();
					}
				}
			}, prev.pos("bl").adds(0, 6));
			add(nightVisionResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				Glob.nightVisionBrightness = 0.0;
				nightVisionSlider.val = 0;
				Utils.setprefd("nightVisionSetting", 0.0);
				if(ui.sess != null && ui.sess.glob != null) {
					ui.sess.glob.brighten();
				}
			}), prev.pos("bl").adds(210, -20));
			prev = add(disableWeatherEffectsCheckBox = new CheckBox("Disable Weather (Requires Relog)"){
				{a = Utils.getprefb("isWeatherDisabled", false);}
				public void set(boolean val) {
					Utils.setprefb("isWeatherDisabled", val);
					MapView.isWeatherDisabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(disableFlavourObjectsCheckBox = new CheckBox("Disable Flavour Objects (Requires Relog)"){
				{a = Utils.getprefb("disableFlavourObjects", false);}
				public void set(boolean val) {
					Utils.setprefb("disableFlavourObjects", val);
					disableFlavourObjects = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(simpleCropsCheckBox = new CheckBox("Simplified Crops (Requires Relog)"){
				{a = Utils.getprefb("simplifiedCrops", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedCrops", val);
					simplifiedCrops = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(simpleForageablesCheckBox = new CheckBox("Simplified Forageables (Requires Relog)"){
				{a = Utils.getprefb("simplifiedForageables", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedForageables", val);
					simplifiedForageables = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(flatWorldCheckBox = new CheckBox("Flat World (Requires Relog)"){
				{a = Utils.getprefb("flatWorld", false);}
				public void set(boolean val) {
					Utils.setprefb("flatWorld", val);
					flatWorldSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 16));

			prev = add(tileSmoothingCheckBox = new CheckBox("Disable Tile Smoothing (Requires Relog)"){
				{a = Utils.getprefb("noTileSmoothing", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileSmoothing", val);
					noTileSmoothing = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			prev = add(tileTransitionsCheckBox = new CheckBox("Disable Tile Transitions (Requires Relog)"){
				{a = Utils.getprefb("noTileTransitions", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileTransitions", val);
					noTileTransitions = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(40)));
			setTooltipsForGraphicsSettingsStuff();
			pack();
		}
	}

	//ND: Set the variables for the camera menu things
	private static Label freeCamZoomSpeedLabel;
	private static HSlider freeCamZoomSpeedSlider;
	private static Button freeCamZoomSpeedResetButton;
	private static Label freeCamHeightLabel;
	private static HSlider freeCamHeightSlider;
	private static Button freeCamHeightResetButton;
	private static CheckBox unlockedOrthoCamCheckBox;
	private static Label orthoCamZoomSpeedLabel;
	private static HSlider orthoCamZoomSpeedSlider;
	private static Button orthoCamZoomSpeedResetButton;
	private static CheckBox revertCameraAxisCheckBox;
	private static CheckBox allowLowerFreeCamTilt;
	public class NDCamSettingsPanel extends Panel {

		public NDCamSettingsPanel(Panel back) {
			Widget prev; // ND: this will be visible with both camera type settings
			Widget FreePrev; // ND: used to calculate the positions for the NDFree camera settings
			Widget OrthoPrev; // ND: used to calculate the positions for the NDOrtho camera settings

			MapView.NDrevertTheAxis(Utils.getprefb("CamAxisSettingBool", true));
			MapView.isometricNDOrtho = !Utils.getprefb("unlockedNDOrtho", true);
			MapView.freeCamTiltBool = Utils.getprefb("allowLowerTiltBool", false);

			prev = add(new Label(""), 0, 0);

			prev = add(new Label("Selected Camera Settings:"), prev.pos("bl").adds(0, 50));
			prev = add(revertCameraAxisCheckBox = new CheckBox("Revert Camera Look Axes"){
				{a = (Utils.getprefb("CamAxisSettingBool", true));}
				public void set(boolean val) {
					Utils.setprefb("CamAxisSettingBool", val);
					MapView.NDrevertTheAxis(val);
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			OrthoPrev = add(unlockedOrthoCamCheckBox = new CheckBox("Unlocked Ortho Camera"){
				{a = Utils.getprefb("unlockedNDOrtho", true);}
				public void set(boolean val) {
					Utils.setprefb("unlockedNDOrtho", val);
					MapView.isometricNDOrtho = !val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			OrthoPrev = add(orthoCamZoomSpeedLabel = new Label("Ortho Camera Zoom Speed:"), OrthoPrev.pos("bl").adds(-16, 10));
			MapView.orthoCameraZoomSpeed = Utils.getprefi("orthoCamZoomSpeed", 10);
			OrthoPrev = add(orthoCamZoomSpeedSlider = new HSlider(UI.scale(200), 2, 40, MapView.orthoCameraZoomSpeed) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = MapView.orthoCameraZoomSpeed;
				}
				public void changed() {
					MapView.orthoCameraZoomSpeed = val;
					Utils.setprefi("orthoCamZoomSpeed", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 4));
			add(orthoCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				MapView.orthoCameraZoomSpeed = 10;
				orthoCamZoomSpeedSlider.val = 10;
				Utils.setprefi("orthoCamZoomSpeed", 10);
			}), OrthoPrev.pos("bl").adds(210, -20));

			//ND: Now the free camera settings
			FreePrev = add(allowLowerFreeCamTilt = new CheckBox("Enable Lower Tilting Angle"){
				{a = (Utils.getprefb("allowLowerTiltBool", false));}
				public void set(boolean val) {
					MapView.freeCamTiltBool = val;
					Utils.setprefb("allowLowerTiltBool", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			FreePrev = add(freeCamZoomSpeedLabel = new Label("Free Camera Zoom Speed:"), FreePrev.pos("bl").adds(-16, 10));
			MapView.freeCameraZoomSpeed = Utils.getprefi("freeCamZoomSpeed", 25);
			FreePrev = add(freeCamZoomSpeedSlider = new HSlider(UI.scale(200), 4, 40, MapView.freeCameraZoomSpeed) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = MapView.freeCameraZoomSpeed;
				}
				public void changed() {
					MapView.freeCameraZoomSpeed = val;
					Utils.setprefi("freeCamZoomSpeed", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				MapView.freeCameraZoomSpeed = 25;
				freeCamZoomSpeedSlider.val = 25;
				Utils.setprefi("freeCamZoomSpeed", 25);
			}), FreePrev.pos("bl").adds(210, -20));
			FreePrev = add(freeCamHeightLabel = new Label("Free Camera Height:"), FreePrev.pos("bl").adds(0, 10));
			MapView.cameraHeightDistance = (float) Utils.getprefd("cameraHeightDistance", 15f);
			FreePrev = add(freeCamHeightSlider = new HSlider(UI.scale(200), 10, 300, (Math.round(MapView.cameraHeightDistance))*10) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = (Math.round(MapView.cameraHeightDistance))*10;
				}
				public void changed() {
					float tempVal = val;
					MapView.cameraHeightDistance = (tempVal/10);
					Utils.setprefd("cameraHeightDistance", (tempVal/10));
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamHeightResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				MapView.cameraHeightDistance = 15f;
				freeCamHeightSlider.val = 150;
				Utils.setprefd("cameraHeightDistance", 15f);
			}), FreePrev.pos("bl").adds(210, -20));
			add(new Label(""), 278, 0); // ND: added this so the window's width does not change when switching camera type and closing/reopening the panel
			prev = add(new Label("Selected Camera Type:"), 0, 0);{
				boolean[] done = {false};
				RadioGroup camGrp = new RadioGroup(this) {
					public void changed(int btn, String lbl) {
						if(!done[0])
							return;
						try {
							if(btn==0) {
								Utils.setpref("defcam", "NDFree");
								setFreeCameraSettingsVisibility(true);
								setOrthoCameraSettingsVisibility(false);
								MapView.publicCurrentCameraName = 1;
								MapView.publicFreeCamDist = 500.0f;
								if (gameui() != null && gameui().map != null) {
									gameui().map.setcam("NDFree");
								}
							}
							if(btn==1) {
								Utils.setpref("defcam", "NDOrtho");
								setFreeCameraSettingsVisibility(false);
								setOrthoCameraSettingsVisibility(true);
								MapView.publicCurrentCameraName = 2;
								MapView.publicOrthoCamDist = 150f;
								if (gameui() != null && gameui().map != null) {
									gameui().map.setcam("NDOrtho");
								}
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
				prev = camGrp.add("Free Camera", prev.pos("bl").adds(16, 6));
				prev = camGrp.add("Ortho Camera", prev.pos("bl").adds(0, 5));

				String startupSelectedCamera = Utils.getpref("defcam", "NDFree");
				if (startupSelectedCamera.equals("NDFree") || startupSelectedCamera.equals("bad") || startupSelectedCamera.equals("worse") || startupSelectedCamera.equals("follow")){
					camGrp.check(0);
					Utils.setpref("defcam", "NDFree");
					setFreeCameraSettingsVisibility(true);
					setOrthoCameraSettingsVisibility(false);
					MapView.publicCurrentCameraName = 1;
					MapView.publicFreeCamDist = 500.0f;
				}
				else {
					camGrp.check(1);
					Utils.setpref("defcam", "NDOrtho");
					setFreeCameraSettingsVisibility(false);
					setOrthoCameraSettingsVisibility(true);
					MapView.publicCurrentCameraName = 2;
					MapView.publicOrthoCamDist = 150f;
				}
				done[0] = true;
			}


			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), FreePrev.pos("bl").adds(0, 18).x(UI.scale(40)));
			setTooltipsForCameraSettingsStuff();
			pack();
		}
	}

	private static Label defaultSpeedLabel;
	private static CheckBox instantFlowerMenuCTRLCheckBox;
	private static CheckBox autoswitchBunnyPlateBootsCheckBox;
	public static CheckBox saveCutleryCheckBox = null;
	public static boolean instantFlowerMenuCTRL = Utils.getprefb("instantFlowerMenuCTRL", true);
	public static boolean autoswitchBunnyPlateBoots = Utils.getprefb("autoswitchBunnyPlateBoots", true);
	public static boolean antiCutleryBreakage = Utils.getprefb("antiCutleryBreakage", true);

	public class NDGameplaySettingsPanel extends Panel {
		private final List<String> runSpeeds = Arrays.asList("Crawl", "Walk", "Run", "Sprint");
		private final int speedSetInt = Utils.getprefi("defaultSetSpeed", 2);
		public NDGameplaySettingsPanel(Panel back) {
			Widget prev;
			add(new Label(""), 298, 0); // ND: To fix window width

			prev = add(new Label("Toggle on Login:"), 0, 0);
			prev = add(new CheckBox("Tracking"){
				{a = Utils.getprefb("toggleTrackingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleTrackingOnLogin", val);
					MenuGrid.toggleTrackingOnLogin = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(new CheckBox("Swimming"){
				{a = Utils.getprefb("toggleSwimmingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleSwimmingOnLogin", val);
					MenuGrid.toggleSwimmingOnLogin = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(new CheckBox("Criminal Acts"){
				{a = Utils.getprefb("toggleCriminalActsOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleCriminalActsOnLogin", val);
					MenuGrid.toggleCriminalActsOnLogin = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(new CheckBox("Check for Siege Engines"){
				{a = Utils.getprefb("toggleSiegeEnginesOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleSiegeEnginesOnLogin", val);
					MenuGrid.toggleSiegeEnginesOnLogin = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(defaultSpeedLabel = new Label("Default Speed:"), prev.pos("bl").adds(0, 10).x(0));
			add(new Dropbox<String>(runSpeeds.size(), runSpeeds) {
					{
						super.change(runSpeeds.get(speedSetInt));
					}
					@Override
					protected String listitem(int i) {
						return runSpeeds.get(i);
					}
					@Override
					protected int listitems() {
						return runSpeeds.size();
					}
					@Override
					protected void drawitem(GOut g, String item, int i) {
						g.text(item, Coord.z);
					}
					@Override
					public void change(String item) {
						super.change(item);
						for (int i = 0; i < runSpeeds.size(); i++){
							if (item.equals(runSpeeds.get(i))){
								Utils.setprefi("defaultSetSpeed", i);
							}
						}
					}
				}, prev.pos("bl").adds(80, -14));

			prev = add(new Label("Altered gameplay behavior:"), prev.pos("bl").adds(0, 6).x(0));
			prev = add(instantFlowerMenuCTRLCheckBox = new CheckBox("Instantly select 1st flower menu option when holding CTRL"){
				{a = Utils.getprefb("instantFlowerMenuCTRL", true);}
				public void set(boolean val) {
					Utils.setprefb("instantFlowerMenuCTRL", val);
					instantFlowerMenuCTRL = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(autoswitchBunnyPlateBootsCheckBox = new CheckBox("Autoswitch Bunny Slippers and Plate Boots from inventory"){
				{a = Utils.getprefb("autoswitchBunnyPlateBoots", true);}
				public void set(boolean val) {
					Utils.setprefb("autoswitchBunnyPlateBoots", val);
					autoswitchBunnyPlateBoots = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));

			prev = add(saveCutleryCheckBox = new CheckBox("Anti Cutlery Breakage (move to inventory before it breaks)"){
				{a = Utils.getprefb("antiCutleryBreakage", true);}
				public void set(boolean val) {
					Utils.setprefb("antiCutleryBreakage", val);
					antiCutleryBreakage = val;
					TableInfo.saveCutleryCheckBox.a = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(50)));
			setTooltipsForGameplaySettingsStuff();
			pack();
		}
	}

	private static HSlider combatUITopPanelHeightSlider;
	private static HSlider combatUIBottomPanelHeightSlider;
	public static CheckBox toggleGobDamageInfoCheckBox;
	public static CheckBox toggleAutoPeaceCheckbox;
	private static Button damageInfoClearButton;
	public class NDCombatSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}
		public NDCombatSettingsPanel(Panel back) {
			Widget prev;

			Fightsess.altui = Utils.getprefb("useProperCombatUI", true);
			Fightsess.showKeybindCombatSetting = Utils.getprefb("showCombatHotkeysUI", true);
			Fightsess.markCombatTargetSetting = Utils.getprefb("markCurrentCombatTarget", true);
			Fightview.autoPeaceSetting = Utils.getprefb("autoPeaceCombat", false);
			GobDamageInfo.toggleGobDamageInfo = Utils.getprefb("GobDamageInfoToggled", true);
			GobDamageInfo.toggleGobDamageInfoWounds = Utils.getprefb("GobDamageInfoWoundsToggled", true);
			GobDamageInfo.toggleGobDamageInfoArmor = Utils.getprefb("GobDamageInfoArmorToggled", true);
			GobDamageInfo.setDamageBackgroundColor(Utils.getprefb("GobDamageInfoBackgroundToggled", false));

			prev = add(new Label("Combat UI:"), 0, 0);
			prev = add(new CheckBox("Use Improved Combat UI"){
				{a = Utils.getprefb("useProperCombatUI", true);}
				public void set(boolean val) {
					Utils.setprefb("useProperCombatUI", val);
					Fightsess.altui = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(new Label("Top panel height:"), prev.pos("bl").adds(-16, 10));
			Fightsess.combaty0HeightInt = Utils.getprefi("combatTopPanelHeight", 400);
			prev = add(combatUITopPanelHeightSlider = new HSlider(UI.scale(200), 1, 500, Fightsess.combaty0HeightInt) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = Fightsess.combaty0HeightInt;
				}
				public void changed() {
					Fightsess.combaty0HeightInt = val;
					Utils.setprefi("combatTopPanelHeight", val);
				}
			}, prev.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Fightsess.combaty0HeightInt = 400;
				combatUITopPanelHeightSlider.val = 400;
				Utils.setprefi("combatTopPanelHeight", 400);
			}), prev.pos("bl").adds(210, -20));
			prev = add(new Label("Bottom panel height:"), prev.pos("bl").adds(0, 10));
			Fightsess.combatbottomHeightInt = Utils.getprefi("combatBottomPanelHeight", 100);
			prev = add(combatUIBottomPanelHeightSlider = new HSlider(UI.scale(200), 1, 500, Fightsess.combatbottomHeightInt) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = Fightsess.combatbottomHeightInt;
				}
				public void changed() {
					Fightsess.combatbottomHeightInt = val;
					Utils.setprefi("combatBottomPanelHeight", val);
				}
			}, prev.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Fightsess.combatbottomHeightInt = 100;
				combatUIBottomPanelHeightSlider.val = 100;
				Utils.setprefi("combatBottomPanelHeight", 100);
			}), prev.pos("bl").adds(210, -20));
			prev = add(new CheckBox("Show hotkeys"){
				{a = Utils.getprefb("showCombatHotkeysUI", true);}
				public void set(boolean val) {
					Utils.setprefb("showCombatHotkeysUI", val);
					Fightsess.showKeybindCombatSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(new Label("Other Combat Settings:"), prev.pos("bl").adds(0, 14).x(0));
			prev = add(toggleAutoPeaceCheckbox = new CheckBox("Autopeace animals when combat starts"){
				{a = Utils.getprefb("autoPeaceCombat", false);}
				public void set(boolean val) {
					Utils.setprefb("autoPeaceCombat", val);
					Fightview.autoPeaceSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 8));
			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(277, 40))), prev.pos("bl").adds(-2, 2));
			Widget cont = scroll.cont;
			addbtn(cont, "Toggle autopeace hotkey:", GameUI.kb_toggleCombatAutoPeace, 0);
			prev = add(new CheckBox("Mark current target"){
				{a = Utils.getprefb("markCurrentCombatTarget", true);}
				public void set(boolean val) {
					Utils.setprefb("markCurrentCombatTarget", val);
					Fightsess.markCombatTargetSetting = val;
					a = val;
				}
			}, scroll.pos("bl").adds(2, -6));
			prev = add(toggleGobDamageInfoCheckBox = new CheckBox("Show damage info:"){
				{a = Utils.getprefb("GobDamageInfoToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoToggled", val);
					GobDamageInfo.toggleGobDamageInfo = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 14));
			prev = add(new Label("> Include:"), prev.pos("bl").adds(18, 3));
			prev = add(new CheckBox("Wounds"){
				{a = Utils.getprefb("GobDamageInfoWoundsToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoWoundsToggled", val);
					GobDamageInfo.toggleGobDamageInfoWounds = val;
					a = val;
				}
			}, prev.pos("bl").adds(56, -13));
			prev = add(new CheckBox("Armor"){
				{a = Utils.getprefb("GobDamageInfoArmorToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoArmorToggled", val);
					GobDamageInfo.toggleGobDamageInfoArmor = val;
					a = val;
				}
			}, prev.pos("bl").adds(62, -14));
			prev = add(new CheckBox("Show damage background"){
				{a = Utils.getprefb("GobDamageInfoBackgroundToggled", false);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoBackgroundToggled", val);
					GobDamageInfo.setDamageBackgroundColor(val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4).x(UI.scale(34)));
			add(damageInfoClearButton = new Button(UI.scale(70), "Clear", false).action(() -> {
				GobDamageInfo.clearAllDamage(gameui());
			}), prev.pos("bl").adds(0, -54).x(UI.scale(210)));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(46)));
			setTooltipsForCombatSettingsStuff();
			pack();
		}
	}

	public static CheckBox toggleGobHidingCheckBox;
	public static CheckBox hideTreesCheckbox;
	public static boolean hideTreesSetting = Utils.getprefb("hideTrees", true);
	public static CheckBox hideBushesCheckbox;
	public static boolean hideBushesSetting = Utils.getprefb("hideBushes", true);
	public static CheckBox hideBouldersCheckbox;
	public static boolean hideBouldersSetting = Utils.getprefb("hideBoulders", true);
	public static CheckBox hideTreeLogsCheckbox;
	public static boolean hideTreeLogsSetting = Utils.getprefb("hideTreeLogs", true);
	public static CheckBox hideWallsCheckbox;
	public static boolean hideWallsSetting = Utils.getprefb("hideWalls", false);
	public static CheckBox hideHousesCheckbox;
	public static boolean hideHousesSetting = Utils.getprefb("hideHouses", false);
	public static CheckBox hideCropsCheckbox;
	public static boolean hideCropsSetting = Utils.getprefb("hideCrops", false);
	public static CheckBox hideStockpilesCheckbox;
	public static boolean hideStockpilesSetting = Utils.getprefb("hideStockpiles", false);


	public class NDHidingSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public NDHidingSettingsPanel(Panel back) {
			Widget prev;
			Widget prev2;
			//add(new Label(""), 298, 0); // ND: To fix window width
			prev = add(toggleGobHidingCheckBox = new CheckBox("Hide Objects"){
				{a = (Utils.getprefb("gobHideObjectsToggle", false));}
				public void set(boolean val) {
					Utils.setprefb("gobHideObjectsToggle", val);
					Gob.hideObjects = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, 0, 10);

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 40))), prev.pos("bl").adds(14, 16));
			Widget cont = scroll.cont;
			addbtn(cont, "Toggle object hiding hotkey:", GameUI.kb_toggleHidingBoxes, 0);

			prev = add(new ColorOptionWidget("Hidden object box color:", "hitboxFilled", 126, 0, 200, 255, 200, (Color col) -> {

				//ND: Update the inner filled box
				HitboxFilled.SOLID_COLOR = col;
				HitboxFilled.SOLID = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);

				// ND: Update the outer box lines
				Color col2 = Hitbox2.SOLID_COLOR = new Color(col.getRed(), col.getGreen(), col.getBlue(), 140);
				Hitbox2.SOLID = Pipe.Op.compose(new BaseColor(col2), new States.LineWidth(Hitbox2.WIDTH));
				Hitbox2.SOLID_TOP = Pipe.Op.compose(new BaseColor(col2), new States.LineWidth(Hitbox2.WIDTH), Hitbox2.TOP);

				// ND: Reload the boxes
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);

			}){}, scroll.pos("bl").adds(1, -2));

			prev = add(new Label("Objects that will be hidden:"), prev.pos("bl").adds(0, 20).x(0));

			prev2 = add(hideTreesCheckbox = new CheckBox("Trees"){
				{a = Utils.getprefb("hideTrees", true);}
				public void set(boolean val) {
					Utils.setprefb("hideTrees", val);
					hideTreesSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(hideBushesCheckbox = new CheckBox("Bushes"){
				{a = Utils.getprefb("hideBushes", true);}
				public void set(boolean val) {
					Utils.setprefb("hideBushes", val);
					hideBushesSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev2.pos("bl").adds(0, 4));

			prev = add(hideBouldersCheckbox = new CheckBox("Boulders"){
				{a = Utils.getprefb("hideBoulders", true);}
				public void set(boolean val) {
					Utils.setprefb("hideBoulders", val);
					hideBouldersSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(hideTreeLogsCheckbox = new CheckBox("Tree Logs"){
				{a = Utils.getprefb("hideTreeLogs", true);}
				public void set(boolean val) {
					Utils.setprefb("hideTreeLogs", val);
					hideTreeLogsSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(hideWallsCheckbox = new CheckBox("Palisades and Brick Walls"){
				{a = Utils.getprefb("hideWalls", false);}
				public void set(boolean val) {
					Utils.setprefb("hideWalls", val);
					hideWallsSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev2.pos("bl").adds(140, -14));

			prev = add(hideHousesCheckbox = new CheckBox("Houses"){
				{a = Utils.getprefb("hideHouses", false);}
				public void set(boolean val) {
					Utils.setprefb("hideHouses", val);
					hideHousesSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(hideCropsCheckbox = new CheckBox("Crops"){
				{a = Utils.getprefb("hideCrops", false);}
				public void set(boolean val) {
					Utils.setprefb("hideCrops", val);
					hideCropsSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(hideStockpilesCheckbox = new CheckBox("Stockpiles"){
				{a = Utils.getprefb("hideStockpiles", false);}
				public void set(boolean val) {
					Utils.setprefb("hideStockpiles", val);
					hideStockpilesSetting = val;
					if (gameui() != null)
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(57)));
			setTooltipsForHidingSettingsStuff();
			pack();
		}
	}

	public class SetButton extends KeyMatch.Capture {
	    public final KeyBinding cmd;

	    public SetButton(int w, KeyBinding cmd) {
		super(w, cmd.key());
		this.cmd = cmd;
	    }

	    public void set(KeyMatch key) {
		super.set(key);
		cmd.set(key);
	    }

	    public void draw(GOut g) {
		if(cmd.key() != key)
		    super.set(cmd.key());
		super.draw(g);
	    }

	    protected KeyMatch mkmatch(KeyEvent ev) {
		return(KeyMatch.forevent(ev, ~cmd.modign));
	    }

	    protected boolean handle(KeyEvent ev) {
		if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    cmd.set(null);
		    super.set(cmd.key());
		    return(true);
		}
		return(super.handle(ev));
	    }

	    public Object tooltip(Coord c, Widget prev) {
		return(kbtt.tex());
	    }
	}



    public static class PointBind extends Button {
	public static final String msg = "Bind other elements...";
	public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
	private UI.Grab mg, kg;
	private KeyBinding cmd;

	public PointBind(int w) {
	    super(w, msg, false);
	    tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
				      "button. Click the element to bind, and then press the key to bind to it. " +
				      "Right-click to stop rebinding.",
				      300);
	}

	public void click() {
	    if(mg == null) {
		change("Click element...");
		mg = ui.grabmouse(this);
	    } else if(kg != null) {
		kg.remove();
		kg = null;
		change(msg);
	    }
	}

	private boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		return(true);
	    }
	    if(code == KeyEvent.VK_BACK_SPACE) {
		cmd.set(null);
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		cmd.set(KeyMatch.nil);
		return(true);
	    }
	    KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
	    if(key != null)
		cmd.set(key);
	    return(true);
	}

	public boolean mousedown(Coord c, int btn) {
	    if(mg == null)
		return(super.mousedown(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
		return(true);
	    }
	    if(btn == 3) {
		mg.remove();
		mg = null;
		change(msg);
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(Coord c, int btn) {
	    if(mg == null)
		return(super.mouseup(c, btn));
	    Coord gc = ui.mc;
	    if(btn == 1) {
		if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
		    mg.remove();
		    mg = null;
		    kg = ui.grabkeys(this);
		    change("Press key...");
		} else {
		    this.cmd = null;
		}
		return(true);
	    }
	    if(btn == 3)
		return(true);
	    return(false);
	}

	public Resource getcurs(Coord c) {
	    if(mg == null)
		return(null);
	    return(curs);
	}

	public boolean keydown(KeyEvent ev) {
	    if(kg == null)
		return(super.keydown(ev));
	    if(handle(ev)) {
		kg.remove();
		kg = null;
		cmd = null;
		change("Click another element...");
		mg = ui.grabmouse(this);
	    }
	    return(true);
	}
    }


    public OptWnd(boolean gopts) {
	super(Coord.z, "Options            ", true); // ND: Added a bunch of spaces to the caption(title) in order avoid text cutoff when changing it
	main = add(new Panel());
	Panel video = add(new VideoPanel(main));
	Panel audio = add(new AudioPanel(main));
	Panel keybind = add(new BindingPanel(main));
	advancedSettings = add(new Panel());
	// ND: Make the advanced settings panel here. Don't really understand why I have to do it like this, and I don't really care either at this point, it's just buttons.
	// IF IT WORKS, IT WORKS.
		Panel iface = add(new InterfacePanel(advancedSettings));
		Panel graphicssettings = add(new NDGraphicsSettingsPanel(advancedSettings));
		Panel actionbarsettings = add(new NDActionBarSettingsPanel(advancedSettings));
		Panel camsettings = add(new NDCamSettingsPanel(advancedSettings));
		Panel gameplaysettings = add(new NDGameplaySettingsPanel(advancedSettings));
		Panel combatsettings = add(new NDCombatSettingsPanel(advancedSettings));
		Panel hidingsettings = add(new NDHidingSettingsPanel(advancedSettings));

		int y2 = UI.scale(6);
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Interface Settings", -1, iface, "Interface Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Graphics Settings", -1, graphicssettings, "Graphics Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Action Bars Settings", -1, actionbarsettings, "Action Bars Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Camera Settings", -1, camsettings, "Camera Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Gameplay Settings", -1, gameplaysettings, "Gameplay Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Combat Settings", -1, combatsettings, "Combat Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Hiding Settings", -1, hidingsettings, "Hiding Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Back", 27, main, "Options            "), 0, y2).pos("bl").adds(0, 5).y;
		this.advancedSettings.pack();
	// ND: Continue with the main panel whatever
	int y = UI.scale(6);
	Widget prev;
	y = main.add(new PButton(UI.scale(200), "Video Settings", -1, video, "Video Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Audio Settings", -1, audio, "Audio Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Keybindings", -1, keybind, "Keybindings"), 0, y).pos("bl").adds(0, 25).y;

	y = main.add(new PButton(UI.scale(200), "Advanced Settings", -1, advancedSettings, "Advanced Settings"), 0, y).pos("bl").adds(0, 5).y;

	y += UI.scale(20);
	if(gopts) {
	    y = main.add(new Button(UI.scale(200), "Switch character", false).action(() -> {
			getparent(GameUI.class).act("lo", "cs");
	    }), 0, y).pos("bl").adds(0, 5).y;
	    y = main.add(new Button(UI.scale(200), "Log out", false).action(() -> {
			getparent(GameUI.class).act("lo");
	    }), 0, y).pos("bl").adds(0, 5).y;
	}
	y = main.add(new Button(UI.scale(200), "Close", false).action(() -> {
		    OptWnd.this.hide();
	}), 0, y).pos("bl").adds(0, 5).y;
	this.main.pack();

	chpanel(this.main);
    }
	private void setFreeCameraSettingsVisibility(boolean bool){
		freeCamZoomSpeedLabel.visible = bool;
		freeCamZoomSpeedSlider.visible = bool;
		freeCamZoomSpeedResetButton.visible = bool;
		freeCamHeightLabel.visible = bool;
		freeCamHeightSlider.visible = bool;
		freeCamHeightResetButton.visible = bool;
		allowLowerFreeCamTilt.visible = bool;
	}
	private void setOrthoCameraSettingsVisibility(boolean bool){
		unlockedOrthoCamCheckBox.visible = bool;
		orthoCamZoomSpeedLabel.visible = bool;
		orthoCamZoomSpeedSlider.visible = bool;
		orthoCamZoomSpeedResetButton.visible = bool;
	}
	private void setTooltipsForCameraSettingsStuff(){
		revertCameraAxisCheckBox.tooltip = RichText.render("Enabling this will revert the Vertical and Horizontal axes when dragging the camera to look around.\n$col[185,185,185]{I don't know why Loftar inverts them in the first place...}", 280);
		unlockedOrthoCamCheckBox.tooltip = RichText.render("Enabling this allows you to rotate the Ortho camera freely, without locking it to only 4 view angles.", 280);
		freeCamZoomSpeedResetButton.tooltip = RichText.render("Reset to default", 300);
		freeCamHeightResetButton.tooltip = RichText.render("Reset to default", 300);
		orthoCamZoomSpeedResetButton.tooltip = RichText.render("Reset to default", 300);
		allowLowerFreeCamTilt.tooltip = RichText.render("Enabling this will allow you to tilt the camera below the character and look upwards.\n$col[200,0,0]{WARNING: Be careful when using this setting in combat! You're not able to click on the ground when looking at the world from below.}\n$col[185,185,185]{Honestly just enable this when you need to take a screenshot or something, and keep it disabled the rest of the time. I added this option for fun.}", 300);
		freeCamHeightLabel.tooltip = RichText.render("This affects the height of the point at which the free camera is pointed. By default, it is pointed right above the player's head.\n$col[185,185,185]{This doesn't really affect gameplay that much, if at all. With this setting, you can make it point at the feet, or torso, or head, or whatever.}", 300);
	}

	private void setTooltipsForInterfaceSettingsStuff() {
		enableCornerFPSCheckBox.tooltip = RichText.render("Enabling this will display the current FPS in the top-right corner of the screen.", 300);
		granularityPositionLabel.tooltip = RichText.render("Equivalent of the :placegrid console command, this allows you to have more freedom when placing constructions/objects.", 300);
		granularityAngleLabel.tooltip = RichText.render("Equivalent of the :placeangle console command, this allows you to have more freedom when rotating constructions/objects before placement.", 300);
		alwaysOpenBeltCheckBox.tooltip = RichText.render("Enabling this will cause your belt window to always open when you log in. \n$col[185,185,185]{Note: By default, Loftar saves the status of the belt at logout. So if you don't enable this setting, but leave the belt window open when you log out/exit the game, it will still open on login.}", 300);
		showQuickSlotsBar.tooltip = RichText.render("Note: The Quick Switch keybinds ('Right Hand' and 'Left Hand') will still work, regardless of this widget being visible or not.", 300);
	}

	private void setTooltipsForCombatSettingsStuff(){
		toggleGobDamageInfoCheckBox.tooltip = RichText.render("Enabling this will display the total amount of damage players and animals took.\nNote: The damage you will see saved above players/animals is the total damage you saw the entity take, while inside of your view range. This is not all of the damage said entity might have taken recently.\n$col[185,185,185]{If you change any of the settings below, you will need a damage update in order to see the changes (for example, deal some damage to the player/animal).}", 300);
		damageInfoClearButton.tooltip = RichText.render("Clear all damage info", 300);
		toggleAutoPeaceCheckbox.tooltip = RichText.render("Enabling this will automatically set your status to 'Peace' when combat is initiated with a new target (animals only). Toggling this on while in combat will also autopeace all animals you are currently fighting.\n$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.}", 300);
	}

	private void setTooltipsForGameplaySettingsStuff(){
		defaultSpeedLabel.tooltip = RichText.render("Sets your character's movement speed on login.", 300);
		instantFlowerMenuCTRLCheckBox.tooltip = RichText.render("Enabling this will make holding CTRL before right clicking an item or object instantly select the first available option from the flower menu.", 300);
		autoswitchBunnyPlateBootsCheckBox.tooltip = RichText.render("Enabling this will cause your currently equipped Plate Boots to automatically swap with a pair of bunny slippers from your inventory, whenever you right click to chase a rabbit, and vice versa if you click on anything else or just left click to walk.\n$col[185,185,185]{I don't see many reason for which you'd ever want to disable this setting, but alas, I made it an option.}", 300);
		saveCutleryCheckBox.tooltip = RichText.render("Enabling this will cause any cutlery that has 1 wear left to be instantly transferred from the table into your inventory.\n$col[185,185,185]{A warning message will be shown, to let you know that the item has been transferred.}", 300);
	}

	private void setTooltipsForGraphicsSettingsStuff(){
		nightVisionLabel.tooltip = RichText.render("Increasing this will simulate daytime lighting during the night.\n$col[185,185,185]{It slightly affects the light levels during the day too.}", 280);
		nightVisionResetButton.tooltip = RichText.render("Reset to default", 300);
		disableWeatherEffectsCheckBox.tooltip = RichText.render("Note: This disables *ALL* weather and camera effects, including rain effects, drunkenness distortion, drug high, valhalla gray overlay, camera shake, and any other similar effects.", 300);
		disableFlavourObjectsCheckBox.tooltip = RichText.render("Note: This only disables random objects that appear in the world which you cannot interact with.\n$col[185,185,185]{Players usually disable flavour objects to improve visibility and/or performance.}", 300);
		flatWorldCheckBox.tooltip = RichText.render("Enabling this will make the entire game world terrain flat.\n$col[185,185,185]{Cliffs will still be drawn with their relative height, scaled down.}", 300);
	}

	private void setTooltipsForHidingSettingsStuff(){
		toggleGobHidingCheckBox.tooltip = RichText.render("$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.", 300);
	}

    public OptWnd() {
	this(true);
    }


    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    hide();
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void show() {
	chpanel(main);
	super.show();
    }
}