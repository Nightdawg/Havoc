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
import haven.res.gfx.fx.msrad.MSRad;
import haven.sprites.AggroCircleSprite;
import haven.sprites.AuraCircleSprite;
import haven.sprites.ChaseVectorSprite;
import haven.sprites.CurrentTargetSprite;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class OptWnd extends Window {
    public final Panel main;
	public final Panel advancedSettings;
    public Panel current;
	public static int cameraLmaoMessage = 1; // ND: Message for "cam" console command, idk where to put this lmao
	AlarmWindow alarmWindow;
	public static AutoFlowerWindow autoFlowerWindow;
	public static final Color msgGreen = new Color(8, 211, 0);
	public static final Color msgGray = new Color(145, 145, 145);
	public static final Color msgRed = new Color(197, 0, 0);
	public static final Color msgYellow = new Color(218, 163, 0);

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
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
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
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
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
		    prev = grp.add("Global", prev.pos("bl").adds(5, 0));
		    prev.settip("Global lighting supports fewer light sources, and scales worse in " +
				"performance per additional light source, than zoned lighting, but " +
				"has lower baseline performance requirements.", true);
		    prev = grp.add("Zoned", prev.pos("bl").adds(0, 0));
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
		    prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 0));
		    prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 0));
		    prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 0));
		    prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 0));
		    prev = add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
			{
				prev = add(new CheckBox("Experimental Render (Require Restart)") {
					{a = (Utils.getprefb("enableExperimentalRender", false));}
					public void set(boolean val) {
						Utils.setprefb("enableExperimentalRender", val);
						a = val;
					}
				}, prev.pos("bl").adds(0, 5));
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

	public static HSlider clapSoundVolumeSlider;
	public static HSlider quernSoundVolumeSlider;
	public static HSlider cauldronSoundVolumeSlider;
	public static HSlider squeakSoundVolumeSlider;
	private final int audioSliderWidth = 220;

    public class AudioPanel extends Panel {
	public AudioPanel(Panel back) {
	    prev = add(new Label("Master Audio Volume"), 0, 0);
	    prev = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, (int)(Audio.volume * 1000)) {
		    public void changed() {
			Audio.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Interface Sound Volume"), prev.pos("bl").adds(0, 10));
	    prev = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.aui.volume * 1000);
		    }
		    public void changed() {
			ui.audio.aui.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("In-game Event Volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.pos.volume * 1000);
		    }
		    public void changed() {
			ui.audio.pos.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Ambient Volume"), prev.pos("bl").adds(0, 5));
	    prev = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
		    protected void attach(UI ui) {
			super.attach(ui);
			val = (int)(ui.audio.amb.volume * 1000);
		    }
		    public void changed() {
			ui.audio.amb.setvolume(val / 1000.0);
		    }
		}, prev.pos("bl").adds(0, 2));
	    prev = add(new Label("Audio Latency"), prev.pos("bl").adds(0, 10));
	    {
		Label dpy = new Label("");
		addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
		       prev = new HSlider(UI.scale(160), Math.round(Audio.fmt.getSampleRate() * 0.05f), Math.round(Audio.fmt.getSampleRate() / 4), Audio.bufsize()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(Math.round((this.val * 1000) / Audio.fmt.getSampleRate()) + " ms");
			       }
			       public void changed() {
				   Audio.bufsize(val, true);
				   dpy();
			       }
			   }, dpy);
		prev.settip("Sets the size of the audio buffer. " +
				"\n$col[185,185,185]{Loftar claims that smaller sizes are better, but anything below 50ms always fucking stutters, so I limited it there." +
				"\nIncrease this if your audio is still stuttering.}", true);
	    }

		prev = add(new Label("Other Sound Settings"), prev.pos("bl").adds(52, 20));
		prev = add(new Label("Clap Sound Effect Volume"), prev.pos("bl").adds(0, 5).x(0));
		prev = add(clapSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("clapSoundVolume", 10)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("clapSoundVolume", val);
			}
		}, prev.pos("bl").adds(0, 2));

		prev = add(new Label("Quern Sound Effect Volume"), prev.pos("bl").adds(0, 5).x(0));
		prev = add(quernSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("quernSoundVolume", 10)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("quernSoundVolume", val);
			}
		}, prev.pos("bl").adds(0, 2));

		prev = add(new Label("Boiling Cauldron Volume (Requires Reload)"), prev.pos("bl").adds(0, 5).x(0));
		prev = add(cauldronSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("cauldronSoundVolume", 25)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("cauldronSoundVolume", val);
			}
		}, prev.pos("bl").adds(0, 2));

		prev = add(new Label("Squeak Sound Volume (Roasting Spit, etc.)"), prev.pos("bl").adds(0, 5).x(0));
		prev = add(squeakSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("squeakSoundVolume", 25)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("squeakSoundVolume", val);
			}
		}, prev.pos("bl").adds(0, 2));


		add(new PButton(UI.scale(audioSliderWidth), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 24).x(0));
	    pack();
	}
    }

	private CheckBox enableCornerFPSCheckBox;
	public static CheckBox enableAdvancedMouseInfoCheckBox;
	private CheckBox enableWrongResCheckBox;
	private CheckBox useAlternativeUi;
	public static CheckBox enableDragWindowsInWhenResizingCheckBox;
	public static CheckBox enableSnapWindowsBackInsideCheckBox;
	private Label interfaceScaleLabel;
	private HSlider interfaceScaleHSlider;
	private Label granularityPositionLabel;
	private Label granularityAngleLabel;
	public static CheckBox toggleQualityDisplayCheckBox;
	public static CheckBox roundedQualityCheckBox;
	public static CheckBox showQualityBackgroundCheckBox;
	public static CheckBox alwaysShowStaminaBarCheckBox;
	public static CheckBox alwaysShowHealthBarCheckBox;
	public static CheckBox requireShiftHoverStacksCheckBox;
	public static CheckBox objectPermanentHighlightingCheckBox;
	public static CheckBox showStudyWindowHistoryCheckBox;
	public static CheckBox disableMenuGridHotkeysCheckBox;
	public static CheckBox enableMineSweeperCheckBox;
	public static CheckBox lockStudyWindowCheckBox;
	public static CheckBox playSoundOnFinishedCurioCheckBox;
	public static CheckBox toggleGobHealthDisplayCheckBox;
	public static CheckBox toggleGobGrowthInfoCheckBox;
	public static CheckBox toggleGobQualityInfoCheckBox;
	public static CheckBox toggleGobCollisionBoxesDisplayCheckBox;
	public static CheckBox toggleBeastDangerRadiiCheckBox;
	public static CheckBox toggleCritterAurasCheckBox;
	public static CheckBox toggleSpeedBoostAurasCheckBox;
	private CheckBox alwaysOpenBeltCheckBox;
	private CheckBox showQuickSlotsBar;
	private CheckBox showCraftHistoryBar;
	public static CheckBox showContainerFullnessCheckBox;
	public static CheckBox showContainerFullnessRedCheckBox;
	public static CheckBox showContainerFullnessYellowCheckBox;
	public static CheckBox showContainerFullnessGreenCheckBox;
	public static CheckBox showWorkstationStageCheckBox;
	public static CheckBox showWorkstationStageRedCheckBox;
	public static CheckBox showWorkstationStageYellowCheckBox;
	public static CheckBox showWorkstationStageGreenCheckBox;
	public static CheckBox showWorkstationStageGrayCheckBox;
	public static CheckBox displayGatePassabilityBoxesCheckBox;
	public static CheckBox highlightCliffsCheckBox;
	public static CheckBox showMineSupportRadiiCheckBox;
	public static CheckBox showMineSupportSafeTilesCheckBox;
	public static CheckBox showBeeSkepsRadiiCheckBox;
	public static CheckBox showFoodTroughsRadiiCheckBox;
	public static boolean expWindowLocationIsTop = Utils.getprefb("expWindowLocationIsTop", true);
	public static Dropbox<Integer> sweeperDurationDropbox;
	public static final List<Integer> sweeperDurations = Arrays.asList(5, 10, 15, 30, 45, 60, 120);
	public static int sweeperSetDuration = Utils.getprefi("sweeperSetDuration", 3);
    public class InterfacePanel extends Panel {

	public InterfacePanel(Panel back) {
		Widget prev;
		prev = add(interfaceScaleLabel = new Label("Interface scale (requires restart)"), UI.scale(0, 0));
	    {
		Label dpy = new Label("");
		final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
		final int steps = (int)Math.round((smax - smin) / 0.25);
		addhlp(prev.pos("bl").adds(0, 4), UI.scale(5),
		       prev = interfaceScaleHSlider = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
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
		Widget topRightColumn;
		topRightColumn = add(enableAdvancedMouseInfoCheckBox = new CheckBox("Show Extended Mouseover Info (Dev)"){
			{a = (Utils.getprefb("advancedMouseInfo", false));}
			public void set(boolean val) {
				Utils.setprefb("advancedMouseInfo", val);
				a = val;
			}
		}, UI.scale(230, 6));
		topRightColumn = add(enableWrongResCheckBox = new CheckBox("Resource Version Prints (Dev)"){
			{a = (Utils.getprefb("showResourceConsolePrints", false));}
			public void set(boolean val) {
				Resource.showResourceConsolePrints = val;
				Utils.setprefb("showResourceConsolePrints", val);
				a = val;
			}
		}, topRightColumn.pos("bl").adds(0, 2));
		topRightColumn = add(useAlternativeUi = new CheckBox("Use alternative UI theme"){
			{a = (Utils.getprefb("useAlternativeUiTheme", false));}
			public void set(boolean val) {
				Window.useAlternativeUi = val;
				Utils.setprefb("useAlternativeUiTheme", val);
				a = val;
			}
		}, topRightColumn.pos("bl").adds(0, 10));

		prev = add(new Label("Advanced Interface Settings"), prev.pos("bl").adds(0, 18).x(146));
		Widget leftColumn;
		Widget rightColumn;
		leftColumn = add(alwaysOpenBeltCheckBox = new CheckBox("Always Open Belt on Login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", false));}
			public void set(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 18).x(0));
		leftColumn = add(showQuickSlotsBar = new CheckBox("Enable Quick Slots (Hands) Widget"){
			{a = (Utils.getprefb("showQuickSlotsBar", true));}
			public void set(boolean val) {
				Utils.setprefb("showQuickSlotsBar", val);
				a = val;
				if (ui != null && ui.gui != null && ui.gui.quickslots != null){
					ui.gui.quickslots.show(val);
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(showCraftHistoryBar = new CheckBox("Enable Craft History Widget"){
			{a = (Utils.getprefb("showCraftHistoryBar", false));}
			public void set(boolean val) {
				Utils.setprefb("showCraftHistoryBar", val);
				a = val;
				if (ui != null && ui.gui != null && ui.gui.histbelt != null){
					ui.gui.histbelt.show(val);
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = add(toggleQualityDisplayCheckBox = new CheckBox("Display Quality on Inventory Items"){
			{a = (Utils.getprefb("qtoggle", true));}
			public void set(boolean val) {
				Utils.setprefb("qtoggle", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(roundedQualityCheckBox = new CheckBox("Rounded Quality Number"){
			{a = (Utils.getprefb("roundedQuality", true));}
			public void set(boolean val) {
				Utils.setprefb("roundedQuality", val);
				if (ui != null && ui.gui != null) {
					for (WItem item : ui.gui.getAllItemsFromAllInventoriesAndStacks()) {
						item.reloadItemOls();
					}
					for (Widget window : ui.gui.getAllWindows()){
						for (Widget w = window.lchild; w != null; w = w.prev) {
							if (w instanceof Equipory) {
								for (WItem equitem : ((Equipory) w).slots) {
									if (equitem != null) {
										equitem.reloadItemOls();
									}
								}
							}
						}
					}
				}
				a = val;
			}
		}, leftColumn.pos("bl").adds(16, 2));

		leftColumn = add(showQualityBackgroundCheckBox = new CheckBox("Show Quality Background"){
			{a = (Utils.getprefb("showQualityBackground", false));}
			public void set(boolean val) {
				Utils.setprefb("showQualityBackground", val);
				if (ui != null && ui.gui != null) {
					for (WItem item : ui.gui.getAllItemsFromAllInventoriesAndStacks()) {
						item.reloadItemOls();
					}
					for (Widget window : ui.gui.getAllWindows()){
						for (Widget w = window.lchild; w != null; w = w.prev) {
							if (w instanceof Equipory) {
								for (WItem equitem : ((Equipory) w).slots) {
									if (equitem != null) {
										equitem.reloadItemOls();
									}
								}
							}
						}
					}
				}
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(alwaysShowHealthBarCheckBox = new CheckBox("Always show Combat UI Health Bar"){
			{a = (Utils.getprefb("alwaysShowHealthBar", false));}
			public void set(boolean val) {
				Utils.setprefb("alwaysShowHealthBar", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12).x(0));
		leftColumn = add(alwaysShowStaminaBarCheckBox = new CheckBox("Always show Combat UI Stamina Bar"){
			{a = (Utils.getprefb("alwaysShowStaminaBar", false));}
			public void set(boolean val) {
				Utils.setprefb("alwaysShowStaminaBar", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(requireShiftHoverStacksCheckBox = new CheckBox("Show Hover-Inventories (Stacks, Belt, etc.) only when holding Shift"){
			{a = (Utils.getprefb("requireShiftHoverStacks", false));}
			public void set(boolean val) {
				Utils.setprefb("requireShiftHoverStacks", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12));
		leftColumn = add(objectPermanentHighlightingCheckBox = new CheckBox("Allow Object Permanent Highlighting on Alt + Middle Click (Mouse Scroll Click)"){
			{a = (Utils.getprefb("objectPermanentHighlighting", false));}
			public void set(boolean val) {
				Utils.setprefb("objectPermanentHighlighting", val);
				a = val;
				if (!val) {
					if (ui != null && ui.gui != null)
						ui.sess.glob.oc.gobAction(Gob::removePermanentHighlight);
					Gob.listHighlighted.clear();
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = add(showStudyWindowHistoryCheckBox = new CheckBox("Show Study Report History"){
			{a = (Utils.getprefb("showStudyWindowHistory", false));}
			public void set(boolean val) {
				CharWnd.showStudyWindowHistoryCheckBox.a = val;
				Utils.setprefb("showStudyWindowHistory", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12));

		rightColumn = add(disableMenuGridHotkeysCheckBox = new CheckBox("Disable All Menu Grid Hotkeys"){
			{a = (Utils.getprefb("disableMenuGridHotkeys", false));}
			public void set(boolean val) {
				Utils.setprefb("disableMenuGridHotkeys", val);
				a = val;
			}
		}, leftColumn.pos("ur").adds(0, 0).x(UI.scale(230)));

		rightColumn = add(enableMineSweeperCheckBox = new CheckBox("Show Mine Sweeper Numbers"){
			{a = (Utils.getprefb("enableMineSweeper", true));}
			public void set(boolean val) {
				Utils.setprefb("enableMineSweeper", val);
				if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
					ui.gui.miningSafetyAssistantWindow.enableMineSweeperCheckBox.a = val;
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(new Label("Sweeper Display Duration (Min):"), rightColumn.pos("bl").adds(0, 2));

		rightColumn.tooltip = RichText.render("Use this to set how long you want the numbers to be displayed on the ground, in minutes. The numbers will be visible as long as the dust particle effect stays on the tile." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Changing this option will only affect the duration of newly spawned cave dust tiles. The duration is set once the wall tile is mined and the cave dust spawns in.}", UI.scale(300));

		add(sweeperDurationDropbox = new Dropbox<Integer>(UI.scale(40), sweeperDurations.size(), UI.scale(17)) {
			{
				super.change(sweeperDurations.get(sweeperSetDuration));
			}
			@Override
			protected Integer listitem(int i) {
				return sweeperDurations.get(i);
			}
			@Override
			protected int listitems() {
				return sweeperDurations.size();
			}
			@Override
			protected void drawitem(GOut g, Integer item, int i) {
				g.aimage(Text.renderstroked(item.toString()).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(Integer item) {
				super.change(item);
				sweeperSetDuration = sweeperDurations.indexOf(item);
				System.out.println(sweeperSetDuration);
				Utils.setprefi("sweeperSetDuration", sweeperDurations.indexOf(item));
				if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
					ui.gui.miningSafetyAssistantWindow.sweeperDurationDropbox.change2(item);
			}
		}, rightColumn.pos("ul").adds(160, 2));
		leftColumn = add(lockStudyWindowCheckBox = new CheckBox("Lock Study Report"){
			{a = (Utils.getprefb("lockStudyWindow", false));}
			public void set(boolean val) {
				CharWnd.lockStudyWindowCheckBox.a = val;
				Utils.setprefb("lockStudyWindow", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2).x(0));
		leftColumn = add(playSoundOnFinishedCurioCheckBox = new CheckBox("Sound Alert for Finished Curiosities"){
			{a = (Utils.getprefb("playSoundOnFinishedCurio", false));}
			public void set(boolean val) {
				CharWnd.playSoundOnFinishedCurioCheckBox.a = val;
				Utils.setprefb("playSoundOnFinishedCurio", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));

		rightColumn = add(enableCornerFPSCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("CornerFPSSettingBool", true));}
			public void set(boolean val) {
				GLPanel.Loop.enableCornerFPSSetting = val;
				Utils.setprefb("CornerFPSSettingBool", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 18).x(UI.scale(230)));

		rightColumn = add(enableSnapWindowsBackInsideCheckBox = new CheckBox("Snap windows back when dragged out"){
			{a = (Utils.getprefb("snapWindowsBackInside", true));}
			public void set(boolean val) {
				Utils.setprefb("snapWindowsBackInside", val);
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(enableDragWindowsInWhenResizingCheckBox = new CheckBox("Drag windows in when resizing game"){
			{a = (Utils.getprefb("dragWindowsInWhenResizing", false));}
			public void set(boolean val) {
				Utils.setprefb("dragWindowsInWhenResizing", val);
				a = val;
			}
		}, rightColumn.pos("bl").adds(0, 2));

		Label expWindowLabel;
		rightColumn = add(expWindowLabel = new Label("New Experience Event Window Location:"), rightColumn.pos("bl").adds(0, 11));{
			boolean[] done = {false};
			RadioGroup expWindowGrp = new RadioGroup(this) {
				public void changed(int btn, String lbl) {
					if(!done[0])
						return;
					try {
						if(btn==0) {
							Utils.setprefb("expWindowLocationIsTop", true);
							expWindowLocationIsTop = true;
						}
						if(btn==1) {
							Utils.setprefb("expWindowLocationIsTop", false);
							expWindowLocationIsTop = false;
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			rightColumn = expWindowGrp.add("Top", rightColumn.pos("bl").adds(36, 3));
			rightColumn = expWindowGrp.add("Bottom", rightColumn.pos("ur").adds(32, 0));

			if (Utils.getprefb("expWindowLocationIsTop", true)){
				expWindowGrp.check(0);
			} else {
				expWindowGrp.check(1);
			}
			done[0] = true;
		}
		expWindowLabel.tooltip = RichText.render("This option sets where the Experience Event Notification Window will appear." +
				"\n$col[185,185,185]{Both the \"Top\" and \"Bottom\" locations are out of the way, unlike Loftar's default position.}", UI.scale(320));

		prev = add(new Label("Advanced Display Settings"), leftColumn.pos("bl").adds(0, 18).x(UI.scale(150)));

		leftColumn = add(toggleGobHealthDisplayCheckBox = new CheckBox("Display Object Health Percentage"){
			{a = (Utils.getprefb("gobHealthDisplayToggle", true));}
			public void set(boolean val) {
				Utils.setprefb("gobHealthDisplayToggle", val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 18).x(0));
		leftColumn = add(toggleGobQualityInfoCheckBox = new CheckBox("Display Object Quality on Inspection"){
			{a = (Utils.getprefb("showGobQualityInfo", true));}
			public void set(boolean val) {
				Utils.setprefb("showGobQualityInfo", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::qualityInfoUpdated);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = add(toggleGobGrowthInfoCheckBox = new CheckBox("Display Growth Info on Plants"){
			{a = (Utils.getprefb("showGobGrowthInfo", false));}
			public void set(boolean val) {
				Utils.setprefb("showGobGrowthInfo", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
					ui.gui.optionInfoMsg("Plant Growth Info is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(showMineSupportRadiiCheckBox = new CheckBox("Show Mine Support Radii"){
			{a = (Utils.getprefb("showMineSupportRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showMineSupportRadii", val);
				a = val;
				MSRad.show(val);
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::toggleMineLadderRadius);
					ui.gui.optionInfoMsg("Mine Support Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, leftColumn.pos("bl").adds(0, 12));
		leftColumn = add(showMineSupportSafeTilesCheckBox = new CheckBox("Show Mine Support Safe Tiles"){
			{a = (Utils.getprefb("showMineSupportTiles", false));}
			public void set(boolean val) {
				Utils.setprefb("showMineSupportTiles", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::settingUpdateMiningSupports);
					ui.gui.optionInfoMsg("Mine Support Safe Tiles are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(showBeeSkepsRadiiCheckBox = new CheckBox("Show Bee Skeps Radii"){
			{a = (Utils.getprefb("showBeeSkepsRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showBeeSkepsRadii", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::toggleBeeSkepRadius);
					ui.gui.optionInfoMsg("Bee Skeps Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, leftColumn.pos("bl").adds(0, 12));

		leftColumn = add(showFoodTroughsRadiiCheckBox = new CheckBox("Show Food Troughs Radii"){
			{a = (Utils.getprefb("showFoodTroughsRadii", false));}
			public void set(boolean val) {
				Utils.setprefb("showFoodTroughsRadii", val);
				a = val;
				if (ui != null && ui.gui != null){
					ui.sess.glob.oc.gobAction(Gob::toggleTroughsRadius);
					ui.gui.optionInfoMsg("Food Troughs Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(displayGatePassabilityBoxesCheckBox = new CheckBox("Display Gate Combat Passability"){
			{a = (Utils.getprefb("displayGatePassabilityBoxes", false));}
			public void set(boolean val) {
				Utils.setprefb("displayGatePassabilityBoxes", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.gui.optionInfoMsg("Gate Combat Passability is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgRed));
				}
			}
		}, leftColumn.pos("bl").adds(0, 12));

		leftColumn = add(highlightCliffsCheckBox = new CheckBox("Highlight Cliffs (Color Overlay)"){
			{a = (Utils.getprefb("highlightCliffs", false));}
			public void set(boolean val) {
				Utils.setprefb("highlightCliffs", val);
				a = val;
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("Cliff Highlighting is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed));
				}
			}
		}, leftColumn.pos("bl").adds(0, 12));

		rightColumn = add(toggleGobCollisionBoxesDisplayCheckBox = new CheckBox("Show Object Collision Boxes"){
			{a = (Utils.getprefb("gobCollisionBoxesDisplayToggle", false));}
			public void set(boolean val) {
				Utils.setprefb("gobCollisionBoxesDisplayToggle", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
			}
		}, prev.pos("bl").adds(0, 18).x(UI.scale(230)));

		rightColumn = add(toggleBeastDangerRadiiCheckBox = new CheckBox("Show Animal Danger Radii"){
			{a = (Utils.getprefb("beastDangerRadii", true));}
			public void set(boolean val) {
				Utils.setprefb("beastDangerRadii", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleBeastDangerRadii);
					ui.gui.optionInfoMsg("Animal Danger Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(toggleCritterAurasCheckBox = new CheckBox("Show Critter Circle Auras"){
			{a = (Utils.getprefb("critterAuras", false));}
			public void set(boolean val) {
				Utils.setprefb("critterAuras", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
					ui.gui.optionInfoMsg("Critter Circle Auras are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}

			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(toggleSpeedBoostAurasCheckBox = new CheckBox("Show Speed Boost Circle Auras"){
			{a = (Utils.getprefb("SpeedBoostAuras", true));}
			public void set(boolean val) {
				Utils.setprefb("SpeedBoostAuras", val);
				a = val;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleSpeedBuffAuras);
					ui.gui.optionInfoMsg("Speed Boost Circle Auras are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray));
				}
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(showContainerFullnessCheckBox = new CheckBox("Highlight Container Fullness"){
			{a = (Utils.getprefb("showContainerFullness", true));}
			public void set(boolean val) {
				Utils.setprefb("showContainerFullness", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
			}
		}, rightColumn.pos("bl").adds(0, 12));
		add(new Label("Show:"), rightColumn.pos("bl").adds(0, 2));
		rightColumn = add(showContainerFullnessRedCheckBox = new CheckBox("Full"){
			{a = (Utils.getprefb("showContainerFullnessRed", true));}
			public void set(boolean val) {
				Utils.setprefb("showContainerFullnessRed", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
			}
		}, rightColumn.pos("bl").adds(36, 3));
		showContainerFullnessRedCheckBox.lbl = Text.create("Full", PUtils.strokeImg(Text.std.render("Full", new Color(185,0,0,255))));
		add(showContainerFullnessYellowCheckBox = new CheckBox("Some"){
			{a = (Utils.getprefb("showContainerFullnessYellow", true));}
			public void set(boolean val) {
				Utils.setprefb("showContainerFullnessYellow", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
			}
		}, rightColumn.pos("ur").adds(5, 0));
		showContainerFullnessYellowCheckBox.lbl = Text.create("Some", PUtils.strokeImg(Text.std.render("Some", new Color(224,213,0,255))));
		add(showContainerFullnessGreenCheckBox = new CheckBox("Empty"){
			{a = (Utils.getprefb("showContainerFullnessGreen", true));}
			public void set(boolean val) {
				Utils.setprefb("showContainerFullnessGreen", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
			}
		}, rightColumn.pos("ur").adds(61, 0));
		showContainerFullnessGreenCheckBox.lbl = Text.create("Empty", PUtils.strokeImg(Text.std.render("Empty", new Color(0,185,0,255))));
		rightColumn = add(showWorkstationStageCheckBox = new CheckBox("Highlight Workstation Progress"){
			{a = (Utils.getprefb("showWorkstationStage", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStage", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
			}
		}, rightColumn.pos("bl").adds(-36, 12));
		add(new Label("Show:"), rightColumn.pos("bl").adds(0, 2));
		rightColumn = add(showWorkstationStageRedCheckBox = new CheckBox("Finished"){
			{a = (Utils.getprefb("showWorkstationStageRed", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStageRed", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
			}
		}, rightColumn.pos("bl").adds(36, 3));
		showWorkstationStageRedCheckBox.lbl = Text.create("Finished", PUtils.strokeImg(Text.std.render("Finished", new Color(185,0,0,255))));
		add(showWorkstationStageYellowCheckBox = new CheckBox("In progress"){
			{a = (Utils.getprefb("showWorkstationStageYellow", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStageYellow", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
			}
		}, rightColumn.pos("ur").adds(9, 0));
		showWorkstationStageYellowCheckBox.lbl = Text.create("In progress", PUtils.strokeImg(Text.std.render("In progress", new Color(224,213,0,255))));
		rightColumn = add(showWorkstationStageGreenCheckBox = new CheckBox("Prepared"){
			{a = (Utils.getprefb("showWorkstationStageGreen", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStageGreen", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		showWorkstationStageGreenCheckBox.lbl = Text.create("Prepared", PUtils.strokeImg(Text.std.render("Prepared", new Color(0,185,0,255))));
		add(showWorkstationStageGrayCheckBox = new CheckBox("Unprepared"){
			{a = (Utils.getprefb("showWorkstationStageGray", true));}
			public void set(boolean val) {
				Utils.setprefb("showWorkstationStageGray", val);
				a = val;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
			}
		}, rightColumn.pos("ur").adds(6, 0));
		showWorkstationStageGrayCheckBox.lbl = Text.create("Unprepared", PUtils.strokeImg(Text.std.render("Unprepared", new Color(160,160,160,255))));


		add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 18).x(UI.scale(117)));
		setTooltipsForInterfaceSettingsStuff();
	    pack();
	}
    }

    private static final Text kbtt = RichText.render("$col[255,200,0]{Escape}: Cancel input\n" +
						     "$col[255,200,0]{Backspace}: Revert to default\n" +
						     "$col[255,200,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		private int addbtnImproved(Widget cont, String nm, String tooltip, Color color, KeyBinding cmd, int y) {
			Label theLabel = new Label(nm);
			if (tooltip != null && !tooltip.equals(""))
				theLabel.tooltip = RichText.render(tooltip, UI.scale(300));
			theLabel.setcolor(color);
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					theLabel, new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public BindingPanel(Panel back) {
			super();
			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(310, 360))), 0, 0);
			Widget cont = scroll.cont;
			Widget prev;
			int y = 0;
			y = cont.adda(new Label(""), 0, y, 0, 0.0).pos("bl").adds(0, 5).y;
			Label topNote = new Label("Don't use the same keys on multiple Keybinds!");
			topNote.setcolor(Color.RED);
			y = cont.adda(topNote, cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = cont.adda(new Label("If you do that, only one of them will work. God knows which."), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = cont.adda(new Label(""), 0, y, 0, 0.0).pos("bl").adds(0, 5).y;
			y = cont.adda(new Label("Main menu"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Inventory", GameUI.kb_inv, y);
			y = addbtn(cont, "Equipment", GameUI.kb_equ, y);
			y = addbtn(cont, "Character sheet", GameUI.kb_chr, y);
			y = addbtn(cont, "Map window", GameUI.kb_map, y);
			y = addbtn(cont, "Kith & Kin", GameUI.kb_bud, y);
			y = addbtn(cont, "Options", GameUI.kb_opt, y);
			y = addbtn(cont, "Search actions", GameUI.kb_srch, y);
			y = addbtn(cont, "Focus chat window", GameUI.kb_chat, y);
			y = addbtn(cont, "Minimap icons", GameUI.kb_ico, y);
			y = addbtn(cont, "Toggle UI", GameUI.kb_hide, y);
			y = addbtn(cont, "Log out", GameUI.kb_logout, y);
			y = addbtn(cont, "Switch character", GameUI.kb_switchchr, y);
			y = cont.adda(new Label("Map Buttons"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Reset view", MapWnd.kb_home, y);
			y = addbtn(cont, "Compact mode", MapWnd.kb_compact, y);
			y = addbtn(cont, "Show Personal Claims on Map", MapWnd.kb_claim, y);
			y = addbtn(cont, "Show Village Claims on Map", MapWnd.kb_vil, y);
			y = addbtn(cont, "Show Realm Provinces on Map", MapWnd.kb_prov, y);
			y = addbtn(cont, "Hide markers", MapWnd.kb_hmark, y);
			y = addbtn(cont, "Add marker", MapWnd.kb_mark, y);
			y = cont.adda(new Label("World Toggles"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Display Personal Claims", GameUI.kb_claim, y);
			y = addbtn(cont, "Display Village Claims", GameUI.kb_vil, y);
			y = addbtn(cont, "Display Realm Provinces", GameUI.kb_rlm, y);
			y = addbtn(cont, "Display Tile Grid", MapView.kb_grid, y);
			y = cont.adda(new Label("Camera control"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			//y = addbtn(cont, "Rotate left", MapView.kb_camleft, y);
			//y = addbtn(cont, "Rotate right", MapView.kb_camright, y);
			//y = addbtn(cont, "Zoom in", MapView.kb_camin, y);
			//y = addbtn(cont, "Zoom out", MapView.kb_camout, y);
			y = addbtn(cont, "Snap North", MapView.kb_camSnapNorth, y);
			y = addbtn(cont, "Snap South", MapView.kb_camSnapSouth, y);
			y = addbtn(cont, "Snap East", MapView.kb_camSnapEast, y);
			y = addbtn(cont, "Snap West", MapView.kb_camSnapWest, y);
			y = cont.adda(new Label("Walking speed"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtn(cont, "Increase speed", Speedget.kb_speedup, y);
			y = addbtn(cont, "Decrease speed", Speedget.kb_speeddn, y);
			for (int i = 0; i < 4; i++)
				y = addbtn(cont, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i], y);
			y = cont.adda(new Label("Combat actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < Fightsess.kb_acts.length; i++)
				y = addbtn(cont, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i], y);
			y = addbtnImproved(cont, "Cycle through targets", "This only cycles through the targets you are currently engaged in combat with.", Color.WHITE, Fightsess.kb_relcycle, y);
			y = addbtnImproved(cont, "Switch to nearest target", "This only switches to the nearest target you are currently engaged in combat with.", Color.WHITE, Fightsess.kb_nearestTarget, y);
			y = addbtnImproved(cont, "Aggro Nearest Player/Animal", "Selects the nearest Player or Animal to attack, based on your situation:" +
					"\n\n$col[218,163,0]{Case 1:} $col[185,185,185]{If you are in combat with Players, it will only attack other not-already-aggroed players.}" +
					"\n$col[218,163,0]{Case 2:} $col[185,185,185]{If you are in combat with Animals, it will try to attack the closest not-already-aggroed player. If none is found, try to attack the closest animal. Once this happens, you're back to Case 1.}" +
					"\n\n$col[218,163,0]{Note:} $col[185,185,185]{Party members will never be attacked by this button. Village or Realm members will not be attacked unless you have them marked as $col[185,0,0]{Red} in your Kin List.}", new Color(255, 0, 0,255), GameUI.kb_aggroNearestTargetButton, y+6);
			y = addbtnImproved(cont, "Aggro Nearest Player", "Selects the nearest non-aggroed Player to attack.", new Color(255, 0, 0,255), GameUI.kb_aggroNearestPlayerButton, y+6);

			//
			y = addbtnImproved(cont, "Aggro all Non-Friendly players.", "", new Color(255, 0, 0,255), GameUI.kb_aggroAllNonFriendlyPlayers, y);
			y = addbtnImproved(cont, "Re-Aggro Last Target", "", new Color(255, 68, 0,255), GameUI.kb_aggroLastTarget, y);
			y = addbtnImproved(cont, "Peace Current Target", "", new Color(0, 255, 34,255), GameUI.kb_peaceCurrentTarget, y);

			y = cont.adda(new Label("Other Custom features"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			y = addbtnImproved(cont, "Drink Button", "", new Color(0, 140, 255, 255), GameUI.kb_drinkButton, y);
			y = addbtnImproved(cont, "Night Vision / Brighter World", "This will simulate daytime lighting during the night. \n$col[185,185,185]{It slightly affects the light levels during the day too.}" +
					"\n$col[218,163,0]{Note:} $col[185,185,185]{This keybind just switches the value of Night Vision / Brighter World between Minimum and Maximum. This can also be set more precisely using the slider in the Graphics Settings.}", Color.WHITE, GameUI.kb_nightVision, y);

			y = addbtnImproved(cont, "Pick/Click Nearest Object","When this button is pressed, you will instantly click the nearest Forageable, Critter, or Non-Visitor Gate." +
					"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestObject, y+6);
			y = addbtnImproved(cont, "Click Nearest Non-Visitor Gate", "This works the same as the button above, but it has a smaller range (for a slightly better precision) and completely ignores Forageables and Critters." +
					"\n$col[218,163,0]{Range:} $col[185,185,185]{8 tiles (approximately)}" +
					"\n$col[185,185,185]{Use this one if you only want a dedicated button for gates, without the other stuff.}", new Color(255, 188, 0,255), GameUI.kb_clickNearestGate, y);
			y = addbtnImproved(cont, "Click Nearest Door Or Ladder","When this button is pressed, you will instantly click the nearest Door, Stairs, or Ladder." +
					"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestDoorLadder, y);


			y = addbtnImproved(cont, "Hop on Nearest Vehicle/Mount", "When this button is pressed, your character will run towards the nearest mountable Vehicle/Animal, and try to mount it." +
					"\n\nThe behavior differs for every object:" +
					"\n=====================" +
					"\n$col[218,163,0]{Tamed Horses:} Whistle at the Horse, then run to it and mount." +
					"\n$col[218,163,0]{Rowboats, Dugouts, Kicksleds:} Run to it and mount." +
					"\n$col[218,163,0]{Knarrs, Snekkjas:} Run to it and try to man the helm. If helm is occupied, join as passenger." +
					"\n=====================" +
					"\n$col[200,0,0]{WARNING:} Only the Vehicles/Mounts mentioned above are taken into consideration! " +
					"\nOther Vehicles/Mounts are completely ignored ($col[200,0,0]{Coracles}, $col[200,0,0]{Skis}, $col[200,0,0]{Wagons}, $col[200,0,0]{Wild Horses}, etc.)!" +
					"\n\n$col[218,163,0]{Range:} $col[185,185,185]{36 tiles (approximately)}", new Color(0, 197, 255,255), GameUI.kb_enterNearestVessel, y);

			y = addbtn(cont, "Left Hand (Quick switch)", GameUI.kb_leftQuickSlotButton, y+6);
			y = addbtn(cont, "Right Hand (Quick switch)", GameUI.kb_rightQuickSlotButton, y);

			y = addbtn(cont, "Toggle Collision Boxes", GameUI.kb_toggleCollisionBoxes, y+6);
			y = addbtn(cont, "Toggle Object Hiding", GameUI.kb_toggleHidingBoxes, y);

			// ND: Might delete these eventually, commented them for now
//			y = addbtn(cont, "Mute Non-Friendly", GameUI.kb_toggleMuteNonFriendly, y+26);
//			y = addbtn(cont, "Toggle Walk Pathfinder", GameUI.kb_toggleWalkWithPathfinder, y);
//			y = addbtn(cont, "Button For Testing", GameUI.kb_buttonForTesting, y);


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
			prev = add(new Label("Enabled Action Bars:"), 0, 0);
			prev = add(new CheckBox("Horizontal Action Bar 1"){
				{a = Utils.getprefb("showActionBar1", true);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar1", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar1 != null){
						ui.gui.actionBar1.show(val);
					}
					a = val;
				}
			}, prev.pos("bl").adds(12, 6));
			prev = add(new CheckBox("Horizontal Action Bar 2"){
				{a = Utils.getprefb("showActionBar2", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar2", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar2 != null){
						ui.gui.actionBar2.show(val);
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(new CheckBox("Vertical Action Bar 1"){
				{a = Utils.getprefb("showActionBar3", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar3", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar3 != null){
						ui.gui.actionBar3.show(val);
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(new CheckBox("Vertical Action Bar 2"){
				{a = Utils.getprefb("showActionBar4", false);}
				public void set(boolean val) {
					Utils.setprefb("showActionBar4", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar4 != null){
						ui.gui.actionBar4.show(val);
					}
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(280, 380))), prev.pos("bl").adds(0,10).x(0));
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

	private Label nightVisionLabel;
	public static HSlider nightVisionSlider;
	private Button nightVisionResetButton;
	public static CheckBox simpleCropsCheckBox;
	public static CheckBox simpleForageablesCheckBox;
	public static CheckBox disableWeatherEffectsCheckBox;
	public static CheckBox disableFlavourObjectsCheckBox;
	public static CheckBox flatWorldCheckBox;
	public static CheckBox tileSmoothingCheckBox;
	public static CheckBox tileTransitionsCheckBox;
	public static CheckBox flatCaveWallsCheckBox;
	public static HSlider treesAndBushesScaleSlider;
	public static CheckBox disableTreeAndBushSwayingCheckBox;
	public static CheckBox disableScentSmoke;
	public static CheckBox disableIndustrialSmoke;
	public static CheckBox disableSomeGobAnimations;
	public class NDWorldGraphicsSettingsPanel extends Panel {
		public NDWorldGraphicsSettingsPanel(Panel back) {
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

			prev = add(new Label("World Visuals:"), prev.pos("bl").adds(0, 12));
			prev = add(disableWeatherEffectsCheckBox = new CheckBox("Disable Weather (Requires Reload)"){
				{a = Utils.getprefb("isWeatherDisabled", false);}
				public void set(boolean val) {
					Utils.setprefb("isWeatherDisabled", val);
					a = val;
				}
			}, prev.pos("bl").adds(12, 8));
			prev = add(simpleCropsCheckBox = new CheckBox("Simplified Crops (Requires Reload)"){
				{a = Utils.getprefb("simplifiedCrops", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedCrops", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(simpleForageablesCheckBox = new CheckBox("Simplified Forageables (Requires Reload)"){
				{a = Utils.getprefb("simplifiedForageables", false);}
				public void set(boolean val) {
					Utils.setprefb("simplifiedForageables", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(disableFlavourObjectsCheckBox = new CheckBox("Hide Flavour Objects"){
				{a = Utils.getprefb("disableFlavourObjects", false);}
				public void set(boolean val) {
					Utils.setprefb("disableFlavourObjects", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Flavour Objects are now now " + (val ? "HIDDEN" : "SHOWN") + "!", (val ? msgGray : msgGreen));
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(flatWorldCheckBox = new CheckBox("Flat World"){
				{a = Utils.getprefb("flatWorld", false);}
				public void set(boolean val) {
					Utils.setprefb("flatWorld", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Flat World is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed));
					}
				}
			}, prev.pos("bl").adds(0, 12));

			prev = add(tileSmoothingCheckBox = new CheckBox("Disable Tile Smoothing"){
				{a = Utils.getprefb("noTileSmoothing", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileSmoothing", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Tile Smoothing is now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? msgRed : msgGreen));
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(tileTransitionsCheckBox = new CheckBox("Disable Tile Transitions"){
				{a = Utils.getprefb("noTileTransitions", false);}
				public void set(boolean val) {
					Utils.setprefb("noTileTransitions", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Tile Transitions are now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? msgRed : msgGreen));
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(flatCaveWallsCheckBox = new CheckBox("Flat Cave Walls"){
				{a = Utils.getprefb("flatCaveWalls", false);}
				public void set(boolean val) {
					Utils.setprefb("flatCaveWalls", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
				}
			}, prev.pos("bl").adds(0, 2));


			prev = add(new Label("Trees & Bushes Scale:"), prev.pos("bl").adds(0, 10).x(0));
			prev = add(treesAndBushesScaleSlider = new HSlider(UI.scale(200), 10, 100, Utils.getprefi("treesAndBushesScale", 100)) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = Utils.getprefi("treesAndBushesScale", 100);
				}
				public void changed() {
					Utils.setprefi("treesAndBushesScale", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
					}
				}
			}, prev.pos("bl").adds(0, 6));

			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				treesAndBushesScaleSlider.val = 100;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
				}
				Utils.setprefi("treesAndBushesScale", 100);
			}), prev.pos("bl").adds(210, -20));
			prev.tooltip = RichText.render("Reset to default");

			prev = add(disableTreeAndBushSwayingCheckBox = new CheckBox("Disable Tree & Bush Swaying"){
				{a = Utils.getprefb("disableTreeAndBushSwaying", false);}
				public void set(boolean val) {
					Utils.setprefb("disableTreeAndBushSwaying", val);
					a = val;
					if (ui != null && ui.gui != null)
						ui.sess.glob.oc.gobAction(Gob::reloadTreeSwaying);
				}
			}, prev.pos("bl").adds(12, 2));

			prev = add(disableSomeGobAnimations = new CheckBox("Disable Gob Animations"){
				{a = (Utils.getprefb("disableSomeGobAnimations", false));}
				public void set(boolean val) {
					Utils.setprefb("disableSomeGobAnimations", val);
					a = val;
					Gob.disableGlobalGobAnimations = val;
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(disableIndustrialSmoke = new CheckBox("Disable Industrial Smoke (Requires Reload)"){
				{a = (Utils.getprefb("disableIndustrialSmoke", false));}
				public void set(boolean val) {
					Utils.setprefb("disableIndustrialSmoke", val);
					Gob.disableIndustrialSmoke = val;
					a = val;
					synchronized (ui.sess.glob.oc){
						for(Gob gob : ui.sess.glob.oc){
							if(gob.getres() != null && !gob.getres().name.equals("gfx/terobjs/clue")){
								synchronized (gob.ols){
									for(Gob.Overlay ol : gob.ols){
										if(ol.res != null && ol.res.get() != null && ol.res.get().name.contains("ismoke")){
											gob.removeOl(ol);
										}
									}
								}
								gob.ols.clear();
							}
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(disableScentSmoke = new CheckBox("Disable Scent Smoke (Requires Reload)"){
				{a = (Utils.getprefb("disableScentSmoke", false));}
				public void set(boolean val) {
					Utils.setprefb("disableScentSmoke", val);
					Gob.disableScentSmoke = val;
					a = val;
					synchronized (ui.sess.glob.oc){
						for(Gob gob : ui.sess.glob.oc){
							if(gob.getres() != null && gob.getres().name.equals("gfx/terobjs/clue")){
								synchronized (gob.ols){
									for(Gob.Overlay ol : gob.ols){
										gob.removeOl(ol);
									}
								}
								gob.ols.clear();
							}
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(40)));
			setTooltipsForGraphicsSettingsStuff();
			pack();
		}
	}

	private Label freeCamZoomSpeedLabel;
	public static HSlider freeCamZoomSpeedSlider;
	private Button freeCamZoomSpeedResetButton;
	private Label freeCamHeightLabel;
	public static HSlider freeCamHeightSlider;
	private Button freeCamHeightResetButton;
	public static CheckBox unlockedOrthoCamCheckBox;
	private Label orthoCamZoomSpeedLabel;
	public static HSlider orthoCamZoomSpeedSlider;
	private Button orthoCamZoomSpeedResetButton;
	private CheckBox revertOrthoCameraAxesCheckBox;
	private CheckBox revertFreeCameraXAxisCheckBox;
	private CheckBox revertFreeCameraYAxisCheckBox;
	public static CheckBox allowLowerFreeCamTiltCheckBox;
	public class NDCamSettingsPanel extends Panel {

		public NDCamSettingsPanel(Panel back) {
			Widget prev; // ND: this will be visible with both camera type settings
			Widget FreePrev; // ND: used to calculate the positions for the NDFree camera settings
			Widget OrthoPrev; // ND: used to calculate the positions for the NDOrtho camera settings

			MapView.NDrevertOrthoAxis(Utils.getprefb("CamAxisSettingBool", true));
			MapView.NDrevertfreeCamXAxis(Utils.getprefb("FreeCamXAxisSettingBool", true));
			MapView.NDrevertfreeCamYAxis(Utils.getprefb("FreeCamYAxisSettingBool", true));

			prev = add(new Label(""), 0, 0);

			prev = add(new Label("Selected Camera Settings:"), prev.pos("bl").adds(0, 44));
			prev = add(revertFreeCameraXAxisCheckBox = new CheckBox("Revert X Axis"){
				{a = (Utils.getprefb("FreeCamXAxisSettingBool", true));}
				public void set(boolean val) {
					Utils.setprefb("FreeCamXAxisSettingBool", val);
					MapView.NDrevertfreeCamXAxis(val);
					a = val;
				}
			}, prev.pos("bl").adds(12, 2));
			add(revertFreeCameraYAxisCheckBox = new CheckBox("Revert Y Axis"){
				{a = (Utils.getprefb("FreeCamYAxisSettingBool", true));}
				public void set(boolean val) {
					Utils.setprefb("FreeCamYAxisSettingBool", val);
					MapView.NDrevertfreeCamYAxis(val);
					a = val;
				}
			}, prev.pos("ul").adds(110, 0));

			prev = add(revertOrthoCameraAxesCheckBox = new CheckBox("Revert Ortho Look Axis"){
				{a = (Utils.getprefb("CamAxisSettingBool", true));}
				public void set(boolean val) {
					Utils.setprefb("CamAxisSettingBool", val);
					MapView.NDrevertOrthoAxis(val);
					a = val;
				}
			}, prev.pos("ul").adds(0, 0));
			OrthoPrev = add(unlockedOrthoCamCheckBox = new CheckBox("Unlocked Ortho Camera"){
				{a = Utils.getprefb("unlockedNDOrtho", true);}
				public void set(boolean val) {
					Utils.setprefb("unlockedNDOrtho", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			OrthoPrev = add(orthoCamZoomSpeedLabel = new Label("Ortho Camera Zoom Speed:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
			OrthoPrev = add(orthoCamZoomSpeedSlider = new HSlider(UI.scale(200), 2, 40, Utils.getprefi("orthoCamZoomSpeed", 10)) {
				public void changed() {
					Utils.setprefi("orthoCamZoomSpeed", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 4));
			add(orthoCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				orthoCamZoomSpeedSlider.val = 10;
				Utils.setprefi("orthoCamZoomSpeed", 10);
			}), OrthoPrev.pos("bl").adds(210, -20));

			// ND: Now the free camera settings
			FreePrev = add(allowLowerFreeCamTiltCheckBox = new CheckBox("Enable Lower Tilting Angle"){
				{a = (Utils.getprefb("allowLowerTiltBool", false));}
				public void set(boolean val) {
					Utils.setprefb("allowLowerTiltBool", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			FreePrev = add(freeCamZoomSpeedLabel = new Label("Free Camera Zoom Speed:"), FreePrev.pos("bl").adds(0, 10).x(0));
			FreePrev = add(freeCamZoomSpeedSlider = new HSlider(UI.scale(200), 4, 40, Utils.getprefi("freeCamZoomSpeed", 25)) {
				public void changed() {
					Utils.setprefi("freeCamZoomSpeed", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamZoomSpeedSlider.val = 25;
				Utils.setprefi("freeCamZoomSpeed", 25);
			}), FreePrev.pos("bl").adds(210, -20));
			FreePrev = add(freeCamHeightLabel = new Label("Free Camera Height:"), FreePrev.pos("bl").adds(0, 10));
			FreePrev = add(freeCamHeightSlider = new HSlider(UI.scale(200), 10, 300, (Math.round((float) Utils.getprefd("cameraHeightDistance", 15f)))*10) {
				public void changed() {
					Utils.setprefd("cameraHeightDistance", (float) (val/10));
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamHeightResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
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
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("NDFree");
								}
							}
							if(btn==1) {
								Utils.setpref("defcam", "NDOrtho");
								setFreeCameraSettingsVisibility(false);
								setOrthoCameraSettingsVisibility(true);
								MapView.publicCurrentCameraName = 2;
								MapView.publicOrthoCamDist = 150f;
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("NDOrtho");
								}
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
				prev = camGrp.add("Free Camera", prev.pos("bl").adds(16, 2));
				prev = camGrp.add("Ortho Camera", prev.pos("bl").adds(0, 1));

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

	public static CheckBox toggleTrackingOnLoginCheckBox;
	public static CheckBox toggleSwimmingOnLoginCheckBox;
	public static CheckBox toggleCriminalActsOnLoginCheckBox;
	public static CheckBox toggleSiegeEnginesOnLoginCheckBox;
	public static CheckBox togglePartyPermissionsOnLoginCheckBox;
	public static CheckBox toggleItemStackingOnLoginCheckBox;
	private Label defaultSpeedLabel;
	public static CheckBox instantFlowerMenuCTRLCheckBox;
	public static CheckBox autoFlowerCTRLSHIFTCheckBox;
	public static CheckBox autoswitchBunnyPlateBootsCheckBox;
	public static CheckBox saveCutleryCheckBox = null;
	public static CheckBox autoStudyCheckBox;
	public static CheckBox autoDropLeechesCheckBox = null;
	public static CheckBox noCursorItemDroppingCheckBox = null;
	public static CheckBox noCursorItemDroppingInWaterCheckBox = null;
	public static CheckBox autoDrinkTeaWhileWorkingCheckBox = null;

	public class NDGameplaySettingsPanel extends Panel {
		private final List<String> runSpeeds = Arrays.asList("Crawl", "Walk", "Run", "Sprint");
		private final int speedSetInt = Utils.getprefi("defaultSetSpeed", 2);
		public NDGameplaySettingsPanel(Panel back) {
			Widget prev;
			Widget rightColumn;

			prev = add(new Label("Toggle on Login:"), 0, 0);
			prev = add(toggleTrackingOnLoginCheckBox = new CheckBox("Tracking"){
				{a = Utils.getprefb("toggleTrackingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleTrackingOnLogin", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(UI.scale(16)));
			rightColumn = add(toggleSiegeEnginesOnLoginCheckBox = new CheckBox("Check for Siege Engines"){
				{a = Utils.getprefb("toggleSiegeEnginesOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleSiegeEnginesOnLogin", val);
					a = val;
				}
			}, prev.pos("ur").adds(60, 0));
			rightColumn = add(togglePartyPermissionsOnLoginCheckBox = new CheckBox("Party Permissions"){
				{a = Utils.getprefb("togglePartyPermissionsOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("togglePartyPermissionsOnLogin", val);
					a = val;
				}
			}, rightColumn.pos("bl").adds(0, 2));
			rightColumn = add(toggleItemStackingOnLoginCheckBox = new CheckBox("Automatic Item Stacking"){
				{a = Utils.getprefb("toggleItemStackingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleItemStackingOnLogin", val);
					a = val;
				}
			}, rightColumn.pos("bl").adds(0, 2));
			prev = add(toggleSwimmingOnLoginCheckBox = new CheckBox("Swimming"){
				{a = Utils.getprefb("toggleSwimmingOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleSwimmingOnLogin", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(toggleCriminalActsOnLoginCheckBox = new CheckBox("Criminal Acts"){
				{a = Utils.getprefb("toggleCriminalActsOnLogin", false);}
				public void set(boolean val) {
					Utils.setprefb("toggleCriminalActsOnLogin", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(defaultSpeedLabel = new Label("Default Speed on Login:"), prev.pos("bl").adds(0, 16).x(0));
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
						g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
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
				}, prev.pos("bl").adds(130, -16));

			prev = add(new Label("Altered gameplay behavior:"), prev.pos("bl").adds(0, 14).x(0));
			prev = add(instantFlowerMenuCTRLCheckBox = new CheckBox("Instantly select 1st Flower-Menu Option when holding Ctrl"){
				{a = Utils.getprefb("instantFlowerMenuCTRL", true);}
				public void set(boolean val) {
					Utils.setprefb("instantFlowerMenuCTRL", val);
					a = val;
				}
			}, prev.pos("bl").adds(12, 6));
			prev = add(autoFlowerCTRLSHIFTCheckBox = new CheckBox("Run Auto Flower-Menu Repeater when holding Ctrl+Shift"){
				{a = Utils.getprefb("autoFlowerCTRLSHIFT", false);}
				public void set(boolean val) {
					Utils.setprefb("autoFlowerCTRLSHIFT", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoswitchBunnyPlateBootsCheckBox = new CheckBox("Autoswitch Bunny Slippers and Plate Boots from inventory"){
				{a = Utils.getprefb("autoswitchBunnyPlateBoots", true);}
				public void set(boolean val) {
					Utils.setprefb("autoswitchBunnyPlateBoots", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 12));

			prev = add(autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
				{a = Utils.getprefb("autoDropLeeches", false);}
				public void set(boolean val) {
					Utils.setprefb("autoDropLeeches", val);
					a = val;
					Equipory.autoDropLeechesCheckBox.a = val;
					if (ui != null && ui.gui != null) {
						Equipory eq = ui.gui.getequipory();
						if (eq != null && eq.player) { // ND: Probably an irrelevant check
							eq.checkForLeeches = true;
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(saveCutleryCheckBox = new CheckBox("Anti Cutlery Breakage (move to inventory before it breaks)"){
				{a = Utils.getprefb("antiCutleryBreakage", true);}
				public void set(boolean val) {
					Utils.setprefb("antiCutleryBreakage", val);
					a = val;
					TableInfo.saveCutleryCheckBox.a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(autoStudyCheckBox = new CheckBox("Auto-Study from Inventory"){
				{a = Utils.getprefb("autoStudy", false);}
				public void set(boolean val) {
					CharWnd.autoStudyCheckBox.a = val;
					Utils.setprefb("autoStudy", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(noCursorItemDroppingCheckBox = new CheckBox("No Cursor Item Dropping (Anywhere)"){
				{a = Utils.getprefb("noCursorItemDropping", false);}
				public void set(boolean val) {
					Utils.setprefb("noCursorItemDropping", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("No Item Dropping (Anywhere) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed));
					}
				}
			}, prev.pos("bl").adds(0, 12));

			prev = add(noCursorItemDroppingInWaterCheckBox = new CheckBox("No Cursor Item Dropping (Water Only)"){
				{a = Utils.getprefb("noCursorItemDroppingInWater", false);}
				public void set(boolean val) {
					Utils.setprefb("noCursorItemDroppingInWater", val);
					a = val;
					if (ui != null && ui.gui != null) {
						if (!noCursorItemDroppingCheckBox.a) {
							ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed));
						} else {
							ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!" + (val ? "" : " (WARNING!!!: No Item Dropping (Anywhere) IS STILL ENABLED, and it overwrites this option!)"), (val ? msgGreen : msgYellow));
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(autoDrinkTeaWhileWorkingCheckBox = new CheckBox("Automatically Drink Tea/Water While Working."){
				{a = Utils.getprefb("autoDrinkTeaOrWater", false);}
				public void set(boolean val) {
					Utils.setprefb("autoDrinkTeaOrWater", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Auto-drinking Tea and Water is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed));
					}
				}
			}, prev.pos("bl").adds(0, 12));

			prev = add(new Button(UI.scale(310), ">>> Auto-Select Manager (Flower Menus) <<<", () -> {
				if(autoFlowerWindow == null) {
					autoFlowerWindow = this.parent.parent.add(new AutoFlowerWindow());
					autoFlowerWindow.show();
				} else {
					autoFlowerWindow.show(!autoFlowerWindow.visible);
					autoFlowerWindow.refresh();
				}
			}),prev.pos("bl").adds(0, 10).x(0));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 20).x(UI.scale(55)));
			setTooltipsForGameplaySettingsStuff();
			pack();
		}
	}

	public static CheckBox useProperCombatUICheckBox;
	public static CheckBox showCombatHotkeysUICheckBox;
	public static CheckBox singleRowCombatMovesCheckBox;
	public static CheckBox drawFloatingCombatDataOnCurrentTargetCheckBox;
	public static CheckBox drawFloatingCombatDataOnOthersCheckBox;
	public static CheckBox combatInfoBackgroundToggledCheckBox;
	public static CheckBox markCurrentCombatTargetCheckBox;
	public static HSlider combatUITopPanelHeightSlider;
	public static HSlider combatUIBottomPanelHeightSlider;
	public static CheckBox toggleGobDamageInfoCheckBox;
	public static CheckBox toggleGobDamageWoundInfoCheckBox;
	public static CheckBox toggleGobDamageArmorInfoCheckBox;
	public static CheckBox toggleAutoPeaceCheckbox;
	public static CheckBox partyMembersHighlightCheckBox;
	public static CheckBox partyMembersCirclesCheckBox;
	public static CheckBox aggroedEnemiesCirclesCheckBox;
	public static CheckBox drawChaseVectorsCheckBox;
	public static Button damageInfoClearButton;
	public class NDCombatSettingsPanel extends Panel {
		public NDCombatSettingsPanel(Panel back) {
			Widget prev;

			prev = add(new Label("Combat UI:"), 0, 0);
			prev = add(useProperCombatUICheckBox = new CheckBox("Use Improved Combat UI"){
				{a = Utils.getprefb("useProperCombatUI", true);}
				public void set(boolean val) {
					Utils.setprefb("useProperCombatUI", val);
					a = val;
				}
			}, prev.pos("bl").adds(16, 6));
			prev = add(new Label("Top panel height (Improved UI):"), prev.pos("bl").adds(-16, 10));
			prev = add(combatUITopPanelHeightSlider = new HSlider(UI.scale(200), 36, 480, Utils.getprefi("combatTopPanelHeight", 400)) {
				public void changed() {
					Utils.setprefi("combatTopPanelHeight", val);
				}
			}, prev.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				combatUITopPanelHeightSlider.val = 400;
				Utils.setprefi("combatTopPanelHeight", 400);
			}), prev.pos("bl").adds(210, -20));
			prev = add(new Label("Bottom panel height (Improved UI):"), prev.pos("bl").adds(0, 10));
			prev = add(combatUIBottomPanelHeightSlider = new HSlider(UI.scale(200), 10, 480, Utils.getprefi("combatBottomPanelHeight", 100)) {
				public void changed() {
					Utils.setprefi("combatBottomPanelHeight", val);
				}
			}, prev.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				combatUIBottomPanelHeightSlider.val = 100;
				Utils.setprefi("combatBottomPanelHeight", 100);
			}), prev.pos("bl").adds(210, -20));
			prev = add(showCombatHotkeysUICheckBox = new CheckBox("Display Combat Move Hotkeys (Bottom Panel)"){
				{a = Utils.getprefb("showCombatHotkeysUI", true);}
				public void set(boolean val) {
					Utils.setprefb("showCombatHotkeysUI", val);
					a = val;
				}
			}, prev.pos("bl").adds(12, 10));
			prev = add(singleRowCombatMovesCheckBox = new CheckBox("Use single row for Combat Moves"){
				{a = Utils.getprefb("singleRowCombatMoves", false);}
				public void set(boolean val) {
					Utils.setprefb("singleRowCombatMoves", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(drawFloatingCombatDataOnCurrentTargetCheckBox = new CheckBox("Display Combat Data above Current Target"){
				{a = Utils.getprefb("drawFloatingCombatDataOnCurrentTarget", true);}
				public void set(boolean val) {
					Utils.setprefb("drawFloatingCombatDataOnCurrentTarget", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(drawFloatingCombatDataOnOthersCheckBox = new CheckBox("Display Combat Data above other Aggroed Enemies"){
				{a = Utils.getprefb("drawFloatingCombatDataOnOthers", true);}
				public void set(boolean val) {
					Utils.setprefb("drawFloatingCombatDataOnOthers", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(combatInfoBackgroundToggledCheckBox = new CheckBox("Also Draw Background for Combat Data"){
				{a = Utils.getprefb("CombatInfoBackgroundToggled", false);}
				public void set(boolean val) {
					Utils.setprefb("CombatInfoBackgroundToggled", val);
					a = val;
				}
			}, prev.pos("bl").adds(16, 2));
			prev = add(toggleGobDamageInfoCheckBox = new CheckBox("Display Damage Info:"){
				{a = Utils.getprefb("GobDamageInfoToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoToggled", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(12)));
			prev = add(new Label("> Include:"), prev.pos("bl").adds(18, 1));
			prev = add(toggleGobDamageWoundInfoCheckBox = new CheckBox("Wounds"){
				{a = Utils.getprefb("GobDamageInfoWoundsToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoWoundsToggled", val);
					a = val;
				}
			}, prev.pos("bl").adds(56, -17));
			toggleGobDamageWoundInfoCheckBox.lbl = Text.create("Wounds", PUtils.strokeImg(Text.std.render("Wounds", new Color(255, 232, 0, 255))));
			prev = add(toggleGobDamageArmorInfoCheckBox = new CheckBox("Armor"){
				{a = Utils.getprefb("GobDamageInfoArmorToggled", true);}
				public void set(boolean val) {
					Utils.setprefb("GobDamageInfoArmorToggled", val);
					a = val;
				}
			}, prev.pos("bl").adds(66, -18));
			toggleGobDamageArmorInfoCheckBox.lbl = Text.create("Armor", PUtils.strokeImg(Text.std.render("Armor", new Color(50, 255, 92, 255))));
			add(damageInfoClearButton = new Button(UI.scale(70), "Clear", false).action(() -> {
				GobDamageInfo.clearAllDamage(ui.gui);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("All Combat Damage Info has been CLEARED!", msgYellow);
				}
			}), prev.pos("bl").adds(0, -34).x(UI.scale(210)));
			prev = add(new Label("Other Combat Settings:"), prev.pos("bl").adds(0, 14).x(0));
			prev = add(toggleAutoPeaceCheckbox = new CheckBox("Autopeace Animals when combat starts"){
				{a = Utils.getprefb("autoPeaceCombat", false);}
				public void set(boolean val) {
					Utils.setprefb("autoPeaceCombat", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Autopeace Animals when combat starts is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed));
					}
				}
			}, prev.pos("bl").adds(12, 6));
			prev = add(markCurrentCombatTargetCheckBox = new CheckBox("Mark Current Target"){
				{a = Utils.getprefb("markCurrentCombatTarget", true);}
				public void set(boolean val) {
					Utils.setprefb("markCurrentCombatTarget", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(aggroedEnemiesCirclesCheckBox = new CheckBox("Put Circles under Aggroed Enemies (Players/Mobs)"){
				{a = Utils.getprefb("aggroedEnemiesCircles", true);}
				public void set(boolean val) {
					Utils.setprefb("aggroedEnemiesCircles", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(partyMembersHighlightCheckBox = new CheckBox("Highlight Party Members"){
				{a = Utils.getprefb("partyMembersHighlight", false);}
				public void set(boolean val) {
					Utils.setprefb("partyMembersHighlight", val);
					a = val;
					if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.partyHighlight != null)
						ui.gui.map.partyHighlight.update();
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(partyMembersCirclesCheckBox = new CheckBox("Put Circles under Party Members"){
				{a = Utils.getprefb("partyMembersCircles", true);}
				public void set(boolean val) {
					Utils.setprefb("partyMembersCircles", val);
					a = val;
					if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.partyCircles != null)
						ui.gui.map.partyCircles.update();
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(drawChaseVectorsCheckBox = new CheckBox("Draw Chase Vectors"){
				{a = Utils.getprefb("drawChaseVectors", true);}
				public void set(boolean val) {
					Utils.setprefb("drawChaseVectors", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 12));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(40)));
			setTooltipsForCombatSettingsStuff();
			pack();
		}
	}

	public static CheckBox toggleGobHidingCheckBox;
	public static CheckBox hideTreesCheckbox;
	public static CheckBox hideBushesCheckbox;
	public static CheckBox hideBouldersCheckbox;
	public static CheckBox hideTreeLogsCheckbox;
	public static CheckBox hideWallsCheckbox;
	public static CheckBox hideHousesCheckbox;
	public static CheckBox hideCropsCheckbox;
	public static CheckBox hideStockpilesCheckbox;
	public static ColorOptionWidget hiddenObjectsColorOptionWidget;
	public static String[] hiddenObjectsColorSetting = Utils.getprefsa("hitboxFilled" + "_colorSetting", new String[]{"0", "225", "255", "200"});

	public class NDHidingSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public NDHidingSettingsPanel(Panel back) {
			Widget prev;
			Widget prev2;
			prev = add(toggleGobHidingCheckBox = new CheckBox("Hide Objects"){
				{a = (Utils.getprefb("gobHideObjectsToggle", false));}
				public void set(boolean val) {
					Utils.setprefb("gobHideObjectsToggle", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
						ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, 0, 10);

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 40))), prev.pos("bl").adds(14, 16));
			Widget cont = scroll.cont;
			addbtn(cont, "Toggle object hiding hotkey:", GameUI.kb_toggleHidingBoxes, 0);

			prev = add(hiddenObjectsColorOptionWidget = new ColorOptionWidget("Hidden Objects Box Color:", "hitboxFilled", 150, Integer.parseInt(hiddenObjectsColorSetting[0]), Integer.parseInt(hiddenObjectsColorSetting[1]), Integer.parseInt(hiddenObjectsColorSetting[2]), Integer.parseInt(hiddenObjectsColorSetting[3]), (Color col) -> {
				HidingBoxFilled.SOLID = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
				hiddenObjectsColorOptionWidget2.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget2.currentColor = col); // ND: set the color for the other widget as well
			}){}, scroll.pos("bl").adds(1, -2));

			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("hitboxFilled" + "_colorSetting", new String[]{"0", "225", "255", "200"});
				hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget.currentColor = new Color(0, 225, 255, 200));
				HidingBoxFilled.SOLID = Pipe.Op.compose(new BaseColor(hiddenObjectsColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(new Color(hiddenObjectsColorOptionWidget.currentColor.getRed(), hiddenObjectsColorOptionWidget.currentColor.getGreen(), hiddenObjectsColorOptionWidget.currentColor.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
				hiddenObjectsColorOptionWidget2.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget2.currentColor = new Color(0, 225, 255, 200)); // ND: set the color for the other widget as well
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = RichText.render("Reset to default color", UI.scale(300));

			prev = add(new Label("Objects that will be hidden:"), prev.pos("bl").adds(0, 20).x(0));

			prev2 = add(hideTreesCheckbox = new CheckBox("Trees"){
				{a = Utils.getprefb("hideTrees", true);}
				public void set(boolean val) {
					Utils.setprefb("hideTrees", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(hideBushesCheckbox = new CheckBox("Bushes"){
				{a = Utils.getprefb("hideBushes", true);}
				public void set(boolean val) {
					Utils.setprefb("hideBushes", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev2.pos("bl").adds(0, 2));

			prev = add(hideBouldersCheckbox = new CheckBox("Boulders"){
				{a = Utils.getprefb("hideBoulders", true);}
				public void set(boolean val) {
					Utils.setprefb("hideBoulders", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideTreeLogsCheckbox = new CheckBox("Tree Logs"){
				{a = Utils.getprefb("hideTreeLogs", true);}
				public void set(boolean val) {
					Utils.setprefb("hideTreeLogs", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideWallsCheckbox = new CheckBox("Palisades and Brick Walls"){
				{a = Utils.getprefb("hideWalls", false);}
				public void set(boolean val) {
					Utils.setprefb("hideWalls", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev2.pos("ur").adds(90, 0));

			prev = add(hideHousesCheckbox = new CheckBox("Houses"){
				{a = Utils.getprefb("hideHouses", false);}
				public void set(boolean val) {
					Utils.setprefb("hideHouses", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideStockpilesCheckbox = new CheckBox("Stockpiles"){
				{a = Utils.getprefb("hideStockpiles", false);}
				public void set(boolean val) {
					Utils.setprefb("hideStockpiles", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideCropsCheckbox = new CheckBox("Crops"){
				{a = Utils.getprefb("hideCrops", false);}
				public void set(boolean val) {
					Utils.setprefb("hideCrops", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
						ui.sess.glob.oc.gobAction(Gob::growthInfoUpdated);
						ui.gui.map.updatePlobDrawable();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(57)));
			setTooltipsForHidingSettingsStuff();
			pack();
		}
	}

	//DropSettings
	public static CheckBox toggleDropItemsCheckBox;
	public static CheckBox dropStoneCheckbox;
	public static TextEntry dropStoneQualityTextEntry;
	public static CheckBox dropOreCheckbox;
	public static TextEntry dropOreQualityTextEntry;
	public static CheckBox dropPreciousOreCheckbox;
	public static TextEntry dropPreciousOreQualityTextEntry;
	public static CheckBox dropMinedCuriosCheckbox;
	public static TextEntry dropMinedCuriosQualityTextEntry;
	public static CheckBox dropQuarryartzCheckbox;
	public static TextEntry dropQuarryartzQualityTextEntry;

	public class NDAutoDropSettingsPanel extends Panel {

		public NDAutoDropSettingsPanel(Panel back) {
			Widget prev;
			prev = add(toggleDropItemsCheckBox = new CheckBox("Enable Auto-Drop Mined Items"){
				{a = (Utils.getprefb("dropItemsToggle", false));}
				public void set(boolean val) {
					Utils.setprefb("dropItemsToggle", val);
					a = val;
				}
			}, 0, 10);

			prev = add(new Label("Objects that will be dropped:"), prev.pos("bl").adds(0, 20).x(0));

			prev = add(dropStoneCheckbox = new CheckBox("Mined Stone"){
				{a = Utils.getprefb("dropStone", false);}
				public void set(boolean val) {
					Utils.setprefb("dropStone", val);
					a = val;
				}
			}, prev.pos("bl").adds(12, 10));
			add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
			add(dropStoneQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("dropStoneQuality", "30")){
				protected void changed() {
					Utils.setpref("dropStoneQuality", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
			prev = add(dropOreCheckbox = new CheckBox("Mined Ore"){
				{a = Utils.getprefb("dropOre", false);}
				public void set(boolean val) {
					Utils.setprefb("dropOre", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));
			add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
			add(dropOreQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("dropOreQuality", "30")){
				protected void changed() {
					Utils.setpref("dropOreQuality", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2).x(UI.scale(156)));

			prev = add(dropPreciousOreCheckbox = new CheckBox("Mined Precious Ore"){
				{a = Utils.getprefb("dropPreciousOre", false);}
				public void set(boolean val) {
					Utils.setprefb("dropPreciousOre", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));
			add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
			add(dropPreciousOreQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("dropPreciousOreQuality", "30")){
				protected void changed() {
					Utils.setpref("dropPreciousOreQuality", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2).x(UI.scale(156)));

			prev = add(dropMinedCuriosCheckbox = new CheckBox("Mined Curios"){
				{a = Utils.getprefb("dropMinedCurios", false);}
				public void set(boolean val) {
					Utils.setprefb("dropMinedCurios", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));
			add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
			add(dropMinedCuriosQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("dropMinedCuriosQuality", "30")){
				protected void changed() {
					Utils.setpref("dropMinedCuriosQuality", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2).x(UI.scale(156)));

			prev = add(dropQuarryartzCheckbox = new CheckBox("Quarryartz"){
				{a = Utils.getprefb("dropQuarryartz", false);}
				public void set(boolean val) {
					Utils.setprefb("dropQuarryartz", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));
			add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
			add(dropQuarryartzQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("dropQuarryartzQuality", "30")){
				protected void changed() {
					Utils.setpref("dropQuarryartzQuality", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
		}
	}

	Button CustomAlarmManagerButton;
	public static CheckBox whitePlayerAlarmEnabledCheckbox;
	public static TextEntry whitePlayerAlarmFilename;
	public static HSlider whitePlayerAlarmVolumeSlider;
	public static CheckBox whiteVillageOrRealmPlayerAlarmEnabledCheckbox;
	public static TextEntry whiteVillageOrRealmPlayerAlarmFilename;
	public static HSlider whiteVillageOrRealmPlayerAlarmVolumeSlider;
	public static CheckBox greenPlayerAlarmEnabledCheckbox;
	public static TextEntry greenPlayerAlarmFilename;
	public static HSlider greenPlayerAlarmVolumeSlider;
	public static CheckBox redPlayerAlarmEnabledCheckbox;
	public static TextEntry redPlayerAlarmFilename;
	public static HSlider redPlayerAlarmVolumeSlider;
	public static CheckBox bluePlayerAlarmEnabledCheckbox;
	public static TextEntry bluePlayerAlarmFilename;
	public static HSlider bluePlayerAlarmVolumeSlider;
	public static CheckBox tealPlayerAlarmEnabledCheckbox;
	public static TextEntry tealPlayerAlarmFilename;
	public static HSlider tealPlayerAlarmVolumeSlider;
	public static CheckBox yellowPlayerAlarmEnabledCheckbox;
	public static TextEntry yellowPlayerAlarmFilename;
	public static HSlider yellowPlayerAlarmVolumeSlider;
	public static CheckBox purplePlayerAlarmEnabledCheckbox;
	public static TextEntry purplePlayerAlarmFilename;
	public static HSlider purplePlayerAlarmVolumeSlider;
	public static CheckBox orangePlayerAlarmEnabledCheckbox;
	public static TextEntry orangePlayerAlarmFilename;
	public static HSlider orangePlayerAlarmVolumeSlider;
	public static CheckBox combatStartSoundEnabledCheckbox;
	public static TextEntry combatStartSoundFilename;
	public static HSlider combatStartSoundVolumeSlider;
	public static CheckBox cleaveSoundEnabledCheckbox;
	public static TextEntry cleaveSoundFilename;
	public static HSlider cleaveSoundVolumeSlider;
	public static CheckBox opkSoundEnabledCheckbox;
	public static TextEntry opkSoundFilename;
	public static HSlider opkSoundVolumeSlider;
	public static CheckBox ponyPowerSoundEnabledCheckbox;
	public static TextEntry ponyPowerSoundFilename;
	public static HSlider ponyPowerSoundVolumeSlider;
	public static CheckBox lowEnergySoundEnabledCheckbox;
	public static TextEntry lowEnergySoundFilename;
	public static HSlider lowEnergySoundVolumeSlider;

	public class NDAlarmsAndSoundsSettingsPanel extends Panel {

		public NDAlarmsAndSoundsSettingsPanel(Panel back) {
			Widget prev;

			add(new Label("You can add your own alarm sound files in the \"Alarms\" folder.", new Text.Foundry(Text.sans, 12)), 0, 0);
			add(new Label("(The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), UI.scale(0, 16));
			prev = add(new Label("Enabled Player Alarms:"), UI.scale(0, 40));
			prev = add(new Label("Sound File"), prev.pos("ur").add(70, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));
			prev = add(whitePlayerAlarmEnabledCheckbox = new CheckBox("White OR Unknown:"){
				{a = Utils.getprefb("whitePlayerAlarmEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("whitePlayerAlarmEnabled", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 10).x(0));
			prev = add(whitePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("whitePlayerAlarmFilename", "ND_YoHeadsUp")){
				protected void changed() {
					Utils.setpref("whitePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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

			prev = add(whiteVillageOrRealmPlayerAlarmEnabledCheckbox = new CheckBox("Village/Realm Member:"){
				{a = Utils.getprefb("whiteVillageOrRealmPlayerAlarmEnabled", true);}
				public void set(boolean val) {
					Utils.setprefb("whiteVillageOrRealmPlayerAlarmEnabled", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(whiteVillageOrRealmPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("whiteVillageOrRealmPlayerAlarmFilename", "ND_HelloFriend")){
				protected void changed() {
					Utils.setpref("whiteVillageOrRealmPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			greenPlayerAlarmEnabledCheckbox.lbl = Text.create("Green:", PUtils.strokeImg(Text.std.render("Green:", BuddyWnd.gc[1])));
			prev = add(greenPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("greenPlayerAlarmFilename", "ND_FlyingTheFriendlySkies")){
				protected void changed() {
					Utils.setpref("greenPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			redPlayerAlarmEnabledCheckbox.lbl = Text.create("Red:", PUtils.strokeImg(Text.std.render("Red:", BuddyWnd.gc[2])));
			prev = add(redPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("redPlayerAlarmFilename", "ND_EnemySighted")){
				protected void changed() {
					Utils.setpref("redPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			bluePlayerAlarmEnabledCheckbox.lbl = Text.create("Blue:", PUtils.strokeImg(Text.std.render("Blue:", BuddyWnd.gc[3])));
			prev = add(bluePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("bluePlayerAlarmFilename", "ND_YeahLetsHustle")){
				protected void changed() {
					Utils.setpref("bluePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			tealPlayerAlarmEnabledCheckbox.lbl = Text.create("Teal:", PUtils.strokeImg(Text.std.render("Teal:", BuddyWnd.gc[4])));
			prev = add(tealPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("tealPlayerAlarmFilename", "ND_YeahLetsHustle")){
				protected void changed() {
					Utils.setpref("tealPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			yellowPlayerAlarmEnabledCheckbox.lbl = Text.create("Yellow:", PUtils.strokeImg(Text.std.render("Yellow:", BuddyWnd.gc[5])));
			prev = add(yellowPlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("yellowPlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("yellowPlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			purplePlayerAlarmEnabledCheckbox.lbl = Text.create("Purple:", PUtils.strokeImg(Text.std.render("Purple:", BuddyWnd.gc[6])));
			prev = add(purplePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("purplePlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("purplePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			orangePlayerAlarmEnabledCheckbox.lbl = Text.create("Orange:", PUtils.strokeImg(Text.std.render("Orange:", BuddyWnd.gc[7])));
			prev = add(orangePlayerAlarmFilename = new TextEntry(UI.scale(140), Utils.getpref("orangePlayerAlarmFilename", "")){
				protected void changed() {
					Utils.setpref("orangePlayerAlarmFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));
			prev = add(combatStartSoundEnabledCheckbox = new CheckBox("Combat Started Alert:"){
				{a = Utils.getprefb("combatStartSoundEnabled", false);}
				public void set(boolean val) {
					Utils.setprefb("combatStartSoundEnabled", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 10).x(0));
			prev = add(combatStartSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("combatStartSoundFilename", "ND_HitAndRun")){
				protected void changed() {
					Utils.setpref("combatStartSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(cleaveSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("cleaveSoundFilename", "ND_Cleave")){
				protected void changed() {
					Utils.setpref("cleaveSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(opkSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("opkSoundFilename", "ND_Opk")){
				protected void changed() {
					Utils.setpref("opkSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(ponyPowerSoundFilename = new TextEntry(UI.scale(140), Utils.getpref("ponyPowerSoundFilename", "ND_HorseEnergy")){
				protected void changed() {
					Utils.setpref("ponyPowerSoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
					a = val;
				}
			}, prev.pos("bl").adds(0, 6).x(0));
			prev = add(lowEnergySoundFilename = new TextEntry(UI.scale(140), Utils.getpref("lowEnergySoundFilename", "ND_NotEnoughEnergy")){
				protected void changed() {
					Utils.setpref("lowEnergySoundFilename", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(143)));
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
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
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
			}),prev.pos("bl").adds(0, 18).x(UI.scale(51)));


			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(UI.scale(131)));
			setTooltipsForAlarmSettingsStuff();
			pack();
		}
	}


	public static ColorOptionWidget hiddenObjectsColorOptionWidget2;
	public static ColorOptionWidget collisionBoxesColorOptionWidget;
	public static String[] collisionBoxesColorSetting = Utils.getprefsa("collisionBoxes" + "_colorSetting", new String[]{"255", "255", "255", "235"});
	public static ColorOptionWidget cliffsHighlightColorOptionWidget;
	public static Pipe.Op cliffMat = null;
	public static String[] cliffsHighlightColorSetting = Utils.getprefsa("cliffsHighlight" + "_colorSetting", new String[]{"255", "0", "0", "170"});
	public static ColorOptionWidget fullContainerOrFinishedWorkstationColorOptionWidget;
	public static String[] fullContainerOrFinishedWorkstationColorSetting = Utils.getprefsa("fullContainerOrFinishedWorkstation" + "_colorSetting", new String[]{"170", "0", "0", "170"});
	public static ColorOptionWidget somewhatFilledContainerOrInProgressWorkstationColorOptionWidget;
	public static String[] somewhatFilledContainerOrInProgressWorkstationColorSetting = Utils.getprefsa("somewhatFilledContainerOrInProgressWorkstation" + "_colorSetting", new String[]{"194", "155", "2", "140"});
	public static ColorOptionWidget emptyContainerOrPreparedWorkstationColorOptionWidget;
	public static String[] emptyContainerOrPreparedWorkstationColorSetting = Utils.getprefsa("emptyContainerOrPreparedWorkstation" + "_colorSetting", new String[]{"0", "120", "0", "180"});
	public static ColorOptionWidget unpreparedWorkstationColorOptionWidget;
	public static String[] unpreparedWorkstationColorSetting = Utils.getprefsa("unpreparedWorkstation" + "_colorSetting", new String[]{"20", "20", "20", "170"});
	public static ColorOptionWidget closedGateColorOptionWidget;
	public static String[] closedGateColorSetting = Utils.getprefsa("closedGate" + "_colorSetting", new String[]{"218", "0", "0", "100"});
	public static ColorOptionWidget openVisitorGateNoCombatColorOptionWidget;
	public static String[] openVisitorGateNoCombatColorSetting = Utils.getprefsa("openVisitorGateNoCombat" + "_colorSetting", new String[]{"255", "233", "0", "100"});
	public static ColorOptionWidget openVisitorGateInCombatColorOptionWidget;
	public static String[] openVisitorGateInCombatColorSetting = Utils.getprefsa("openVisitorGateInCombat" + "_colorSetting", new String[]{"255", "150", "0", "100"});
	public static ColorOptionWidget passableGateCombatColorOptionWidget;
	public static String[] passableGateCombatColorSetting = Utils.getprefsa("passableGate" + "_colorSetting", new String[]{"0", "217", "30", "100"});
	public static ColorOptionWidget genericCritterAuraColorOptionWidget;
	public static String[] genericCritterAuraColorSetting = Utils.getprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
	public static ColorOptionWidget rabbitAuraColorOptionWidget;
	public static String[] rabbitAuraColorSetting = Utils.getprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
	public static ColorOptionWidget speedbuffAuraColorOptionWidget;
	public static String[] speedbuffAuraColorSetting = Utils.getprefsa("speedbuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
	public static ColorOptionWidget objectPingColorOptionWidget;
	public static String[] objectPingColorSetting = Utils.getprefsa("objectPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
	public static ColorOptionWidget partyObjectPingColorOptionWidget;
	public static String[] partyObjectPingColorSetting = Utils.getprefsa("partyObjectPing" + "_colorSetting", new String[]{"243", "0", "0", "255"});
	public static ColorOptionWidget permanentHighlightColorOptionWidget;
	public static String[] permanentHighlightColorSetting = Utils.getprefsa("permanentHighlight" + "_colorSetting", new String[]{"116", "0", "178", "200"});
	public static ColorOptionWidget partyMemberColorOptionWidget;
	public static String[] partyMemberColorSetting = Utils.getprefsa("partyMember" + "_colorSetting", new String[]{"0", "160", "0", "164"});
	public static ColorOptionWidget partyLeaderColorOptionWidget;
	public static String[] partyLeaderColorSetting = Utils.getprefsa("partyLeader" + "_colorSetting", new String[]{"0", "74", "208", "164"});
	public static ColorOptionWidget myselfColorOptionWidget;
	public static String[] myselfLeaderColorSetting = Utils.getprefsa("myself" + "_colorSetting", new String[]{"255", "255", "255", "128"});
	public static ColorOptionWidget aggroedEnemiesColorOptionWidget;
	public static String[] aggroedEnemiesColorSetting = Utils.getprefsa("aggroedEnemies" + "_colorSetting", new String[]{"255", "0", "0", "140"});
	public static boolean refreshCurrentTargetSpriteColor = false;
	public static ColorOptionWidget myselfChaseVectorColorOptionWidget;
	public static String[] myselfChaseVectorColorSetting = Utils.getprefsa("myselfChaseVector" + "_colorSetting", new String[]{"255", "255", "255", "220"});
	public static ColorOptionWidget friendChaseVectorColorOptionWidget;
	public static String[] friendChaseVectorColorSetting = Utils.getprefsa("friendChaseVector" + "_colorSetting", new String[]{"47", "191", "7", "230"});
	public static ColorOptionWidget foeChaseVectorColorOptionWidget;
	public static String[] foeChaseVectorColorSetting = Utils.getprefsa("foeChaseVector" + "_colorSetting", new String[]{"255", "0", "0", "230"});
	public static ColorOptionWidget unknownChaseVectorColorOptionWidget;
	public static String[] unknownChaseVectorColorSetting = Utils.getprefsa("unknownChaseVector" + "_colorSetting", new String[]{"255", "199", "0", "230"});


	public class NDColorSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public NDColorSettingsPanel(Panel back) {
			Widget topLabel;
			Widget leftColumn;
			Widget rightColumn;
			topLabel = add(new Label("These settings exist thanks to my colorblind friend, who asked me if I could add this. You can change most custom feature colors here, if you don't like the defaults."), 0, 0);
			leftColumn = add(hiddenObjectsColorOptionWidget2 = new ColorOptionWidget("Hidden Objects Box:", "hitboxFilled", 246, Integer.parseInt(hiddenObjectsColorSetting[0]), Integer.parseInt(hiddenObjectsColorSetting[1]), Integer.parseInt(hiddenObjectsColorSetting[2]), Integer.parseInt(hiddenObjectsColorSetting[3]), (Color col) -> {
				HidingBoxFilled.SOLID = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
				hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget.currentColor = col); // ND: set the color for the other widget as well
			}){}, topLabel.pos("bl").adds(0, 16).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("hitboxFilled" + "_colorSetting", new String[]{"0", "225", "255", "200"});
				hiddenObjectsColorOptionWidget2.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget2.currentColor = new Color(0, 225, 255, 200));
				HidingBoxFilled.SOLID = Pipe.Op.compose(new BaseColor(hiddenObjectsColorOptionWidget2.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(new Color(hiddenObjectsColorOptionWidget2.currentColor.getRed(), hiddenObjectsColorOptionWidget2.currentColor.getGreen(), hiddenObjectsColorOptionWidget2.currentColor.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
				hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget.currentColor = new Color(0, 225, 255, 200));// ND: set the color for the other widget as well
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			leftColumn = add(collisionBoxesColorOptionWidget = new ColorOptionWidget("Collision Boxes:", "collisionBoxes", 246, Integer.parseInt(collisionBoxesColorSetting[0]), Integer.parseInt(collisionBoxesColorSetting[1]), Integer.parseInt(collisionBoxesColorSetting[2]), Integer.parseInt(collisionBoxesColorSetting[3]), (Color col) -> {
				CollisionBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(col), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("collisionBoxes" + "_colorSetting", new String[]{"255", "255", "255", "235"});
				collisionBoxesColorOptionWidget.cb.colorChooser.setColor(collisionBoxesColorOptionWidget.currentColor = new Color(255, 255, 255, 235));
				CollisionBox.SOLID_TOP = Pipe.Op.compose(new BaseColor(OptWnd.collisionBoxesColorOptionWidget.currentColor), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated);
					ui.gui.map.updatePlobDrawable();
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			leftColumn = add(cliffsHighlightColorOptionWidget = new ColorOptionWidget("Cliffs Highlight (Color Overlay):", "cliffsHighlight", 246, Integer.parseInt(cliffsHighlightColorSetting[0]), Integer.parseInt(cliffsHighlightColorSetting[1]), Integer.parseInt(cliffsHighlightColorSetting[2]), Integer.parseInt(cliffsHighlightColorSetting[3]), (Color col) -> {
				cliffMat = new MixColor(cliffsHighlightColorOptionWidget.currentColor);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("cliffsHighlight" + "_colorSetting", new String[]{"255", "0", "0", "170"});
				cliffsHighlightColorOptionWidget.cb.colorChooser.setColor(cliffsHighlightColorOptionWidget.currentColor = new Color(255, 0, 0, 170));
				cliffMat = new MixColor(cliffsHighlightColorOptionWidget.currentColor);
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			cliffMat = new MixColor(OptWnd.cliffsHighlightColorOptionWidget.currentColor);

			leftColumn = add(new Label("Containers & Workstations"), leftColumn.pos("bl").adds(0, 12).x(UI.scale(122)));
			leftColumn = add(fullContainerOrFinishedWorkstationColorOptionWidget = new ColorOptionWidget("Full Container / Finished Workstation:", "fullContainerOrFinishedWorkstation", 246,
					Integer.parseInt(fullContainerOrFinishedWorkstationColorSetting[0]), Integer.parseInt(fullContainerOrFinishedWorkstationColorSetting[1]),
					Integer.parseInt(fullContainerOrFinishedWorkstationColorSetting[2]), Integer.parseInt(fullContainerOrFinishedWorkstationColorSetting[3]), (Color col) -> {

				GobStateHighlight.red = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}){}, leftColumn.pos("bl").adds(0, 6).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("fullContainerOrFinishedWorkstation" + "_colorSetting", new String[]{"170", "0", "0", "170"});
				fullContainerOrFinishedWorkstationColorOptionWidget.cb.colorChooser.setColor(fullContainerOrFinishedWorkstationColorOptionWidget.currentColor = new Color(170, 0, 0, 170));
				GobStateHighlight.red = new MixColor(170, 0, 0, 170);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			leftColumn = add(somewhatFilledContainerOrInProgressWorkstationColorOptionWidget = new ColorOptionWidget("Partly Filled Container / In Progress Workstation:", "somewhatFilledContainerOrInProgressWorkstation", 246,
					Integer.parseInt(somewhatFilledContainerOrInProgressWorkstationColorSetting[0]), Integer.parseInt(somewhatFilledContainerOrInProgressWorkstationColorSetting[1]),
					Integer.parseInt(somewhatFilledContainerOrInProgressWorkstationColorSetting[2]), Integer.parseInt(somewhatFilledContainerOrInProgressWorkstationColorSetting[3]), (Color col) -> {

				GobStateHighlight.yellow = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("somewhatFilledContainerOrInProgressWorkstation" + "_colorSetting", new String[]{"194", "155", "2", "140"});
				somewhatFilledContainerOrInProgressWorkstationColorOptionWidget.cb.colorChooser.setColor(somewhatFilledContainerOrInProgressWorkstationColorOptionWidget.currentColor = new Color(194, 155, 2, 140));
				GobStateHighlight.yellow = new MixColor(194, 155, 2, 140);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			leftColumn = add(emptyContainerOrPreparedWorkstationColorOptionWidget = new ColorOptionWidget("Empty Container / Prepared Workstation:", "emptyContainerOrPreparedWorkstation", 246,
					Integer.parseInt(emptyContainerOrPreparedWorkstationColorSetting[0]), Integer.parseInt(emptyContainerOrPreparedWorkstationColorSetting[1]),
					Integer.parseInt(emptyContainerOrPreparedWorkstationColorSetting[2]), Integer.parseInt(emptyContainerOrPreparedWorkstationColorSetting[3]), (Color col) -> {

				GobStateHighlight.green = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("emptyContainerOrPreparedWorkstation" + "_colorSetting", new String[]{"0", "120", "0", "180"});
				emptyContainerOrPreparedWorkstationColorOptionWidget.cb.colorChooser.setColor(emptyContainerOrPreparedWorkstationColorOptionWidget.currentColor = new Color(0, 120, 0, 180));
				GobStateHighlight.green = new MixColor(0, 120, 0, 180);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			leftColumn = add(unpreparedWorkstationColorOptionWidget = new ColorOptionWidget("Unprepared Workstation:", "unpreparedWorkstation", 246,
					Integer.parseInt(unpreparedWorkstationColorSetting[0]), Integer.parseInt(unpreparedWorkstationColorSetting[1]),
					Integer.parseInt(unpreparedWorkstationColorSetting[2]), Integer.parseInt(unpreparedWorkstationColorSetting[3]), (Color col) -> {

				GobStateHighlight.gray = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("unpreparedWorkstation" + "_colorSetting", new String[]{"20", "20", "20", "170"});
				unpreparedWorkstationColorOptionWidget.cb.colorChooser.setColor(unpreparedWorkstationColorOptionWidget.currentColor = new Color(20, 20, 20, 170));
				GobStateHighlight.gray = new MixColor(20, 20, 20, 170);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerHighlight);
					ui.sess.glob.oc.gobAction(Gob::settingUpdateWorkstationStage);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			leftColumn = add(new Label("Gate Combat Passability"), leftColumn.pos("bl").adds(0, 12).x(UI.scale(126)));
			leftColumn = add(closedGateColorOptionWidget = new ColorOptionWidget("Closed Gate (Unpassable):", "closedGate", 246,
					Integer.parseInt(closedGateColorSetting[0]), Integer.parseInt(closedGateColorSetting[1]),
					Integer.parseInt(closedGateColorSetting[2]), Integer.parseInt(closedGateColorSetting[3]), (Color col) -> {
				HidingBox.CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(col), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.CLOSEDGATE = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(),
						col.getBlue(), collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}){}, leftColumn.pos("bl").adds(0, 6).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("closedGate" + "_colorSetting", new String[]{"218", "0", "0", "100"});
				closedGateColorOptionWidget.cb.colorChooser.setColor(closedGateColorOptionWidget.currentColor = new Color(218, 0, 0, 100));
				HidingBox.CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(closedGateColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.CLOSEDGATE = Pipe.Op.compose(new BaseColor(closedGateColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.CLOSEDGATE_TOP = Pipe.Op.compose(new BaseColor(new Color(218, 0,0, collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			leftColumn = add(openVisitorGateNoCombatColorOptionWidget = new ColorOptionWidget("Open Visitor Gate (Out of Combat):", "openVisitorGateNoCombat", 246,
					Integer.parseInt(openVisitorGateNoCombatColorSetting[0]), Integer.parseInt(openVisitorGateNoCombatColorSetting[1]),
					Integer.parseInt(openVisitorGateNoCombatColorSetting[2]), Integer.parseInt(openVisitorGateNoCombatColorSetting[3]), (Color col) -> {
				HidingBox.OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(col), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.OPENVISITORGATE_NoCombat = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(),
						col.getBlue(), collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("openVisitorGateNoCombat" + "_colorSetting", new String[]{"255", "233", "0", "100"});
				openVisitorGateNoCombatColorOptionWidget.cb.colorChooser.setColor(openVisitorGateNoCombatColorOptionWidget.currentColor = new Color(255, 233, 0, 100));
				HidingBox.OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(openVisitorGateNoCombatColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.OPENVISITORGATE_NoCombat = Pipe.Op.compose(new BaseColor(openVisitorGateNoCombatColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.OPENVISITORGATE_TOP_NoCombat = Pipe.Op.compose(new BaseColor(new Color(255, 233,0, collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			leftColumn = add(openVisitorGateInCombatColorOptionWidget = new ColorOptionWidget("Open Visitor Gate (In Combat):", "openVisitorGateInCombat", 246,
					Integer.parseInt(openVisitorGateInCombatColorSetting[0]), Integer.parseInt(openVisitorGateInCombatColorSetting[1]),
					Integer.parseInt(openVisitorGateInCombatColorSetting[2]), Integer.parseInt(openVisitorGateInCombatColorSetting[3]), (Color col) -> {
				HidingBox.OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(col), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.OPENVISITORGATE_InCombat = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(),
						col.getBlue(), collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("openVisitorGateInCombat" + "_colorSetting", new String[]{"255", "150", "0", "100"});
				openVisitorGateInCombatColorOptionWidget.cb.colorChooser.setColor(openVisitorGateInCombatColorOptionWidget.currentColor = new Color(255, 150, 0, 100));
				HidingBox.OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(openVisitorGateInCombatColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.OPENVISITORGATE_InCombat = Pipe.Op.compose(new BaseColor(openVisitorGateInCombatColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.OPENVISITORGATE_TOP_InCombat = Pipe.Op.compose(new BaseColor(new Color(255, 150,0, collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			leftColumn = add(passableGateCombatColorOptionWidget = new ColorOptionWidget("Open Non-Visitor Gate / Other Passable Objects:", "passableGate", 246,
					Integer.parseInt(passableGateCombatColorSetting[0]), Integer.parseInt(passableGateCombatColorSetting[1]),
					Integer.parseInt(passableGateCombatColorSetting[2]), Integer.parseInt(passableGateCombatColorSetting[3]), (Color col) -> {
				HidingBox.PASSABLE_TOP = Pipe.Op.compose(new BaseColor(col), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.PASSABLE = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.PASSABLE_TOP = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(),
						col.getBlue(), collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("passableGate" + "_colorSetting", new String[]{"0", "217", "30", "100"});
				passableGateCombatColorOptionWidget.cb.colorChooser.setColor(passableGateCombatColorOptionWidget.currentColor = new Color(0, 217, 30, 100));
				HidingBox.PASSABLE_TOP = Pipe.Op.compose(new BaseColor(passableGateCombatColorOptionWidget.currentColor), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				HidingBoxFilled.PASSABLE = Pipe.Op.compose(new BaseColor(passableGateCombatColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				CollisionBox.PASSABLE_TOP = Pipe.Op.compose(new BaseColor(new Color(0, 217,30, collisionBoxesColorOptionWidget.currentColor.getAlpha())), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::hidingBoxUpdated);
					ui.sess.glob.oc.gobAction(Gob::collisionBoxUpdated); // ND: Gotta update collision boxes too, cause the gate colors depend on this setting
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));



			leftColumn = add(new Label("Aura Circles"), leftColumn.pos("bl").adds(0, 12).x(UI.scale(126)));
			leftColumn = add(genericCritterAuraColorOptionWidget = new ColorOptionWidget("Generic Critter Aura:", "genericCritterAura", 246,
					Integer.parseInt(genericCritterAuraColorSetting[0]), Integer.parseInt(genericCritterAuraColorSetting[1]),
					Integer.parseInt(genericCritterAuraColorSetting[2]), Integer.parseInt(genericCritterAuraColorSetting[3]), (Color col) -> {
				AuraCircleSprite.genericCritterAuraColor = genericCritterAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
				genericCritterAuraColorOptionWidget.cb.colorChooser.setColor(genericCritterAuraColorOptionWidget.currentColor = new Color(193, 0, 255, 140));
				AuraCircleSprite.genericCritterAuraColor = genericCritterAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));


			leftColumn = add(rabbitAuraColorOptionWidget = new ColorOptionWidget("Rabbit/Bunny Aura:", "rabbitAura", 246,
					Integer.parseInt(rabbitAuraColorSetting[0]), Integer.parseInt(rabbitAuraColorSetting[1]),
					Integer.parseInt(rabbitAuraColorSetting[2]), Integer.parseInt(rabbitAuraColorSetting[3]), (Color col) -> {
				AuraCircleSprite.rabbitAuraColor = rabbitAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
				rabbitAuraColorOptionWidget.cb.colorChooser.setColor(rabbitAuraColorOptionWidget.currentColor = new Color(88, 255, 0, 140));
				AuraCircleSprite.rabbitAuraColor = rabbitAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleCritterAuras);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));


			leftColumn = add(speedbuffAuraColorOptionWidget = new ColorOptionWidget("Speed Buff Aura:", "speedbuffAura", 246,
					Integer.parseInt(speedbuffAuraColorSetting[0]), Integer.parseInt(speedbuffAuraColorSetting[1]),
					Integer.parseInt(speedbuffAuraColorSetting[2]), Integer.parseInt(speedbuffAuraColorSetting[3]), (Color col) -> {
				AuraCircleSprite.speedbuffAuraColor = speedbuffAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleSpeedBuffAuras);
				}
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("speedbuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
				speedbuffAuraColorOptionWidget.cb.colorChooser.setColor(speedbuffAuraColorOptionWidget.currentColor = new Color(255, 255, 255, 140));
				AuraCircleSprite.speedbuffAuraColor = speedbuffAuraColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::toggleSpeedBuffAuras);
				}
			}), leftColumn.pos("ur").adds(30, 0));
			leftColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(objectPingColorOptionWidget = new ColorOptionWidget("Object Ping (Alt + Left Click, Area Chat):", "objectPing", 246,
					Integer.parseInt(objectPingColorSetting[0]), Integer.parseInt(objectPingColorSetting[1]), Integer.parseInt(objectPingColorSetting[2]), Integer.parseInt(objectPingColorSetting[3]), (Color col) -> {
			}){}, topLabel.pos("bl").adds(0, 16).x(UI.scale(400)));

			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("objectPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
				objectPingColorOptionWidget.cb.colorChooser.setColor(objectPingColorOptionWidget.currentColor = new Color(255, 183, 0, 255));
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(partyObjectPingColorOptionWidget = new ColorOptionWidget("Party Object Ping (Alt + Right Click, Party Chat):", "partyObjectPing", 246,
					Integer.parseInt(partyObjectPingColorSetting[0]), Integer.parseInt(partyObjectPingColorSetting[1]), Integer.parseInt(partyObjectPingColorSetting[2]), Integer.parseInt(partyObjectPingColorSetting[3]), (Color col) -> {
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));

			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("partyObjectPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
				partyObjectPingColorOptionWidget.cb.colorChooser.setColor(partyObjectPingColorOptionWidget.currentColor = new Color(243, 0, 0, 255));
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(permanentHighlightColorOptionWidget = new ColorOptionWidget("Permanent Object Highlight (Alt + Middle Click):", "permanentHighlight", 246,
					Integer.parseInt(permanentHighlightColorSetting[0]), Integer.parseInt(permanentHighlightColorSetting[1]), Integer.parseInt(permanentHighlightColorSetting[2]), Integer.parseInt(permanentHighlightColorSetting[3]), (Color col) -> {
				GobPermanentHighlight.purple = new MixColor(col);
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::setHighlightedObjects);
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));

			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("permanentHighlight" + "_colorSetting", new String[]{"116", "0", "178", "200"});
				permanentHighlightColorOptionWidget.cb.colorChooser.setColor(permanentHighlightColorOptionWidget.currentColor = new Color(116, 0, 178, 200));
				GobPermanentHighlight.purple = new MixColor(permanentHighlightColorOptionWidget.currentColor);
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::setHighlightedObjects);
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(new Label("Combat Friends & Foes"), rightColumn.pos("bl").adds(0, 12).x(UI.scale(520)));
			rightColumn = add(partyMemberColorOptionWidget = new ColorOptionWidget("Party Member Circle/Highlight:", "partyMember", 246,
					Integer.parseInt(partyMemberColorSetting[0]), Integer.parseInt(partyMemberColorSetting[1]), Integer.parseInt(partyMemberColorSetting[2]), Integer.parseInt(partyMemberColorSetting[3]), (Color col) -> {
				PartyHighlight.MEMBER_OL_COLOR = col;
				PartyCircles.MEMBER_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("partyMember" + "_colorSetting", new String[]{"0", "160", "0", "164"});
				partyMemberColorOptionWidget.cb.colorChooser.setColor(partyMemberColorOptionWidget.currentColor = new Color(0, 160, 0, 164));
				PartyHighlight.MEMBER_OL_COLOR = partyMemberColorOptionWidget.currentColor;
				PartyCircles.MEMBER_OL_COLOR = partyMemberColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(partyLeaderColorOptionWidget = new ColorOptionWidget("Party Leader Circle/Highlight:", "partyLeader", 246,
					Integer.parseInt(partyLeaderColorSetting[0]), Integer.parseInt(partyLeaderColorSetting[1]), Integer.parseInt(partyLeaderColorSetting[2]), Integer.parseInt(partyLeaderColorSetting[3]), (Color col) -> {
				PartyHighlight.LEADER_OL_COLOR = col;
				PartyCircles.LEADER_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("partyLeader" + "_colorSetting", new String[]{"0", "74", "208", "164"});
				partyLeaderColorOptionWidget.cb.colorChooser.setColor(partyLeaderColorOptionWidget.currentColor = new Color(0, 74, 208, 164));
				PartyHighlight.LEADER_OL_COLOR = partyLeaderColorOptionWidget.currentColor;
				PartyCircles.LEADER_OL_COLOR = partyLeaderColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(myselfColorOptionWidget = new ColorOptionWidget("My own Circle/Highlight:", "myself", 246,
					Integer.parseInt(myselfLeaderColorSetting[0]), Integer.parseInt(myselfLeaderColorSetting[1]), Integer.parseInt(myselfLeaderColorSetting[2]), Integer.parseInt(myselfLeaderColorSetting[3]), (Color col) -> {
				PartyHighlight.MYSELF_OL_COLOR = col;
				PartyCircles.MYSELF_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("myself" + "_colorSetting", new String[]{"255", "255", "255", "128"});
				myselfColorOptionWidget.cb.colorChooser.setColor(myselfColorOptionWidget.currentColor = new Color(255, 255, 255, 128));
				PartyHighlight.MYSELF_OL_COLOR = myselfColorOptionWidget.currentColor;
				PartyCircles.MYSELF_OL_COLOR = myselfColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(aggroedEnemiesColorOptionWidget = new ColorOptionWidget("Aggroed Enemy Circles / Current Target Mark:", "aggroedEnemies", 246,
					Integer.parseInt(aggroedEnemiesColorSetting[0]), Integer.parseInt(aggroedEnemiesColorSetting[1]), Integer.parseInt(aggroedEnemiesColorSetting[2]), Integer.parseInt(aggroedEnemiesColorSetting[3]), (Color col) -> {
				AggroCircleSprite.col = col;
				refreshCurrentTargetSpriteColor = true;
				CurrentTargetSprite.col = new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 255));
				if (ui != null && ui.gui != null && ui.gui.fv != null && ui.gui.fv.lsrel != null){
					for (Fightview.Relation rel : ui.gui.fv.lsrel) {
						try {
							rel.destroy();
						} catch (Exception ignored) {}
					}
				}
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("aggroedEnemies" + "_colorSetting", new String[]{"255", "0", "0", "140"});
				aggroedEnemiesColorOptionWidget.cb.colorChooser.setColor(aggroedEnemiesColorOptionWidget.currentColor = new Color(255, 0, 0, 140));
				AggroCircleSprite.col = aggroedEnemiesColorOptionWidget.currentColor;
				CurrentTargetSprite.col = new BaseColor(new Color(255, 0, 0, 255));
				refreshCurrentTargetSpriteColor = true;
				if (ui != null && ui.gui != null && ui.gui.fv != null && ui.gui.fv.lsrel != null){
					for (Fightview.Relation rel : ui.gui.fv.lsrel) {
						try {
							rel.destroy();
						} catch (Exception ignored) {}
					}
				}
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			rightColumn = add(new Label("Combat Chase Vectors"), rightColumn.pos("bl").adds(0, 12).x(UI.scale(522)));
			rightColumn = add(myselfChaseVectorColorOptionWidget = new ColorOptionWidget("My own Vector:", "myselfChaseVector", 246,
					Integer.parseInt(myselfChaseVectorColorSetting[0]), Integer.parseInt(myselfChaseVectorColorSetting[1]), Integer.parseInt(myselfChaseVectorColorSetting[2]), Integer.parseInt(myselfChaseVectorColorSetting[3]), (Color col) -> {
				ChaseVectorSprite.MAINCOLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("myselfChaseVector" + "_colorSetting", new String[]{"255", "255", "255", "220"});
				myselfChaseVectorColorOptionWidget.cb.colorChooser.setColor(myselfChaseVectorColorOptionWidget.currentColor = new Color(255, 255, 255, 220));
				ChaseVectorSprite.MAINCOLOR = myselfChaseVectorColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(friendChaseVectorColorOptionWidget = new ColorOptionWidget("Party Member Vector:", "friendChaseVector", 246,
					Integer.parseInt(friendChaseVectorColorSetting[0]), Integer.parseInt(friendChaseVectorColorSetting[1]), Integer.parseInt(friendChaseVectorColorSetting[2]), Integer.parseInt(friendChaseVectorColorSetting[3]), (Color col) -> {
				ChaseVectorSprite.FRIENDCOLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("friendChaseVector" + "_colorSetting", new String[]{"47", "191", "7", "230"});
				friendChaseVectorColorOptionWidget.cb.colorChooser.setColor(friendChaseVectorColorOptionWidget.currentColor = new Color(47, 191, 7, 230));
				ChaseVectorSprite.FRIENDCOLOR = friendChaseVectorColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(foeChaseVectorColorOptionWidget = new ColorOptionWidget("Foe Vector:", "foeChaseVector", 246,
					Integer.parseInt(foeChaseVectorColorSetting[0]), Integer.parseInt(foeChaseVectorColorSetting[1]), Integer.parseInt(foeChaseVectorColorSetting[2]), Integer.parseInt(foeChaseVectorColorSetting[3]), (Color col) -> {
				ChaseVectorSprite.FOECOLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("foeChaseVector" + "_colorSetting", new String[]{"255", "0", "0", "230"});
				foeChaseVectorColorOptionWidget.cb.colorChooser.setColor(foeChaseVectorColorOptionWidget.currentColor = new Color(255, 0, 0, 230));
				ChaseVectorSprite.FOECOLOR = foeChaseVectorColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));
			rightColumn = add(unknownChaseVectorColorOptionWidget = new ColorOptionWidget("Animal / Random Irrelevant Player Vector:", "unknownChaseVector", 246,
					Integer.parseInt(unknownChaseVectorColorSetting[0]), Integer.parseInt(unknownChaseVectorColorSetting[1]), Integer.parseInt(unknownChaseVectorColorSetting[2]), Integer.parseInt(unknownChaseVectorColorSetting[3]), (Color col) -> {
				ChaseVectorSprite.UNKNOWNCOLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 1).x(UI.scale(400)));
			rightColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("unknownChaseVector" + "_colorSetting", new String[]{"255", "199", "0", "230"});
				unknownChaseVectorColorOptionWidget.cb.colorChooser.setColor(unknownChaseVectorColorOptionWidget.currentColor = new Color(255, 199, 0, 230));
				ChaseVectorSprite.UNKNOWNCOLOR = unknownChaseVectorColorOptionWidget.currentColor;
			}), rightColumn.pos("ur").adds(30, 0));
			rightColumn.tooltip = RichText.render("Reset to default color", UI.scale(300));

			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 18).x(UI.scale(285)));
			setTooltipsForColorSettingsStuff();
			pack();
		}
	}

	public static TextEntry webmapEndpointTextEntry;
	public static String mapClientEndpoint = Utils.getpref("webMapEndpoint", "");
	public static CheckBox enableMapUploaderCheckbox;
	public static boolean mapUploadBoolean = Utils.getprefb("enableMapUploader", false);
	public static CheckBox enableLocationTrackingCheckbox;
	public static boolean trackingEnableBoolean = Utils.getprefb("enableLocationTracking", false);
	public static Map<Color, Boolean> colorCheckboxesMap = new HashMap<>();
	public static TextEntry panicButtonApiTokenTextEntry;
	public static TextEntry panicButtonDiscordChannelIDTextEntry;
	public static TextEntry panicButtonDiscordMessageTextEntry;
	public static TextEntry panicButtonYourNicknameTextEntry;
	public static TextEntry panicButtonVillageNameTextEntry;

	static {
		for (Color color : BuddyWnd.gc) {
			colorCheckboxesMap.put(color, Utils.getprefb("enableMarkerUpload" + color.getRGB(), false));
		}
	}

	public class NDServerIntegrationSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}


		public NDServerIntegrationSettingsPanel(Panel back) {
			Widget prev;
			prev = add(new Label("Web Map Integration"), 110, 8);
			prev = add(new Label("Web Map Endpoint:"), prev.pos("bl").adds(0, 16).x(0));
			prev = add(webmapEndpointTextEntry = new TextEntry(UI.scale(220), mapClientEndpoint){
				protected void changed() {
					Utils.setpref("webMapEndpoint", this.buf.line());
					mapClientEndpoint = this.buf.line();
					super.changed();
				}
			}, prev.pos("ur").adds(6, 0));
			prev = add(enableMapUploaderCheckbox = new CheckBox("Enable Map Uploader"){
				{a = mapUploadBoolean;}
				public void set(boolean val) {
					Utils.setprefb("enableMapUploader", val);
					mapUploadBoolean = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 8).x(12));

			prev = add(enableLocationTrackingCheckbox = new CheckBox("Enable Location Tracking"){
				{a = trackingEnableBoolean;}
				public void set(boolean val) {
					Utils.setprefb("enableLocationTracking", val);
					trackingEnableBoolean = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(new Label("Markers to upload:"), prev.pos("bl").adds(0, 4));

			for (Map.Entry<Color, Boolean> entry : colorCheckboxesMap.entrySet()) {
				Color color = entry.getKey();
				boolean isChecked = entry.getValue();

				CheckBox colorCheckbox = new CheckBox(""){
					{a = isChecked;}
					@Override
					public void draw(GOut g) {
						g.chcolor(color);
						g.frect(Coord.z.add(0, (sz.y - box.sz().y) / 2), box.sz());
						g.chcolor();
						if(state())
							g.image(mark, Coord.z.add(0, (sz.y - mark.sz().y) / 2));
					}

					public void set(boolean val) {
						Utils.setprefb("enableMarkerUpload" + color.getRGB(), val);
						colorCheckboxesMap.put(color, val);
						a = val;
					}
				};
				prev = add(colorCheckbox, prev.pos("ur").adds(10, 0));
			}

			prev = add(new Label("Panic Button Integration"), prev.pos("bl").adds(0, 20).x(100));

			prev = add(new Label("Api Token:"), prev.pos("bl").adds(0, 16).x(0));
			prev = add(panicButtonApiTokenTextEntry = new TextEntry(UI.scale(220), Utils.getpref("panicButtonApiToken", "")){
				protected void changed() {
					Utils.setpref("panicButtonApiToken", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(104)));

			prev = add(new Label("Discord Channel ID:"), prev.pos("bl").adds(0, 12).x(0));
			prev = add(panicButtonDiscordChannelIDTextEntry = new TextEntry(UI.scale(220), Utils.getpref("panicButtonDiscordChannelID", "")){
				protected void changed() {
					Utils.setpref("panicButtonDiscordChannelID", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(104)));

			RichText discordMessageTooltip = RichText.render("This is the message that will be sent in the discord text channel and/or village, along with the Nickname you set below.", UI.scale(300));
			prev = add(new Label("Discord Message:"), prev.pos("bl").adds(0, 12).x(0));
			prev.tooltip = discordMessageTooltip;
			prev = add(panicButtonDiscordMessageTextEntry = new TextEntry(UI.scale(220), Utils.getpref("panicButtonDiscordMessage", "@here Help! I'm being chased! (Panic Button)")){
				protected void changed() {
					Utils.setpref("panicButtonDiscordMessage", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(104)));
			prev.tooltip = discordMessageTooltip;

			RichText yourNameTooltip = RichText.render("This is the nickname that will be included in the discord message, to know who's asking for help.\n$col[185,185,185]{You can set this to your discord name, or something.}", UI.scale(300));
			prev = add(new Label("Your Nickname:"), prev.pos("bl").adds(0, 12).x(0));
			prev.tooltip = yourNameTooltip;
			prev = add(panicButtonYourNicknameTextEntry = new TextEntry(UI.scale(220), Utils.getpref("panicButtonNickname", "NightdawgFanboy69")){
				protected void changed() {
					Utils.setpref("panicButtonNickname", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(104)));
			prev.tooltip = yourNameTooltip;

			RichText villageNameTooltip = RichText.render("This is the In-Game Village Name.\n$col[185,185,185]{This is needed to send the message in the village chat.}", UI.scale(300));
			prev = add(new Label("Village Name:"), prev.pos("bl").adds(0, 12).x(0));
			prev.tooltip = villageNameTooltip;
			prev = add(panicButtonVillageNameTextEntry = new TextEntry(UI.scale(220), Utils.getpref("panicButtonVillageName", "Ex: London Mandem")){
				protected void changed() {
					Utils.setpref("panicButtonVillageName", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(0, -2).x(UI.scale(104)));
			prev.tooltip = villageNameTooltip;


			add(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 30).x(UI.scale(62)));

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
		Panel worldgraphicssettings = add(new NDWorldGraphicsSettingsPanel(advancedSettings));
		Panel actionbarsettings = add(new NDActionBarSettingsPanel(advancedSettings));
		Panel camsettings = add(new NDCamSettingsPanel(advancedSettings));
		Panel gameplaysettings = add(new NDGameplaySettingsPanel(advancedSettings));
		Panel combatsettings = add(new NDCombatSettingsPanel(advancedSettings));
		Panel hidingsettings = add(new NDHidingSettingsPanel(advancedSettings));
		Panel dropsettings = add(new NDAutoDropSettingsPanel(advancedSettings));
		Panel alarmsettings = add(new NDAlarmsAndSoundsSettingsPanel(advancedSettings));
		Panel colorsettings = add(new NDColorSettingsPanel(advancedSettings));
		Panel serverintegrationsettings = add(new NDServerIntegrationSettingsPanel(advancedSettings));


		int y2 = UI.scale(6);
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Interface & Display Settings", -1, iface, "Interface & Display Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Action Bars Settings", -1, actionbarsettings, "Action Bars Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "World Graphics Settings", -1, worldgraphicssettings, "World Graphics Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Camera Settings", -1, camsettings, "Camera Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Gameplay Settings", -1, gameplaysettings, "Gameplay Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Combat Settings", -1, combatsettings, "Combat Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Hiding Settings", -1, hidingsettings, "Hiding Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Mining Auto-Drop Settings", -1, dropsettings, "Mining Auto-Drop Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Alarms & Sounds Settings", -1, alarmsettings, "Alarms & Sounds Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Color Settings", -1, colorsettings, "Color Settings"), 0, y2).pos("bl").adds(0, 5).y;
		y2 = advancedSettings.add(new PButton(UI.scale(200), "Server Integration Settings", -1, serverintegrationsettings, "Server Integration Settings"), 0, y2).pos("bl").adds(0, 25).y;

		y2 = advancedSettings.add(new PButton(UI.scale(200), "Back", 27, main, "Options            "), 0, y2).pos("bl").adds(0, 5).y;
		this.advancedSettings.pack();
	// ND: Continue with the main panel whatever
	int y = UI.scale(6);
	Widget prev;
	y = main.add(new PButton(UI.scale(200), "Video Settings", -1, video, "Video Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Audio Settings", -1, audio, "Audio Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Keybindings (Hotkeys)", -1, keybind, "Keybindings (Hotkeys)"), 0, y).pos("bl").adds(0, 25).y;

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
		allowLowerFreeCamTiltCheckBox.visible = bool;
		revertFreeCameraXAxisCheckBox.visible = bool;
		revertFreeCameraYAxisCheckBox.visible = bool;
	}
	private void setOrthoCameraSettingsVisibility(boolean bool){
		unlockedOrthoCamCheckBox.visible = bool;
		orthoCamZoomSpeedLabel.visible = bool;
		orthoCamZoomSpeedSlider.visible = bool;
		orthoCamZoomSpeedResetButton.visible = bool;
		revertOrthoCameraAxesCheckBox.visible = bool;
	}
	private void setTooltipsForCameraSettingsStuff(){
		revertOrthoCameraAxesCheckBox.tooltip = RichText.render("Enabling this will revert the Horizontal axis when dragging the camera to look around.\n$col[185,185,185]{I don't know why Loftar inverts it in the first place...}", UI.scale(280));
		unlockedOrthoCamCheckBox.tooltip = RichText.render("Enabling this allows you to rotate the Ortho camera freely, without locking it to only 4 view angles.", UI.scale(280));
		freeCamZoomSpeedResetButton.tooltip = RichText.render("Reset to default", UI.scale(300));
		freeCamHeightResetButton.tooltip = RichText.render("Reset to default", UI.scale(300));
		orthoCamZoomSpeedResetButton.tooltip = RichText.render("Reset to default", UI.scale(300));
		allowLowerFreeCamTiltCheckBox.tooltip = RichText.render("Enabling this will allow you to tilt the camera below the character and look upwards.\n$col[200,0,0]{WARNING: Be careful when using this setting in combat! You're not able to click on the ground when looking at the world from below.}\n$col[185,185,185]{Honestly just enable this when you need to take a screenshot or something, and keep it disabled the rest of the time. I added this option for fun.}", UI.scale(300));
		freeCamHeightLabel.tooltip = RichText.render("This affects the height of the point at which the free camera is pointed. By default, it is pointed right above the player's head.\n$col[185,185,185]{This doesn't really affect gameplay that much, if at all. With this setting, you can make it point at the feet, or torso, or head, or whatever.}", UI.scale(300));
	}

	private void setTooltipsForInterfaceSettingsStuff() {
		enableCornerFPSCheckBox.tooltip = RichText.render("Enabling this will display the current FPS in the top-right corner of the screen.", UI.scale(300));
		enableAdvancedMouseInfoCheckBox.tooltip = RichText.render("Holding Ctrl+Shift will show the Resource Path of the object or tile you are mousing over. Enabling this option will show additional information.\n$col[185,185,185]{Unless you're a client dev, you don't really need to enable this option.}", UI.scale(300));
		enableWrongResCheckBox.tooltip = RichText.render("$col[185,185,185]{Unless you're a client dev, you don't really need to enable this option.}", UI.scale(300));
		useAlternativeUi.tooltip = RichText.render("$col[185,185,185]{Use alternative, simplified UI theme, Buttons might bug a bit, so recommending game restart.}", UI.scale(300));
		enableDragWindowsInWhenResizingCheckBox.tooltip = RichText.render("Enabling this will force ALL Windows to be dragged back inside the Game Window, whenever you resize it.\n$col[218,163,0]{Note:} $col[185,185,185]{By default, windows will remain in the same spot when you resize your Game Window, even if they're outside of it.", UI.scale(300));
		enableSnapWindowsBackInsideCheckBox.tooltip = RichText.render("Enabling this cause most windows to be fully snapped back into your Game's Window.\nBy default, when you try to drag a window outside of your Game Window, it will only pop 25% of it back in.\n$col[185,185,185]{Large windows like the Cattle Roster or Cook Book are not affected by this setting. The 25% rule always applies to them.}", UI.scale(300));
		interfaceScaleLabel.tooltip = RichText.render("$col[218,163,0]{Warning:} This setting is by no means perfect, and it can mess up many things." +
				"\nSome windows might just break when this is set above 1.00x." +
				"\n$col[185,185,185]{Honestly, fuck this setting. Unless you're on a 4K or 8K display, keep this at 1.00x.}", UI.scale(300));
		interfaceScaleHSlider.tooltip = RichText.render("$col[218,163,0]{Warning:} This setting is by no means perfect, and it can mess up many things." +
				"\nSome windows might just break when this is set above 1.00x." +
				"\n$col[185,185,185]{Honestly, fuck this setting. Unless you're on a 4K or 8K display, keep this at 1.00x.}", UI.scale(300));
		granularityPositionLabel.tooltip = RichText.render("Equivalent of the :placegrid console command, this allows you to have more freedom when placing constructions/objects.", UI.scale(300));
		granularityAngleLabel.tooltip = RichText.render("Equivalent of the :placeangle console command, this allows you to have more freedom when rotating constructions/objects before placement.", UI.scale(300));
		alwaysOpenBeltCheckBox.tooltip = RichText.render("Enabling this will cause your belt window to always open when you log in.\n$col[218,163,0]{Note:} $col[185,185,185]{By default, Loftar saves the status of the belt at logout. So if you don't enable this setting, but leave the belt window open when you log out/exit the game, it will still open on login.}", UI.scale(300));
		showQuickSlotsBar.tooltip = RichText.render("$col[218,163,0]{Note:} The Quick Switch keybinds ('Right Hand' and 'Left Hand') will still work, regardless of this widget being visible or not.", UI.scale(300));
		toggleGobGrowthInfoCheckBox.tooltip = RichText.render("Enabling this will show the following growth information:\n$col[185,185,185]{> Trees and Bushes will display their current growth percentage\n> Crops will display their growth stage as \"Current / Final\"\n}$col[218,163,0]{Note:} If a Tree or Bush is not showing a percentage, that means it is fully grown, and can be harvested.\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(300));
		toggleGobCollisionBoxesDisplayCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using a Hotkey.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Collision Box Color can be changed in the Color Settings.}", UI.scale(300));
		toggleBeastDangerRadiiCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		toggleCritterAurasCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		showContainerFullnessCheckBox.tooltip = RichText.render("Enabling this will overlay the following colors over Container Objects, to indicate their fullness:" +
				"\n=====================" +
				"\n$col[185,0,0]{Red: }$col[255,255,255]{Full}\n$col[224,213,0]{Yellow: }$col[255,255,255]{Contains some}\n$col[0,185,0]{Green: }$col[255,255,255]{Empty}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Highlight Colors can be changed in the Color Settings.}", UI.scale(310));
		showWorkstationStageCheckBox.tooltip = RichText.render("Enabling this will overlay the following colors over Workstation Objects (Drying Frame, Tanning Tub, Garden Pot, Cheese Rack), to indicate their progress stage:" +
				"\n=====================" +
				"\n$col[185,0,0]{Red: }$col[255,255,255]{Finished}\n$col[224,213,0]{Yellow: }$col[255,255,255]{In progress}\n$col[0,185,0]{Green: }$col[255,255,255]{Prepared (Ready for use)}\n$col[160,160,160]{Gray: }$col[255,255,255]{Unprepared (Missing: Water, Soil, Tanning Liquid, etc.)}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Highlight Colors can be changed in the Color Settings.}", UI.scale(310));
		displayGatePassabilityBoxesCheckBox.tooltip = RichText.render("By default, these only show up while you're in combat." +
				"\nEnabling this will cause a collision box to be displayed under gates at all times.\nThe displayed colors depend on the gate type, and your combat status." +
				"\n=====================" +
				"\n$col[0,160,0]{Green: }$col[185,185,185]{Normal Gate, Open and Passable, even in combat}" +
				"\n$col[224,213,0]{Yellow: }$col[185,185,185]{Visitor Gate, Open and Passable (you're out of combat)}" +
				"\n$col[224,150,0]{Orange: }$col[185,185,185]{Visitor Gate, Open, but NOT Passable (you're in combat)}" +
				"\n$col[185,0,0]{Red: }$col[185,185,185]{Normal/Visitor Gate, Closed, so it's NOT Passable}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		highlightCliffsCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Highlight Color can be changed in the Color Settings.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		objectPermanentHighlightingCheckBox.tooltip = RichText.render("Enabling this setting will allow you to highlight objects by using Alt + Middle Click (Mouse Scroll Click)." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Objects remain highlighted until you completely restart your client, even if you switch characters or accounts. " +
				"\nIf you want to reset the highlighted objects without restarting the client, you can disable and re-enable this setting.}", UI.scale(320));
		alwaysShowStaminaBarCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Stamina Bar will still appear out of combat, when you are drinking, regardless of this option being enabled or not.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The position of the Stamina Bar depends on the position of the Combat UI Top Panel (Combat Settings).}", UI.scale(320));
		alwaysShowHealthBarCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The position of the Health Bar depends on the position of the Combat UI Top Panel (Combat Settings).}", UI.scale(320));
		showStudyWindowHistoryCheckBox.tooltip = RichText.render("If this is enabled, the Study Report will show what curiosity was formerly placed in each slot. The history is saved separately for every Account and Character." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{It does not work for Gems. Don't ask me why.}", UI.scale(300));
		lockStudyWindowCheckBox.tooltip = RichText.render("Enabling this will prevent moving or dropping items from the Study Report", UI.scale(300));
		disableMenuGridHotkeysCheckBox.tooltip = RichText.render("This option completely disables the hotkeys for the Action Buttons & Categories in the bottom right corner menu (aka the Menu Grid)." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Your Action Bar Keybinds are not affected by this setting.}", UI.scale(300));
		enableMineSweeperCheckBox.tooltip = RichText.render("Enabling this will cause cave dust tiles to show the number of potential cave-ins surrounding them, just like in Minesweeper." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{If a cave-in has been mined out, the tiles surrounding it will still drop cave dust, and they will still show a number on the ground. The cave dust tiles are pre-generated with the world. That's just how Loftar coded it.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{You can still pick up the cave dust item off the ground. The numbers are affected only by the duration of the falling dust particles effect (aka dust rain), which can be set below}" +
				"\n\n$col[200,0,0]{NOTE:} $col[185,185,185]{There's a bug with the falling dust particles, that we can't really \"fix\". If you mine them out on a level, the same particles can also show up on different levels or the overworld. If you want them to vanish, you can just relog, but they will despawn from their original location too.}", UI.scale(300));
	}

	private void setTooltipsForCombatSettingsStuff(){
		useProperCombatUICheckBox.tooltip = RichText.render("I don't even know why I'm allowing you to disable this and use Loftar's default UI.\nLike, why would you ever?", UI.scale(300));
		toggleGobDamageInfoCheckBox.tooltip = RichText.render("Enabling this will display the total amount of damage players and animals took.\n$col[218,163,0]{Note:} The damage you will see saved above players/animals is the total damage you saw the entity take, while inside of your view range. This is not all of the damage said entity might have taken recently.\n$col[185,185,185]{If you change any of the settings below, you will need a damage update in order to see the changes (for example, deal some damage to the player/animal).}", UI.scale(300));
		damageInfoClearButton.tooltip = RichText.render("Clears all damage info.\n$col[218,163,0]{Note:} $col[185,185,185]{This can also be done using an Action Button.}", UI.scale(320));
		toggleAutoPeaceCheckbox.tooltip = RichText.render("Enabling this will automatically set your status to 'Peace' when combat is initiated with a new target (animals only). Toggling this on while in combat will also autopeace all animals you are currently fighting.\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(300));
		partyMembersHighlightCheckBox.tooltip = RichText.render("Enabling this will put a color highlight over all party members." +
				"\n=====================" +
				"\n$col[255,255,255]{White: }$col[185,185,185]{Yourself}\n$col[0,74,208]{Blue: }$col[185,185,185]{Party Leader}\n$col[0,160,0]{Green: }$col[185,185,185]{Other Members}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{If you are the party leader, your color highlight will always be $col[0,74,208]{Blue}, rather than $col[255,255,255]{White}.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Highlight Colors can be changed in the Color Settings.}", UI.scale(310));
		partyMembersCirclesCheckBox.tooltip = RichText.render("Enabling this will put a colored circle under all party members." +
				"\n=====================" +
				"\n$col[255,255,255]{White: }$col[185,185,185]{Yourself}\n$col[0,74,208]{Blue: }$col[185,185,185]{Party Leader}\n$col[0,160,0]{Green: }$col[185,185,185]{Other Members}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{If you are the party leader, your circle's color will always be $col[0,74,208]{Blue}, rather than $col[255,255,255]{White}.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Circle Colors can be changed in the Color Settings.}", UI.scale(300));
		drawChaseVectorsCheckBox.tooltip = RichText.render("If this setting is enabled, colored lines will be drawn between chasers and chased targets." +
				"\n=====================" +
				"\n$col[255,255,255]{White: }$col[185,185,185]{You are the chaser.}" +
				"\n$col[0,160,0]{Green: }$col[185,185,185]{A party member is the chaser.}" +
				"\n$col[185,0,0]{Red: }$col[185,185,185]{A player is chasing you or a party member.}" +
				"\n$col[224,213,0]{Yellow: }$col[185,185,185]{An animal is the chaser, OR random (non-party) players are chasing each other.}" +
				"\n=====================" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Chase vectors include queuing attacks, clicking a critter to pick up, or simply following someone.}" +
				"\n$col[218,163,0]{Disclaimer:} $col[185,185,185]{Chase vectors sometimes don't show when chasing a critter that is standing still. The client treats this as something else for some reason and I can't fix it.}" +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{The Chase Vector Colors can be changed in the Color Settings.}", UI.scale(430));
		markCurrentCombatTargetCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Target Mark Color can be changed in the Color Settings.}" , UI.scale(400));
		aggroedEnemiesCirclesCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Circle Color can be changed in the Color Settings.}" , UI.scale(300));
	}
	private void setTooltipsForGameplaySettingsStuff(){
		defaultSpeedLabel.tooltip = RichText.render("Sets your character's movement speed on login.", UI.scale(300));
		instantFlowerMenuCTRLCheckBox.tooltip = RichText.render("Enabling this will make holding Ctrl before right clicking an item or object to instantly select the first available option from the flower menu.", UI.scale(300));
		autoFlowerCTRLSHIFTCheckBox.tooltip = RichText.render("Enabling this will trigger the Auto Flower Repeater Script to run when you Right Click an item while holding Ctrl + Shift." +
				"\n\n$col[185,185,185]{You have} $col[218,163,0]{2 seconds} $col[185,185,185]{to select a Flower Menu option, after which the script will automatically click the selected option for ALL items that have the same name in your inventory.}" +
				"\n$col[200,0,0]{If you don't select an option within} $col[218,163,0]{2 seconds}$col[200,0,0]{, the script won't run.}" +
				"\n\nYou can stop the script before it finishes by pressing ESC." +
				"\n\n$col[218,163,0]{Example:} You have 10 Oak Blocks in your inventory. You hold Ctrl + Shift and right click one of the Oak Blocks and select \"Split\" in the flower menu. The script starts running and it splits all 10 Oak Blocks." +
				"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This script only runs on items that have the same name inside your inventory. It does not take into consideration items inside other containers, or items of the same \"type\" (for example, if you run the script on Oak Blocks, it won't also run on Spruce Blocks).} ", UI.scale(310));
		autoswitchBunnyPlateBootsCheckBox.tooltip = RichText.render("Enabling this will cause your currently equipped Plate Boots to automatically swap with a pair of bunny slippers from your inventory, whenever you right click to chase a rabbit, and vice versa if you click on anything else or just left click to walk.\n$col[185,185,185]{I don't see any reason for which you'd ever want to disable this setting, but alas, I made it an option.}", UI.scale(300));
		saveCutleryCheckBox.tooltip = RichText.render("Enabling this will cause any cutlery that has 1 wear left to be instantly transferred from the table into your inventory.\n$col[185,185,185]{A warning message will be shown, to let you know that the item has been transferred.}", UI.scale(300));
		noCursorItemDroppingCheckBox.tooltip = RichText.render("$col[185,185,185]{You can still drop the item on your cursor if you hold Ctrl.}\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}" +
				"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}", UI.scale(300));
		noCursorItemDroppingInWaterCheckBox.tooltip = RichText.render("$col[218,163,0]{Warning:} If the previous option is Enabled, it will overwrite this one. You will still not be able to drop items in water.\n$col[185,185,185]{You can still drop the item on your cursor if you hold Ctrl.}\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}" +
				"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}", UI.scale(300));
		autoDrinkTeaWhileWorkingCheckBox.tooltip = RichText.render("When your goes reaches below 70%, automatically drink Tea or Water (depending on your current Energy).", UI.scale(300));
		autoStudyCheckBox.tooltip = RichText.render("If this is enabled, curiosities will be automatically replaced in the Study Report once they finish being studied." +
				"\nIt picks items from your Inventory and currently open Cupboards (only Cupboards, no other containers)." +
				"\n$col[218,163,0]{Note:} $col[185,185,185]{Once a curiosity is studied, this will only look for a replacement that has the same name. It does not actually try picking new items that are not currently being studied.}", UI.scale(300));


	}

	private void setTooltipsForGraphicsSettingsStuff(){
		nightVisionLabel.tooltip = RichText.render("Increasing this will simulate daytime lighting during the night.\n$col[185,185,185]{It can slightly affect the light levels during the day too, but it is barely noticeable.}" +
				"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using a Hotkey." +
				"\nThe hotkey just switches the value of Night Vision / Brighter World between Minimum and Maximum value.}", UI.scale(380));
		nightVisionResetButton.tooltip = RichText.render("Reset to default", UI.scale(300));
		disableWeatherEffectsCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} This disables *ALL* weather and camera effects, including rain effects, drunkenness distortion, drug high, valhalla gray overlay, camera shake, and any other similar effects.", UI.scale(300));
		disableFlavourObjectsCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} This only disables random objects that appear in the world which you cannot interact with.\n$col[185,185,185]{Players usually disable flavour objects to improve visibility, especially in combat.}\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		flatWorldCheckBox.tooltip = RichText.render("Enabling this will make the entire game world terrain flat.\n$col[185,185,185]{Cliffs will still be drawn with their relative height, scaled down.}\n$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		tileTransitionsCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		tileSmoothingCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		showMineSupportRadiiCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		showMineSupportSafeTilesCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using an Action Button.}", UI.scale(320));
		disableTreeAndBushSwayingCheckBox.tooltip = RichText.render("This can improve your the FPS.", UI.scale(300));
		disableSomeGobAnimations.tooltip = RichText.render("Stop certain animations: Fire, trash stockpile, beehive. Should improve FPS a bit when seeing a lot of those.", UI.scale(300));
		disableIndustrialSmoke.tooltip = RichText.render("Disable smelter, tarkiln and few more smoke animations. To show again need to either relog or just walk outside viewing distance.", UI.scale(300));
		disableScentSmoke.tooltip = RichText.render("Disable scent smoke animations. To show again need to either relog or just walk outside viewing distance.", UI.scale(300));
	}

	private void setTooltipsForHidingSettingsStuff(){
		toggleGobHidingCheckBox.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This option can also be turned on/off using a Hotkey.", UI.scale(300));
		hiddenObjectsColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Hidden Object Box Edges are always visible, even through other objects, at a fixed transparency (40%)." +
				"\nThe transparency level you set here only affects the filled box.}", UI.scale(310));
	}

	private void setTooltipsForAlarmSettingsStuff(){
		lowEnergySoundEnabledCheckbox.tooltip = RichText.render("This alarm will also trigger again when you reach <2000% energy.\n$col[185,185,185]{Don't starve yourself, dumbass.}", UI.scale(400));
		whiteVillageOrRealmPlayerAlarmEnabledCheckbox.tooltip = RichText.render("This alarm will only be triggered on White or Unmemorised Village/Realm Members.\n$col[185,185,185]{If you have them Kinned or Memorised and have changed their Kin color from White, the alarm of their respective kin color will be triggered instead (if enabled).}", UI.scale(300));
	}

	private void setTooltipsForColorSettingsStuff(){
		hiddenObjectsColorOptionWidget2.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{The Hidden Object Box Edges are always visible, even through other objects, at a fixed transparency (40%)." +
				"\nThe transparency level you set here only affects the filled box.}", UI.scale(310));
		collisionBoxesColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{Collision boxes for gates and other passable objects will use the combat passability colors, which can be set further below." +
				"\n(The transparency levels are the same for all collision boxes, which you set here!)}", UI.scale(310));
		closedGateColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This also affects the Collision Box Color, but not the transparency.}", UI.scale(350));
		openVisitorGateNoCombatColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This also affects the Collision Box Color, but not the transparency.}", UI.scale(350));
		openVisitorGateInCombatColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This also affects the Collision Box Color, but not the transparency.}", UI.scale(350));
		passableGateCombatColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{This also affects the Collision Box Color, but not the transparency.}", UI.scale(350));
		permanentHighlightColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{For this to work, this option must be Enabled in Interface & Display Settings!}", UI.scale(400));
		myselfColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Remember:} $col[185,185,185]{If you're the Party Leader, your own color is overwritten by the Party Leader Color.}", UI.scale(310));
		aggroedEnemiesColorOptionWidget.tooltip = RichText.render("$col[218,163,0]{Note:} $col[185,185,185]{For the sake of visibility, the Current Target transparency level is not affected by this setting.\nIt always stays fully visible, with the lowest transparency (0%).}", UI.scale(300));
		myselfChaseVectorColorOptionWidget.tooltip = RichText.render("$col[185,185,185]{You are the chaser.}", UI.scale(350));
		friendChaseVectorColorOptionWidget.tooltip = RichText.render("$col[185,185,185]{A party member is the chaser.}", UI.scale(350));
		foeChaseVectorColorOptionWidget.tooltip = RichText.render("$col[185,185,185]{A player is chasing you or a party member.}", UI.scale(350));
		unknownChaseVectorColorOptionWidget.tooltip = RichText.render("$col[185,185,185]{An animal is the chaser, OR random (non-party) players are chasing each other.}", UI.scale(350));
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