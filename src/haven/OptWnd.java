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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class OptWnd extends Window {
    public final Panel main;
	public final Panel advancedSettings;
    public Panel current;
	public static int cameraLmaoMessage = 1; // ND: Message for "cam" console command, idk where to put this lmao
	AlarmWindow alarmWindow;

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
		pack(); // ND: Fixes top bar not being fully draggable the first time I open the video panel. Idfk.
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
			   prev = new HSlider(UI.scale(160), -2 * steps, 1 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
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
		    int val = prefs.maxlights.val, max = 32;
		    if(val == 0) {    /* XXX: This is just ugly. */
			if(prefs.lightmode.val == GSettings.LightMode.ZONED)
			    val = Lighting.LightGrid.defmax;
			else
			    val = Lighting.SimpleLights.defmax;
		    }
		    if(prefs.lightmode.val == GSettings.LightMode.SIMPLE)
			max = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, val / 4) {
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
	private CheckBox enableAdvancedMouseInfoCheckBox;
	private CheckBox enableKeepWindowsInsideCheckBox;
	private Label granularityPositionLabel;
	private Label granularityAngleLabel;
	public static CheckBox toggleQualityDisplayCheckBox;
	public static CheckBox toggleGobHealthDisplayCheckBox;
	public static CheckBox toggleGobGrowthInfoCheckBox;
	public static CheckBox toggleGobQualityInfoCheckBox;
	public static CheckBox toggleGobCollisionBoxesDisplayCheckBox;
	public static CheckBox toggleBeastDangerRadiiCheckBox;
	public static CheckBox toggleCritterAurasCheckBox;
	public static CheckBox alwaysOpenBeltCheckBox;
	public static CheckBox showQuickSlotsBar;
	public static CheckBox showContainerFullnessCheckBox;
	public static CheckBox showWorkstationStageCheckBox;
	public static boolean critterAuraEnabled = Utils.getprefb("critterAuras", false);
	public static boolean beastDangerRadiiEnabled = Utils.getprefb("beastDangerRadii", true);
	public static boolean showContainerFullness = Utils.getprefb("showContainerFullness", true);
	public static boolean showWorkstationStage = Utils.getprefb("showWorkstationStage", true);
	public static boolean advancedMouseInfo = Utils.getprefb("advancedMouseInfo", false);
	public static boolean keepWindowsInside = Utils.getprefb("keepWindowsInside", false);
    public class InterfacePanel extends Panel {

	public InterfacePanel(Panel back) {
		Widget prev;
		prev = add(new Label("Interface scale (requires restart)"), UI.scale(0, 0));
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
		prev = add(new Label("Advanced Interface & Display Settings"), prev.pos("bl").adds(0, 16).x(110));
		Widget topRightColumn;
		topRightColumn = add(enableCornerFPSCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("CornerFPSSettingBool", false));}
			public void set(boolean val) {
				GLPanel.Loop.enableCornerFPSSetting = val;
				Utils.setprefb("CornerFPSSettingBool", val);
				a = val;
			}
		}, UI.scale(230, 10));
		topRightColumn = add(enableAdvancedMouseInfoCheckBox = new CheckBox("Show Extended Mouseover Info"){
			{a = (Utils.getprefb("advancedMouseInfo", false));}
			public void set(boolean val) {
				advancedMouseInfo = val;
				Utils.setprefb("advancedMouseInfo", val);
				a = val;
			}
		}, topRightColumn.pos("bl").adds(0, 6));
		topRightColumn = add(enableKeepWindowsInsideCheckBox = new CheckBox("Keep Windows in when Resizing Game"){
			{a = (Utils.getprefb("keepWindowsInside", false));}
			public void set(boolean val) {
				keepWindowsInside = val;
				Utils.setprefb("keepWindowsInside", val);
				a = val;
			}
		}, topRightColumn.pos("bl").adds(0, 6));
		Widget leftColumn = add(alwaysOpenBeltCheckBox = new CheckBox("Always open belt on login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", false));}
			public void set(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 20).x(0));
		leftColumn = add(showQuickSlotsBar = new CheckBox("Show Quick Slots Widget"){
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
		}, leftColumn.pos("bl").adds(0, 6));
		leftColumn = add(toggleQualityDisplayCheckBox = new CheckBox("Display Quality on Items"){
			{a = (Utils.getprefb("qtoggle", true));}
			public void set(boolean val) {
				Utils.setprefb("qtoggle", val);
				Quality.show = val;
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 6));
		leftColumn = add(toggleGobHealthDisplayCheckBox = new CheckBox("Display Object Health Percentage"){
			{a = (Utils.getprefb("gobHealthDisplayToggle", true));}
			public void set(boolean val) {
				Utils.setprefb("gobHealthDisplayToggle", val);
				GobHealthInfo.displayHealthPercentage = val;
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 16));
		leftColumn = add(toggleGobQualityInfoCheckBox = new CheckBox("Show Object Quality on Inspection"){
			{a = (Utils.getprefb("showGobQualityInfo", true));}
			public void set(boolean val) {
				Utils.setprefb("showGobQualityInfo", val);
				GobQualityInfo.showGobQualityInfo = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::qualityInfoUpdated);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 6));
		leftColumn = add(toggleGobGrowthInfoCheckBox = new CheckBox("Display Growth Info on Plants"){
			{a = (Utils.getprefb("showGobGrowthInfo", false));}
			public void set(boolean val) {
				Utils.setprefb("showGobGrowthInfo", val);
				GobGrowthInfo.showGobGrowthInfo = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 6));
		Widget rightColumn = add(toggleGobCollisionBoxesDisplayCheckBox = new CheckBox("Display Collision Box on Objects"){
			{a = (Utils.getprefb("gobCollisionBoxesDisplayToggle", false));}
			public void set(boolean val) {
				Utils.setprefb("gobCollisionBoxesDisplayToggle", val);
				Gob.showCollisionBoxes = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated);
				a = val;
			}
		}, prev.pos("bl").adds(0, 20).x(230));

		rightColumn = add(toggleBeastDangerRadiiCheckBox = new CheckBox("Display Animal Danger Radii"){
			{a = (Utils.getprefb("beastDangerRadii", true));}
			public void set(boolean val) {
				Utils.setprefb("beastDangerRadii", val);
				beastDangerRadiiEnabled = val;
				if (gameui() != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleBeastDangerRadii);
					gameui().msg("Animal danger radii are now " + (val ? "ENABLED" : "DISABLED") + "!");
				}
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 6));

		rightColumn = add(toggleCritterAurasCheckBox = new CheckBox("Display Critter Circle Auras"){
			{a = (Utils.getprefb("critterAuras", false));}
			public void set(boolean val) {
				Utils.setprefb("critterAuras", val);
				critterAuraEnabled = val;
				if (gameui() != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
					gameui().msg("Critter circle auras are now " + (val ? "ENABLED" : "DISABLED") + "!");
				}
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 6));

		rightColumn = add(showContainerFullnessCheckBox = new CheckBox("Show Container Fullness"){
			{a = (Utils.getprefb("showContainerFullness", true));}
			public void set(boolean val) {
				Utils.setprefb("showContainerFullness", val);
				showContainerFullness = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 16));
		rightColumn = add(showWorkstationStageCheckBox = new CheckBox("Show Workstation Progress"){
			{a = (Utils.getprefb("showWorkstationStage", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStage", val);
				showWorkstationStage = val;
				if (gameui() != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 6));

		add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 30).x(101));
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
			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 360))), 0, 0);
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
			y = addbtn(cont, "Focus chat window", GameUI.kb_chat, y);
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
			y = addbtn(cont, "Re-aggro Last Target", GameUI.kb_aggroLastTarget, y);
			y = addbtn(cont, "Toggle Collision Boxes", GameUI.kb_toggleCollisionBoxes, y+6);
			y = addbtn(cont, "Toggle Object Hiding", GameUI.kb_toggleHidingBoxes, y);
			y = addbtn(cont, "Click Nearest Non-Visitor Gate", GameUI.kb_clickNearestGate, y);
			y = addbtn(cont, "Toggle Animal Danger Radii", GameUI.kb_toggleDangerRadii, y+6);
			y = addbtn(cont, "Toggle Critter Circle Auras", GameUI.kb_toggleCritterAuras, y);

			y = addbtn(cont, "Display Vehicle Speed", GameUI.kb_toggleVehicleSpeed, y+6);

			y = addbtn(cont, "Toggle No Water Drop", GameUI.kb_toggleNoWaterDropping, y+6);
			y = addbtn(cont, "Toggle No Drop", GameUI.kb_toggleNoDropping, y);
			y = addbtn(cont, "Mute Non-Friendly", GameUI.kb_toggleMuteNonFriendly, y+6);

			y = addbtn(cont, "Toggle Walk Pathfinder", GameUI.kb_toggleWalkWithPathfinder, y+6);

			y = addbtn(cont, "Button For Testing", GameUI.kb_buttonForTesting, y+6);


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
	private static CheckBox flatCaveWallsCheckBox;
	public static boolean disableFlavourObjects = Utils.getprefb("disableFlavourObjects", false);
	public static boolean flatWorldSetting = Utils.getprefb("flatWorld", false);
	public static boolean noTileSmoothing = Utils.getprefb("noTileSmoothing", false);
	public static boolean noTileTransitions = Utils.getprefb("noTileTransitions", false);
	public static boolean flatCaveWalls = Utils.getprefb("flatCaveWalls", false);
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
			prev = add(disableWeatherEffectsCheckBox = new CheckBox("Disable Weather (Requires Reload)"){
				{a = Utils.getprefb("isWeatherDisabled", false);}
				public void set(boolean val) {
					Utils.setprefb("isWeatherDisabled", val);
					MapView.isWeatherDisabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(disableFlavourObjectsCheckBox = new CheckBox("Disable Flavour Objects"){
				{a = Utils.getprefb("disableFlavourObjects", false);}
				public void set(boolean val) {
					Utils.setprefb("disableFlavourObjects", val);
					disableFlavourObjects = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(simpleCropsCheckBox = new CheckBox("Simplified Crops (Requires Reload)"){
				{a = Utils.getprefb("simplifiedCrops", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedCrops", val);
					simplifiedCrops = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(simpleForageablesCheckBox = new CheckBox("Simplified Forageables (Requires Reload)"){
				{a = Utils.getprefb("simplifiedForageables", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedForageables", val);
					simplifiedForageables = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));
			prev = add(flatWorldCheckBox = new CheckBox("Flat World"){
				{a = Utils.getprefb("flatWorld", false);}
				public void set(boolean val) {
					Utils.setprefb("flatWorld", val);
					flatWorldSetting = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					a = val;
				}
			}, prev.pos("bl").adds(0, 16));

			prev = add(tileSmoothingCheckBox = new CheckBox("Disable Tile Smoothing"){
				{a = Utils.getprefb("noTileSmoothing", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileSmoothing", val);
					noTileSmoothing = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			prev = add(tileTransitionsCheckBox = new CheckBox("Disable Tile Transitions"){
				{a = Utils.getprefb("noTileTransitions", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileTransitions", val);
					noTileTransitions = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					a = val;
				}
			}, prev.pos("bl").adds(0, 8));

			prev = add(flatCaveWallsCheckBox = new CheckBox("Flat Cave Walls"){
				{a = Utils.getprefb("flatCaveWalls", false);}
				public void set(boolean val) {
					Utils.setprefb("flatCaveWalls", val);
					flatCaveWalls = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
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
	private static CheckBox massSplitCtrlShiftAltCheckBox;
	private static CheckBox autoswitchBunnyPlateBootsCheckBox;
	public static CheckBox saveCutleryCheckBox = null;
	public static boolean instantFlowerMenuCTRL = Utils.getprefb("instantFlowerMenuCTRL", true);
	public static boolean massSplitCtrlShiftAlt = Utils.getprefb("massSplitCtrlShiftAlt", true);
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
			prev = add(new CheckBox("Party Permissions"){
				{a = Utils.getprefb("togglePartyPermissionsOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("togglePartyPermissionsOnLogin", val);
					GameUI.togglePartyPermissionsOnLogin = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(new CheckBox("Automatic Item Stacking"){
				{a = Utils.getprefb("toggleItemStackingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleItemStackingOnLogin", val);
					GameUI.toggleItemStackingOnLogin = val;
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
			prev = add(instantFlowerMenuCTRLCheckBox = new CheckBox("Instantly select 1st flower menu option when holding Ctrl"){
				{a = Utils.getprefb("instantFlowerMenuCTRL", true);}
				public void set(boolean val) {
					Utils.setprefb("instantFlowerMenuCTRL", val);
					instantFlowerMenuCTRL = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(massSplitCtrlShiftAltCheckBox = new CheckBox("Mass Split on Right Click + holding Ctrl+Shift+Alt"){
				{a = Utils.getprefb("massSplitCtrlShiftAlt", true);}
				public void set(boolean val) {
					Utils.setprefb("massSplitCtrlShiftAlt", val);
					massSplitCtrlShiftAlt = val;
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
	public static CheckBox partyMembersHighlightCheckBox;
	public static CheckBox partyMembersCirclesCheckBox;
	public static CheckBox aggroedEnemiesCirclesCheckBox;
	private static Button damageInfoClearButton;
	public static boolean partyMembersHighlight = Utils.getprefb("partyMembersHighlight", false);
	public static boolean partyMembersCircles = Utils.getprefb("partyMembersCircles", true);
	public class NDCombatSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}
		public NDCombatSettingsPanel(Panel back) {
			Widget prev;

			Fightsess.altui = Utils.getprefb("useProperCombatUI", true);
			Fightsess.drawFloatingCombatData = Utils.getprefb("drawFloatingCombatData", true);
			Fightsess.drawFloatingCombatDataOnCur = Utils.getprefb("drawFloatingCombatDataOnCurrentTarget ", true);
			Fightsess.showKeybindCombatSetting = Utils.getprefb("showCombatHotkeysUI", true);
			Fightsess.markCombatTargetSetting = Utils.getprefb("markCurrentCombatTarget", true);
			Fightview.autoPeaceSetting = Utils.getprefb("autoPeaceCombat", false);
			GobDamageInfo.toggleGobDamageInfo = Utils.getprefb("GobDamageInfoToggled", true);
			GobDamageInfo.toggleGobDamageInfoWounds = Utils.getprefb("GobDamageInfoWoundsToggled", true);
			GobDamageInfo.toggleGobDamageInfoArmor = Utils.getprefb("GobDamageInfoArmorToggled", true);

			prev = add(new Label("Combat UI:"), 0, 0);
			prev = add(new CheckBox("Use Improved Combat UI"){
				{a = Utils.getprefb("useProperCombatUI", true);}
				public void set(boolean val) {
					Utils.setprefb("useProperCombatUI", val);
					Fightsess.altui = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(new Label("Top panel height (Improved UI):"), prev.pos("bl").adds(-16, 10));
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
			prev = add(new Label("Bottom panel height (Improved UI):"), prev.pos("bl").adds(0, 10));
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
			prev = add(new CheckBox("Show Hotkeys on the Bottom Panel (Combat Moves)"){
				{a = Utils.getprefb("showCombatHotkeysUI", true);}
				public void set(boolean val) {
					Utils.setprefb("showCombatHotkeysUI", val);
					Fightsess.showKeybindCombatSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(new CheckBox("Show Combat Data above Current Target"){
				{a = Utils.getprefb("drawFloatingCombatData", true);}
				public void set(boolean val) {
					Utils.setprefb("drawFloatingCombatDataOnCurrentTarget", val);
					Fightsess.drawFloatingCombatDataOnCur = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(new CheckBox("Show Combat Data above other Aggroed Enemies"){
				{a = Utils.getprefb("drawFloatingCombatData", true);}
				public void set(boolean val) {
					Utils.setprefb("drawFloatingCombatData", val);
					Fightsess.drawFloatingCombatData = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(new CheckBox("Show Background for Combat Data"){
				{a = Utils.getprefb("CombatInfoBackgroundToggled", false);}
				public void set(boolean val) {
					Utils.setprefb("CombatInfoBackgroundToggled", val);
					Fightsess.showInfoBackground = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(toggleGobDamageInfoCheckBox = new CheckBox("Show Damage Info:"){
				{a = Utils.getprefb("GobDamageInfoToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoToggled", val);
					GobDamageInfo.toggleGobDamageInfo = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 12).x(16));
			prev = add(new Label("> Include:"), prev.pos("bl").adds(18, 3));
			CheckBox woundsCheckBox;
			prev = add(woundsCheckBox = new CheckBox("Wounds"){
				{a = Utils.getprefb("GobDamageInfoWoundsToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoWoundsToggled", val);
					GobDamageInfo.toggleGobDamageInfoWounds = val;
					a = val;
				}
			}, prev.pos("bl").adds(56, -13));
			woundsCheckBox.lbl = Text.std.render("Wounds", new Color(255, 232, 0, 255));
			CheckBox armorCheckBox;
			prev = add(armorCheckBox = new CheckBox("Armor"){
				{a = Utils.getprefb("GobDamageInfoArmorToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoArmorToggled", val);
					GobDamageInfo.toggleGobDamageInfoArmor = val;
					a = val;
				}
			}, prev.pos("bl").adds(62, -14));
			armorCheckBox.lbl = Text.std.render("Armor", new Color(50, 255, 92, 255));
			add(damageInfoClearButton = new Button(UI.scale(70), "Clear", false).action(() -> {
				GobDamageInfo.clearAllDamage(gameui());
			}), prev.pos("bl").adds(0, -34).x(UI.scale(210)));
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
			prev = add(new CheckBox("Mark Current Target"){
				{a = Utils.getprefb("markCurrentCombatTarget", true);}
				public void set(boolean val) {
					Utils.setprefb("markCurrentCombatTarget", val);
					Fightsess.markCombatTargetSetting = val;
					a = val;
				}
			}, scroll.pos("bl").adds(2, -6));
			prev = add(aggroedEnemiesCirclesCheckBox = new CheckBox("Put Circles under Aggroed Enemies (Players/Mobs)"){
				{a = Utils.getprefb("aggroedEnemiesCircles", true);}
				public void set(boolean val) {
					Utils.setprefb("aggroedEnemiesCircles", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));
			prev = add(partyMembersHighlightCheckBox = new CheckBox("Highlight Party Members"){
				{a = Utils.getprefb("partyMembersHighlight", false);}
				public void set(boolean val) {
					Utils.setprefb("partyMembersHighlight", val);
					partyMembersHighlight = val;
					if (gameui() != null && gameui().map != null && gameui().map.partyHighlight != null)
						gameui().map.partyHighlight.update();
					a = val;
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(partyMembersCirclesCheckBox = new CheckBox("Put Circles under Party Members"){
				{a = Utils.getprefb("partyMembersCircles", true);}
				public void set(boolean val) {
					Utils.setprefb("partyMembersCircles", val);
					partyMembersCircles = val;
					if (gameui() != null && gameui().map != null && gameui().map.partyCircles != null)
						gameui().map.partyCircles.update();
					a = val;
				}
			}, prev.pos("bl").adds(0, 6));

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
					if (gameui() != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
						ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
					}
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
					if (gameui() != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
					}
					a = val;
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(hideBushesCheckbox = new CheckBox("Bushes"){
				{a = Utils.getprefb("hideBushes", true);}
				public void set(boolean val) {
					Utils.setprefb("hideBushes", val);
					hideBushesSetting = val;
					if (gameui() != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
					}
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
			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(57)));
			setTooltipsForHidingSettingsStuff();
			pack();
		}
	}

	//DropSettings
	public static CheckBox toggleDropItemsCheckBox;
	public static boolean dropMinedItemsSetting = Utils.getprefb("dropItemsToggle", false);
	public static CheckBox dropStoneCheckbox;
	public static boolean dropStoneSetting = Utils.getprefb("dropStone", false);
	public static CheckBox dropOreCheckbox;
	public static boolean dropOreSetting = Utils.getprefb("dropOre", false);
	public static CheckBox dropPreciousOreCheckbox;
	public static boolean dropPreciousOreSetting = Utils.getprefb("dropPreciousOre", false);
	public static CheckBox dropMinedCuriosCheckbox;
	public static boolean dropMinedCuriosSetting = Utils.getprefb("dropMinedCurios", false);
	public static CheckBox dropQuarryartzCheckbox;
	public static boolean dropQuarryartzSetting = Utils.getprefb("dropQuarryartz", false);

	public class NDAutoDropSettingsPanel extends Panel {

		public NDAutoDropSettingsPanel(Panel back) {
			Widget prev;
			prev = add(toggleDropItemsCheckBox = new CheckBox("Drop Mined Items"){
				{a = (Utils.getprefb("dropItemsToggle", false));}
				public void set(boolean val) {
					Utils.setprefb("dropItemsToggle", val);
					dropMinedItemsSetting = val;
					a = val;
				}
			}, 0, 10);

			prev = add(new Label("Objects that will be dropped:"), prev.pos("bl").adds(0, 20).x(0));

			prev = add(dropStoneCheckbox = new CheckBox("Mined Stone"){
				{a = Utils.getprefb("dropStone", false);}
				public void set(boolean val) {
					Utils.setprefb("dropStone", val);
					dropStoneSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(dropOreCheckbox = new CheckBox("Mined Ore"){
				{a = Utils.getprefb("dropOre", false);}
				public void set(boolean val) {
					Utils.setprefb("dropOre", val);
					dropOreSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(dropPreciousOreCheckbox = new CheckBox("Mined Precious Ore"){
				{a = Utils.getprefb("dropPreciousOre", false);}
				public void set(boolean val) {
					Utils.setprefb("dropPreciousOre", val);
					dropPreciousOreSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(dropMinedCuriosCheckbox = new CheckBox("Mined Curios"){
				{a = Utils.getprefb("dropMinedCurios", false);}
				public void set(boolean val) {
					Utils.setprefb("dropMinedCurios", val);
					dropMinedCuriosSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(dropQuarryartzCheckbox = new CheckBox("Quarryartz"){
				{a = Utils.getprefb("dropQuarryartz", false);}
				public void set(boolean val) {
					Utils.setprefb("dropQuarryartz", val);
					dropQuarryartzSetting = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));
			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));

			pack();
		}
	}

	Button CustomAlarmManagerButton;
	private static CheckBox whitePlayerAlarmEnabledCheckbox;
	public static boolean whitePlayerAlarmEnabled = Utils.getprefb("whitePlayerAlarmEnabled", true);
	public static TextEntry whitePlayerAlarmFilename;
	public static HSlider whitePlayerAlarmVolumeSlider;
	private static CheckBox whiteVillageOrRealmPlayerAlarmEnabledCheckbox;
	public static boolean whiteVillageOrRealmPlayerAlarmEnabled = Utils.getprefb("whiteVillageOrRealmPlayerAlarmEnabled", true);
	public static TextEntry whiteVillageOrRealmPlayerAlarmFilename;
	public static HSlider whiteVillageOrRealmPlayerAlarmVolumeSlider;
	private static CheckBox greenPlayerAlarmEnabledCheckbox;
	public static boolean greenPlayerAlarmEnabled = Utils.getprefb("greenPlayerAlarmEnabled", false);
	public static TextEntry greenPlayerAlarmFilename;
	public static HSlider greenPlayerAlarmVolumeSlider;
	private static CheckBox redPlayerAlarmEnabledCheckbox;
	public static boolean redPlayerAlarmEnabled = Utils.getprefb("redPlayerAlarmEnabled", true);
	public static TextEntry redPlayerAlarmFilename;
	public static HSlider redPlayerAlarmVolumeSlider;
	private static CheckBox bluePlayerAlarmEnabledCheckbox;
	public static boolean bluePlayerAlarmEnabled = Utils.getprefb("bluePlayerAlarmEnabled", false);
	public static TextEntry bluePlayerAlarmFilename;
	public static HSlider bluePlayerAlarmVolumeSlider;
	private static CheckBox tealPlayerAlarmEnabledCheckbox;
	public static boolean tealPlayerAlarmEnabled = Utils.getprefb("tealPlayerAlarmEnabled", false);
	public static TextEntry tealPlayerAlarmFilename;
	public static HSlider tealPlayerAlarmVolumeSlider;
	private static CheckBox yellowPlayerAlarmEnabledCheckbox;
	public static boolean yellowPlayerAlarmEnabled = Utils.getprefb("yellowPlayerAlarmEnabled", false);
	public static TextEntry yellowPlayerAlarmFilename;
	public static HSlider yellowPlayerAlarmVolumeSlider;
	private static CheckBox purplePlayerAlarmEnabledCheckbox;
	public static boolean purplePlayerAlarmEnabled = Utils.getprefb("purplePlayerAlarmEnabled", false);
	public static TextEntry purplePlayerAlarmFilename;
	public static HSlider purplePlayerAlarmVolumeSlider;
	private static CheckBox orangePlayerAlarmEnabledCheckbox;
	public static boolean orangePlayerAlarmEnabled = Utils.getprefb("orangePlayerAlarmEnabled", false);
	public static TextEntry orangePlayerAlarmFilename;
	public static HSlider orangePlayerAlarmVolumeSlider;
	private static CheckBox combatStartSoundEnabledCheckbox;
	public static boolean combatStartSoundEnabled = Utils.getprefb("combatStartSoundEnabled", false);
	public static TextEntry combatStartSoundFilename;
	public static HSlider combatStartSoundVolumeSlider;
	private static CheckBox cleaveSoundEnabledCheckbox;
	public static boolean cleaveSoundEnabled = Utils.getprefb("cleaveSoundEnabled", true);
	public static TextEntry cleaveSoundFilename;
	public static HSlider cleaveSoundVolumeSlider;
	private static CheckBox opkSoundEnabledCheckbox;
	public static boolean opkSoundEnabled = Utils.getprefb("opkSoundEnabled", true);
	public static TextEntry opkSoundFilename;
	public static HSlider opkSoundVolumeSlider;

	private static CheckBox ponyPowerSoundEnabledCheckbox;
	public static boolean ponyPowerSoundEnabled = Utils.getprefb("ponyPowerSoundEnabled", true);
	public static TextEntry ponyPowerSoundFilename;
	public static HSlider ponyPowerSoundVolumeSlider;

	private static CheckBox lowEnergySoundEnabledCheckbox;
	public static boolean lowEnergySoundEnabled = Utils.getprefb("lowEnergySoundEnabled", true);
	public static TextEntry lowEnergySoundFilename;
	public static HSlider lowEnergySoundVolumeSlider;

	public class NDAlarmsAndSoundsSettingsPanel extends Panel {

		public NDAlarmsAndSoundsSettingsPanel(Panel back) {
			Widget prev;

			add(new Label("You can add your own alarm sound files in the \"Alarms\" folder.", new Text.Foundry(Text.sans, 12)), 0, 0);
			add(new Label("(The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), UI.scale(0, 16));
			prev = add(new Label("Enabled Player Alarms:"), UI.scale(0, 40));
			prev = add(new Label("Sound File"), prev.pos("ur").add(70, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(84, 0));
			prev = add(whitePlayerAlarmEnabledCheckbox = new CheckBox("White/Unknown:"){
				{a = Utils.getprefb("whitePlayerAlarmEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("whitePlayerAlarmEnabled", val);
					whitePlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 10).x(0));
			prev = add(whitePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("whitePlayerAlarmFilename", "ND_YoHeadsUp")){
				protected void changed() {
					Utils.setpref("whitePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(45, -2));
			prev = add(whitePlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("whitePlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("whitePlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + whitePlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, whitePlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(whiteVillageOrRealmPlayerAlarmEnabledCheckbox = new CheckBox("White(Village/Realm):"){
				{a = Utils.getprefb("whiteVillageOrRealmPlayerAlarmEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("whiteVillageOrRealmPlayerAlarmEnabled", val);
					whiteVillageOrRealmPlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(whiteVillageOrRealmPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("whiteVillageOrRealmPlayerAlarmFilename", "ND_HelloFriend")){
				protected void changed() {
					Utils.setpref("whiteVillageOrRealmPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(16, -2));
			prev = add(whiteVillageOrRealmPlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("whiteVillageOrRealmPlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("whiteVillageOrRealmPlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + whiteVillageOrRealmPlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, whiteVillageOrRealmPlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(greenPlayerAlarmEnabledCheckbox = new CheckBox("Green:"){
				{a = Utils.getprefb("greenPlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("greenPlayerAlarmEnabled", val);
					greenPlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			greenPlayerAlarmEnabledCheckbox.lbl = Text.std.render("Green:", BuddyWnd.gc[1]);
			prev = add(greenPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("greenPlayerAlarmFilename", "ND_FlyingTheFriendlySkies")){
				protected void changed() {
					Utils.setpref("greenPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(88, -2));
			prev = add(greenPlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("greenPlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("greenPlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + greenPlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, greenPlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(redPlayerAlarmEnabledCheckbox = new CheckBox("Red:"){
				{a = Utils.getprefb("redPlayerAlarmEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("redPlayerAlarmEnabled", val);
					redPlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			redPlayerAlarmEnabledCheckbox.lbl = Text.std.render("Red:", BuddyWnd.gc[2]);
			prev = add(redPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("redPlayerAlarmFilename", "ND_EnemySighted")){
				protected void changed() {
					Utils.setpref("redPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(98, -2));
			prev = add(redPlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("redPlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("redPlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + redPlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, redPlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(bluePlayerAlarmEnabledCheckbox = new CheckBox("Blue:"){
				{a = Utils.getprefb("bluePlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("bluePlayerAlarmEnabled", val);
					bluePlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			bluePlayerAlarmEnabledCheckbox.lbl = Text.std.render("Blue:", BuddyWnd.gc[3]);
			prev = add(bluePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("bluePlayerAlarmFilename", "ND_YeahLetsHustle")){
				protected void changed() {
					Utils.setpref("bluePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(95, -2));
			prev = add(bluePlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("bluePlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("bluePlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + bluePlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, bluePlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(tealPlayerAlarmEnabledCheckbox = new CheckBox("Teal:"){
				{a = Utils.getprefb("tealPlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("tealPlayerAlarmEnabled", val);
					tealPlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			tealPlayerAlarmEnabledCheckbox.lbl = Text.std.render("Teal:", BuddyWnd.gc[4]);
			prev = add(tealPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("tealPlayerAlarmFilename", "ND_YeahLetsHustle")){
				protected void changed() {
					Utils.setpref("tealPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(95, -2));
			prev = add(tealPlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("tealPlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("tealPlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + tealPlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, tealPlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(yellowPlayerAlarmEnabledCheckbox = new CheckBox("Yellow:"){
				{a = Utils.getprefb("yellowPlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("yellowPlayerAlarmEnabled", val);
					yellowPlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			yellowPlayerAlarmEnabledCheckbox.lbl = Text.std.render("Yellow:", BuddyWnd.gc[5]);
			prev = add(yellowPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("yellowPlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("yellowPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(85, -2));
			prev = add(yellowPlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("yellowPlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("yellowPlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + yellowPlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, yellowPlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(purplePlayerAlarmEnabledCheckbox = new CheckBox("Purple:"){
				{a = Utils.getprefb("purplePlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("purplePlayerAlarmEnabled", val);
					purplePlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			purplePlayerAlarmEnabledCheckbox.lbl = Text.std.render("Purple:", BuddyWnd.gc[6]);
			prev = add(purplePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("purplePlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("purplePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(86, -2));
			prev = add(purplePlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("purplePlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("purplePlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + purplePlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, purplePlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(orangePlayerAlarmEnabledCheckbox = new CheckBox("Orange:"){
				{a = Utils.getprefb("orangePlayerAlarmEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("orangePlayerAlarmEnabled", val);
					orangePlayerAlarmEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			orangePlayerAlarmEnabledCheckbox.lbl = Text.std.render("Orange:", BuddyWnd.gc[7]);
			prev = add(orangePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("orangePlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("orangePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(82, -2));
			prev = add(orangePlayerAlarmVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("orangePlayerAlarmVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("orangePlayerAlarmVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + orangePlayerAlarmFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, orangePlayerAlarmVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));


			prev = add(new Label("Enabled Sounds & Alerts:"), prev.pos("bl").add(0, 10).x(0));
			prev = add(new Label("Sound File"), prev.pos("ur").add(69, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(84, 0));
			prev = add(combatStartSoundEnabledCheckbox = new CheckBox("Combat Start Alert:"){
				{a = Utils.getprefb("combatStartSoundEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("combatStartSoundEnabled", val);
					combatStartSoundEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 10).x(0));
			prev = add(combatStartSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("combatStartSoundFilename", "ND_HitAndRun")){
				protected void changed() {
					Utils.setpref("combatStartSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(30, -2));
			prev = add(combatStartSoundVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("combatStartSoundVolume", 50)){
				@Override
				public void changed() {
					Utils.setprefi("combatStartSoundVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + combatStartSoundFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, combatStartSoundVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);

				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(cleaveSoundEnabledCheckbox = new CheckBox("Cleave Sound Effect:"){
				{a = Utils.getprefb("cleaveSoundEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("cleaveSoundEnabled", val);
					cleaveSoundEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(cleaveSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("cleaveSoundFilename", "ND_Cleave")){
				protected void changed() {
					Utils.setpref("cleaveSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(19, -2));
			prev = add(cleaveSoundVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("cleaveSoundVolume", 75)){
				@Override
				public void changed() {
					Utils.setprefi("cleaveSoundVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + cleaveSoundFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, cleaveSoundVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);

				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(opkSoundEnabledCheckbox = new CheckBox("Oppknock Sound Effect:"){
				{a = Utils.getprefb("opkSoundEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("opkSoundEnabled", val);
					opkSoundEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(opkSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("opkSoundFilename", "ND_Opk")){
				protected void changed() {
					Utils.setpref("opkSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2));
			prev = add(opkSoundVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("opkSoundVolume", 75)){
				@Override
				public void changed() {
					Utils.setprefi("opkSoundVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + opkSoundFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, opkSoundVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);

				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(ponyPowerSoundEnabledCheckbox = new CheckBox("Pony Power <10% Alert:"){
				{a = Utils.getprefb("ponyPowerSoundEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("ponyPowerSoundEnabled", val);
					ponyPowerSoundEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(ponyPowerSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("ponyPowerSoundFilename", "ND_HorseEnergy")){
				protected void changed() {
					Utils.setpref("ponyPowerSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2));
			prev = add(ponyPowerSoundVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("ponyPowerSoundVolume", 35)){
				@Override
				public void changed() {
					Utils.setprefi("ponyPowerSoundVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + ponyPowerSoundFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, ponyPowerSoundVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(lowEnergySoundEnabledCheckbox = new CheckBox("Energy <2500% Alert:"){
				{a = Utils.getprefb("lowEnergySoundEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("lowEnergySoundEnabled", val);
					lowEnergySoundEnabled = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(lowEnergySoundFilename = new TextEntry(UI.scale(140), Utils.getpref("lowEnergySoundFilename", "ND_NotEnoughEnergy")){
				protected void changed() {
					Utils.setpref("lowEnergySoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(16, -2));
			prev = add(lowEnergySoundVolumeSlider = new HSlider(UI.scale(100), 0, 100, Utils.getprefi("lowEnergySoundVolume", 35)){
				@Override
				public void changed() {
					Utils.setprefi("lowEnergySoundVolume", val);
					super.changed();
				}
			}, prev.pos("ur").adds(6,3));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(Coord c, int button) {
					if(button != 1)
						return true;
					File file = new File("Alarms/" + lowEnergySoundFilename.buf.line() + ".wav");
					if(!file.exists() || file.isDirectory()) {
						if (gameui() != null)
							gameui().msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
						return super.mousedown(c, button);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, lowEnergySoundVolumeSlider.val/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(c, button);
				}
			}, prev.pos("ur").adds(6,-5));

			prev = add(CustomAlarmManagerButton = new Button(UI.scale(360), ">>> Other Alarms (Custom Alarm Manager) <<<", () -> {
				if(alarmWindow == null) {
					alarmWindow = this.parent.parent.add(new AlarmWindow());
					alarmWindow.show();
				} else {
					alarmWindow.show(!alarmWindow.visible);
					alarmWindow.bottomNote.settext("NOTE: You can add your own alarm sound files in the \"Alarms\" folder. (The file extension must be .wav)");
					alarmWindow.bottomNote.setcolor(Color.WHITE);
					alarmWindow.bottomNote.c.x = UI.scale(140);
				}
			}),prev.pos("bl").adds(0, 18).x(51));


			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(131));
			setTooltipsForAlarmSettingsStuff();
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
		Panel dropsettings = add(new NDAutoDropSettingsPanel(advancedSettings));
		Panel alarmsettings = add(new NDAlarmsAndSoundsSettingsPanel(advancedSettings));

		int y2 = UI.scale(6);
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Interface & Display Settings", -1, iface, "Interface & Display Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Graphics Settings", -1, graphicssettings, "Graphics Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Action Bars Settings", -1, actionbarsettings, "Action Bars Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Camera Settings", -1, camsettings, "Camera Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Gameplay Settings", -1, gameplaysettings, "Gameplay Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Combat Settings", -1, combatsettings, "Combat Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Hiding Settings", -1, hidingsettings, "Hiding Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Drop Settings", -1, dropsettings, "Drop Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Alarms & Sounds Settings", -1, alarmsettings, "Alarms & Sounds Settings"), 0, y2).pos("bl").adds(0, 25).y;

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
		enableAdvancedMouseInfoCheckBox.tooltip = RichText.render("Holding Ctrl+Shift will show the Resource Path of the object or tile you are mousing over. Enabling this option will show additional information.\n$col[185,185,185]{Unless you're a client dev, you don't really need to enable this option.}", 300);
		enableKeepWindowsInsideCheckBox.tooltip = RichText.render("Enabling this will cause ALL Windows to always be kept inside the Game Window, whenever you resize it.\n$col[185,185,185]{Note: By default, windows remain in the same spot, outside of your resized window.", 300);
		granularityPositionLabel.tooltip = RichText.render("Equivalent of the :placegrid console command, this allows you to have more freedom when placing constructions/objects.", 300);
		granularityAngleLabel.tooltip = RichText.render("Equivalent of the :placeangle console command, this allows you to have more freedom when rotating constructions/objects before placement.", 300);
		alwaysOpenBeltCheckBox.tooltip = RichText.render("Enabling this will cause your belt window to always open when you log in.\n$col[185,185,185]{Note: By default, Loftar saves the status of the belt at logout. So if you don't enable this setting, but leave the belt window open when you log out/exit the game, it will still open on login.}", 300);
		showQuickSlotsBar.tooltip = RichText.render("Note: The Quick Switch keybinds ('Right Hand' and 'Left Hand') will still work, regardless of this widget being visible or not.", 300);
		toggleGobGrowthInfoCheckBox.tooltip = RichText.render("Enabling this will show the following growth information:\n$col[185,185,185]{> Trees and Bushes will display their current growth percentage\n> Crops will display their growth stage as \"Current / Final\"\n}Note: If a Tree or Bush is not showing a percentage, that means it is fully grown, and can be harvested.", 300);
		toggleGobCollisionBoxesDisplayCheckBox.tooltip = RichText.render("$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.}", 300);
		toggleBeastDangerRadiiCheckBox.tooltip = RichText.render("$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.}", 300);
		toggleCritterAurasCheckBox.tooltip = RichText.render("$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.}", 300);
		showContainerFullnessCheckBox.tooltip = RichText.render("Enabling this will overlay the following colors over Container Objects, to indicate their fullness:" +
				"\n$col[185,0,0]{Red: }$col[255,255,255]{Full}\n$col[224,213,0]{Yellow: }$col[255,255,255]{Contains items}\n$col[0,185,0]{Green: }$col[255,255,255]{Empty}", 300);
		showWorkstationStageCheckBox.tooltip = RichText.render("Enabling this will overlay the following colors over Workstation Objects (Drying Frame, Tanning Tub, Garden Pot, Cheese Rack), to indicate their progress stage:" +
				"\n$col[185,0,0]{Red: }$col[255,255,255]{Finished}\n$col[224,213,0]{Yellow: }$col[255,255,255]{In progress}\n$col[0,185,0]{Green: }$col[255,255,255]{Ready for use}\n$col[160,160,160]{Gray: }$col[255,255,255]{Unprepared}", 300);
	}

	private void setTooltipsForCombatSettingsStuff(){
		toggleGobDamageInfoCheckBox.tooltip = RichText.render("Enabling this will display the total amount of damage players and animals took.\nNote: The damage you will see saved above players/animals is the total damage you saw the entity take, while inside of your view range. This is not all of the damage said entity might have taken recently.\n$col[185,185,185]{If you change any of the settings below, you will need a damage update in order to see the changes (for example, deal some damage to the player/animal).}", 300);
		damageInfoClearButton.tooltip = RichText.render("Clear all damage info", 300);
		toggleAutoPeaceCheckbox.tooltip = RichText.render("Enabling this will automatically set your status to 'Peace' when combat is initiated with a new target (animals only). Toggling this on while in combat will also autopeace all animals you are currently fighting.\n$col[185,185,185]{Note: This option can also be turned on/off using a hotkey.}", 300);
		partyMembersHighlightCheckBox.tooltip = RichText.render("Enabling this will put a color highlight over all party members." +
				"\n=====================" +
				"\n$col[255,255,255]{White: }$col[185,185,185]{Yourself}\n$col[0,74,208]{Blue: }$col[185,185,185]{Party Leader}\n$col[0,160,0]{Green: }$col[185,185,185]{Other Members}" +
				"\n=====================" +
				"\n$col[185,185,185]{Note: If you are the party leader, your color highlight will always be $col[0,74,208]{Blue}, rather than $col[255,255,255]{White}.}", 300);
		partyMembersCirclesCheckBox.tooltip = RichText.render("Enabling this will put a colored circle under all party members." +
				"\n=====================" +
				"\n$col[255,255,255]{White: }$col[185,185,185]{Yourself}\n$col[0,74,208]{Blue: }$col[185,185,185]{Party Leader}\n$col[0,160,0]{Green: }$col[185,185,185]{Other Members}" +
				"\n=====================" +
				"\n$col[185,185,185]{Note: If you are the party leader, your circle's color will always be $col[0,74,208]{Blue}, rather than $col[255,255,255]{White}.}", 300);
	}
	private void setTooltipsForGameplaySettingsStuff(){
		defaultSpeedLabel.tooltip = RichText.render("Sets your character's movement speed on login.", 300);
		instantFlowerMenuCTRLCheckBox.tooltip = RichText.render("Enabling this will make holding Ctrl before right clicking an item or object instantly select the first available option from the flower menu.", 300);
		massSplitCtrlShiftAltCheckBox.tooltip = RichText.render("Enabling this will make holding Ctrl+Shift+Alt before right clicking an item to instantly split all of the items of this type.", 300);
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

	private void setTooltipsForAlarmSettingsStuff(){
		lowEnergySoundEnabledCheckbox.tooltip = RichText.render("This alarm will also trigger again when you reach <2000% energy.\n$col[185,185,185]{Don't starve yourself, dumbass.}", 400);
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