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

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.io.*;
import java.nio.file.*;
import java.awt.image.*;
import java.awt.Color;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.filechooser.*;

public class GobIcon extends GAttrib {
    private static final int size = UI.scale(20);
    public static final PUtils.Convolution filter = new PUtils.Hanning(1);
    private static final Map<Indir<Resource>, Image> cache = new WeakHashMap<>();
    private static final Map<Indir<Resource>, Image> cachegray = new WeakHashMap<>();
    public final Indir<Resource> res;
    private Image img, imggray;
	private static LinkedHashMap<String, ArrayList<String>> mapIconPresets = new LinkedHashMap<String, ArrayList<String>>();

    public GobIcon(Gob g, Indir<Resource> res) {
	super(g);
	this.res = res;
    }

    public static class Image {
	public final Tex tex;
	public Coord cc;
	public boolean rot;
	public double ao;
	public int z;

	public Image(Resource.Image rimg) {
	    Tex tex = rimg.tex();
	    if ((tex.sz().x > size) || (tex.sz().y > size)) {
		BufferedImage buf = rimg.img;
		buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
		Coord tsz;
		if(buf.getWidth() > buf.getHeight())
		    tsz = new Coord(size, (size * buf.getHeight()) / buf.getWidth());
		else
		    tsz = new Coord((size * buf.getWidth()) / buf.getHeight(), size);
		buf = PUtils.convolve(buf, tsz, filter);
		tex = new TexI(buf);
	    }
	    this.tex = tex;
	    this.cc = tex.sz().div(2);
	    byte[] data = rimg.kvdata.get("mm/rot");
	    if(data != null) {
		this.rot = true;
		this.ao = Utils.float32d(data, 0) * (Math.PI / 180f);
	    }
	    this.z = rimg.z;
	    data = rimg.kvdata.get("mm/z");
	    if(data != null)
		this.z = Utils.intvard(data, 0);
	}

	public Image(Resource.Image rimg, BufferedImage img) {
		Tex tex = new TexI(img);
		if ((tex.sz().x > size) || (tex.sz().y > size)) {
			BufferedImage buf;
			buf = PUtils.rasterimg(PUtils.blurmask2(img.getRaster(), 1, 1, Color.BLACK));
			Coord tsz;
			if(buf.getWidth() > buf.getHeight())
				tsz = new Coord(size, (size * buf.getHeight()) / buf.getWidth());
			else
				tsz = new Coord((size * buf.getWidth()) / buf.getHeight(), size);
			buf = PUtils.convolve(buf, tsz, filter);
			tex = new TexI(buf);
		}
		this.tex = tex;
		this.cc = tex.sz().div(2);
		byte[] data = rimg.kvdata.get("mm/rot");
		if(data != null) {
			this.rot = true;
			this.ao = Utils.float32d(data, 0) * (Math.PI / 180f);
		}
		this.z = rimg.z;
		data = rimg.kvdata.get("mm/z");
		if(data != null)
			this.z = Utils.intvard(data, 0);
	}

    }

    public Image img() {
	if(this.img == null) {
	    synchronized(cache) {
		Image img = cache.get(res);
		if(img == null) {
		    img = new Image(res.get().layer(Resource.imgc));
		    cache.put(res, img);
		}
		this.img = img;
	    }
	}
	return(this.img);
    }

    public Image imggray() {
        if(this.imggray == null) {
            synchronized(cachegray) {
                Image img = cachegray.get(res);
                if(img == null) {
                    Resource.Image rimg = res.get().layer(Resource.imgc);
                    img = new Image(rimg, PUtils.monochromizeCopy(rimg.img, Color.WHITE));
                    cachegray.put(res, img);
                }
                this.imggray = img;
            }
        }
        return(this.imggray);
    }

    private static Consumer<UI> resnotif(String nm) {
	return(ui -> {
		Indir<Resource> resid = Resource.local().load(nm);
		ui.sess.glob.loader.defer(() -> {
			Resource res;
			try {
			    res = resid.get();
			} catch(Loading l) {
			    throw(l);
			} catch(RuntimeException e) {
			    ui.error("Could not play " + nm);
			    return;
			}
			Audio.CS clip = Audio.fromres(res);
			ui.sfx(clip);
		    }, null);
	    });
    }

    private static Consumer<UI> wavnotif(Path path) {
	return(ui -> {
		ui.sess.glob.loader.defer(() -> {
			Audio.CS clip;
			InputStream fail = null;
			try {
			    fail = Files.newInputStream(path);
			    clip = Audio.PCMClip.fromwav(new BufferedInputStream(fail));
			    fail = null;
			} catch(IOException e) {
			    String msg = e.getMessage();
			    if(e instanceof FileSystemException)
				msg = "Could not open file";
			    ui.error("Could not play " + path + ": " + msg);
			    return;
			} finally {
			    if(fail != null) {
				try {
				    fail.close();
				} catch(IOException e) {
				    new Warning(e, "unexpected error on close").issue();
				}
			    }
			}
			ui.sfx(clip);
		    }, null);
	    });
    }

    private static final Map<Object, Double> lastnotifs = new HashMap<>();
    private static Consumer<UI> notiflimit(Consumer<UI> bk, Object id) {
	return(ui -> {
		double now = Utils.rtime();
		synchronized(lastnotifs) {
		    Double last = lastnotifs.get(id);
		    if((last != null) && (now - last < 0.5))
			return;
		    lastnotifs.put(id, now);
		}
		bk.accept(ui);
	    });
    }

    public static class Setting implements Serializable {
	public Resource.Spec res;
	public boolean show, defshow, notify;
	public String resns;
	public Path filens;

	public Setting(Resource.Spec res) {
	    this.res = res;
	}

	public Consumer<UI> notification() {
	    if(resns != null)
		return(notiflimit(resnotif(resns), resns));
	    if(filens != null)
		return(notiflimit(wavnotif(filens), filens));
	    return(null);
	}
    }

    public static class Settings implements Serializable {
	public static final byte[] sig = "Icons".getBytes(Utils.ascii);
	public Map<String, Setting> settings = new HashMap<>();
	public int tag = -1;
	public boolean notify = false;

	public Setting get(Resource.Named res) {
	    Setting ret = settings.get(res.name);
	    if((ret != null) && (ret.res.ver < res.ver))
		ret.res = new Resource.Spec(null, res.name, res.ver);
	    return(ret);
	}

	public Setting get(Resource res) {
	    return(get(res.indir()));
	}

	public void receive(int tag, Setting[] conf) {
	    Map<String, Setting> nset = new HashMap<>(settings);
	    for(int i = 0; i < conf.length; i++) {
		String nm = conf[i].res.name;
		Setting prev = nset.get(nm);
		if(prev == null)
		    nset.put(nm, conf[i]);
		else if(prev.res.ver < conf[i].res.ver)
		    prev.res = conf[i].res;
	    }
	    this.settings = nset;
	    this.tag = tag;
	}

	public void save(Message buf) {
	    buf.addbytes(sig);
	    buf.adduint8(2);
	    buf.addint32(tag);
	    buf.adduint8(notify ? 1 : 0);
	    for(Setting set : settings.values()) {
		buf.addstring(set.res.name);
		buf.adduint16(set.res.ver);
		buf.adduint8((byte)'s');
		buf.adduint8(set.show ? 1 : 0);
		buf.adduint8((byte)'d');
		buf.adduint8(set.defshow ? 1 : 0);
		if(set.notify) {
		    buf.adduint8((byte)'n');
		    buf.adduint8(1);
		}
		if(set.resns != null) {
		    buf.adduint8((byte)'R');
		    buf.addstring(set.resns);
		} else if(set.filens != null) {
		    buf.adduint8((byte)'W');
		    buf.addstring(set.filens.toString());
		}
		buf.adduint8(0);
	    }
	    buf.addstring("");
	}

		public static void removeEnderCustomIcons(Map<String, GobIcon.Setting> settings) {
			//System.out.println("Removing custom icons");
			removeSetting(settings, "paginae/act/hearth");
			removeSetting(settings, "radar/hearthfire");
			removeSetting(settings, "radar/dugout");
			removeSetting(settings, "radar/wheelbarrow");
			removeSetting(settings, "radar/knarr");
			removeSetting(settings, "radar/gem");
			removeSetting(settings, "radar/horse/mare");
			removeSetting(settings, "radar/horse/stallion");
			removeSetting(settings, "radar/rowboat");
			removeSetting(settings, "radar/snekkja");
			removeSetting(settings, "radar/milestone-stone-m");
			removeSetting(settings, "radar/milestone-stone-e");
			removeSetting(settings, "radar/midgeswarm");
		}

		private static void removeSetting(Map<String, GobIcon.Setting> settings, String res) {
			if(settings.containsKey(res)) {
				settings.remove(res);
				//System.out.println("Removed custom icon");
			}
		}

	private static final List<String> mandatoryEnabledIcons = Arrays.asList("gfx/hud/mmap/plo", "gfx/hud/mmap/cave", "gfx/terobjs/mm/watervortex", "gfx/terobjs/mm/boostspeed");
	public static Settings load(Message buf, UI ui) {
	    if(!Arrays.equals(buf.bytes(sig.length), sig))
		throw(new Message.FormatError("Invalid signature"));
	    int ver = buf.uint8();
	    if((ver < 1) || (ver > 2))
		throw(new Message.FormatError("Unknown version: " + ver));
	    Settings ret = new Settings();
	    ret.tag = buf.int32();
	    if(ver >= 2)
		ret.notify = (buf.uint8() != 0);
	    while(true) {
		String resnm = buf.string();
		if(resnm.equals(""))
		    break;
		int resver = buf.uint16();
		Resource.Spec res = new Resource.Spec(null, resnm, resver);
		Setting set = new Setting(res);
		boolean setdef = false;
		data: while(true) {
		    int datum = buf.uint8();
		    switch(datum) {
		    case (int)'s':
			set.show = (buf.uint8() != 0);
			break;
		    case (int)'d':
			set.defshow = (buf.uint8() != 0);
			setdef = true;
			break;
		    case (int)'n':
			set.notify = (buf.uint8() != 0);
			break;
		    case (int)'R':
			set.resns = buf.string();
			break;
		    case (int)'W':
			try {
			    set.filens = Utils.path(buf.string());
			} catch(RuntimeException e) {
			    new Warning(e, "could not read path").issue();
			}
			break;
		    case 0:
			break data;
		    default:
			throw(new Message.FormatError("Unknown datum: " + datum));
		    }
		}
		if(!setdef)
		    set.defshow = set.show;

		if (mandatoryEnabledIcons.stream().anyMatch(set.res.name::matches))
			set.defshow = set.show = true;

		ret.settings.put(res.name, set);
	    }
		removeEnderCustomIcons(ret.settings);
		CustomMapIcons.addCustomSettings(ret.settings, ui);
	    return(ret);
	}
    }

    public static class NotificationSetting {
	public final String name, res;
	public final Path wav;

	private NotificationSetting(String name, String res, Path wav) {this.name = name; this.res = res; this.wav = wav;}
	public NotificationSetting(String name, String res) {this(name, res, null);}
	public NotificationSetting(String name, Path wav)   {this(name, null, wav);}
	public NotificationSetting(Path wav) {this(wav.getFileName().toString(), wav);}

	public boolean act(Setting conf) {
	    return(Utils.eq(conf.resns, this.res) && Utils.eq(conf.filens, wav));
	}

	public static final NotificationSetting nil = new NotificationSetting("None", null, null);
	public static final NotificationSetting other = new NotificationSetting("Select file...", null, null);
	public static final List<NotificationSetting> builtin;

	static {
	    List<NotificationSetting> buf = new ArrayList<>();
	    buf.add(new NotificationSetting("Bell 1", "sfx/hud/mmap/bell1"));
	    buf.add(new NotificationSetting("Bell 2", "sfx/hud/mmap/bell2"));
	    buf.add(new NotificationSetting("Bell 3", "sfx/hud/mmap/bell3"));
	    buf.add(new NotificationSetting("Wood 1", "sfx/hud/mmap/wood1"));
	    buf.add(new NotificationSetting("Wood 2", "sfx/hud/mmap/wood2"));
	    buf.add(new NotificationSetting("Wood 3", "sfx/hud/mmap/wood3"));
	    buf.add(new NotificationSetting("Wood 4", "sfx/hud/mmap/wood4"));
	    builtin = buf;
	}
    }

    public static class SettingsWindow extends Window {
	public final Settings conf;
	private final Runnable save;
	private final PackCont.LinPack cont;
	private final IconList list;
	private Widget setbox;
	private final CheckBox toggleAll;
	private GobIconCategoryList.GobCategory category = GobIconCategoryList.GobCategory.ALL;

	public static class Icon {
	    public final Setting conf;
	    public String name;
		public Text tname = null;

	    public Icon(Setting conf) {this.conf = conf;}

		private Tex img = null;
		public Tex img() {
			if(this.img == null) {
				this.img = tex(conf.res.loadsaved(Resource.remote()).layer(Resource.imgc).img);
			}
			return(this.img);
		}

		public static Tex tex(BufferedImage img) {
			Coord tsz;
			if(img.getWidth() > img.getHeight())
				tsz = new Coord(elh, (elh * img.getHeight()) / img.getWidth());
			else
				tsz = new Coord((elh * img.getWidth()) / img.getHeight(), elh);
			return(new TexI(PUtils.convolve(img, tsz, filter)));
		}

	}

	private <T> Consumer<T> andsave(Consumer<T> main) {
	    return(val -> {main.accept(val); if(save != null) save.run();});
	}

	private static final Text.Foundry elf = CharWnd.attrf;
	private static final int elh = elf.height() + UI.scale(2);
	public class IconList extends SSearchBox<Icon, IconList.IconLine> {
	    private List<Icon> ordered = Collections.emptyList();
		private List<Icon> categorized = Collections.emptyList();
	    private Map<String, Setting> cur = null;
	    private boolean reorder = false;

	    private IconList(Coord sz) {
		super(sz, elh);
	    }

	    public class IconLine extends SListWidget.ItemWidget<Icon> {
		public IconLine(Coord sz, Icon icon) {
		    super(IconList.this, sz, icon);
		    Widget prev;
		    prev = adda(new CheckBox("").state(() -> icon.conf.notify).set(andsave(val -> icon.conf.notify = val)).settip("Notify"),
				sz.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
			prev = adda(new CheckBox(""){
				@Override
				public void set(boolean val) {
					String iconTooltipName = icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t;
					if (Config.mandatoryAlwaysEnabledMapIcons.keySet().stream().anyMatch(iconTooltipName::equals)){
						icon.conf.show = true;
						ui.gui.error(Config.mandatoryAlwaysEnabledMapIcons.get(iconTooltipName));
					} else {
						icon.conf.show = val;
					}
					if(save != null)
						save.run();
					updateAllCheckbox();
				}}.state(() -> icon.conf.show).settip("Show icon on map"),
				prev.c.x - UI.scale(2) - (sz.y / 2), sz.y / 2, 0.5, 0.5);
		    add(SListWidget.IconText.of(Coord.of(prev.c.x - UI.scale(2), sz.y), () -> item.conf.res.loadsaved(Resource.remote())), Coord.z);
		}
	    }

	    protected boolean searchmatch(Icon icon, String text) {
		return((icon.name != null) &&
		       (icon.name.toLowerCase().indexOf(text.toLowerCase()) >= 0));
	    }
		protected List<Icon> allitems() {return(categorized);}
	    protected IconLine makeitem(Icon icon, int idx, Coord sz) {return(new IconLine(sz, icon));}

		public void tick(double dt) {
			Map<String, Setting> cur = this.cur;
			if(cur != conf.settings) {
				cur = conf.settings;
				ArrayList<Icon> ordered = new ArrayList<>(cur.size());
				for(Setting conf : cur.values())
					ordered.add(new Icon(conf));
				this.cur = cur;
				this.ordered = ordered;
				reorder = true;
			}
			if(reorder) {
				reorder = false;
				for(Icon icon : ordered) {
					if(icon.name == null) {
						try {
							Resource.Tooltip name = icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip);
							icon.name = (name == null) ? "???" : name.t;
						} catch(Loading l) {
							reorder = true;
						}
					}
				}
				Collections.sort(ordered, (a, b) -> {
					if((a.name == null) && (b.name == null))
						return(0);
					if(a.name == null)
						return(1);
					if(b.name == null)
						return(-1);
					return(a.name.compareTo(b.name));
				});
				categorized = list.ordered.stream()
						.filter(category::matches)
						.collect(Collectors.toList());
				research();
				updateAllCheckbox();
			}
			super.tick(dt);
		}

	    public boolean keydown(java.awt.event.KeyEvent ev) {
		if(ev.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
		    if(sel != null) {
			sel.conf.show = !sel.conf.show;
			if(save != null)
			    save.run();
		    }
		    return(true);
		}
		return(super.keydown(ev));
	    }

	    public void change(Icon icon) {
		super.change(icon);
		if(setbox != null) {
		    setbox.destroy();
		    setbox = null;
		}
		if(icon != null) {
		    setbox = cont.after(new IconSettings(sz.x - UI.scale(10), icon.conf), list, UI.scale(5));
		}
	    }
	}

	public class IconSettings extends Widget {
	    public final Setting conf;
	    public final NotifBox nb;

	    public IconSettings(int w, Setting conf) {
		super(Coord.z);
		this.conf = conf;
		Widget prev = add(new CheckBox("Show icon on map"){
					@Override
					public void set(boolean val) {
						String iconTooltipName = conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t;
						if (Config.mandatoryAlwaysEnabledMapIcons.keySet().stream().anyMatch(iconTooltipName::equals)){
							conf.show = true;
							ui.gui.error(Config.mandatoryAlwaysEnabledMapIcons.get(iconTooltipName));
						} else {
							conf.show = val;
						}
						if(save != null)
							save.run();
						updateAllCheckbox();
					}
				}.state(() -> conf.show),
				  0, 0);
		add(new CheckBox("Notify").state(() -> conf.notify).set(andsave(val -> conf.notify = val)),
		    w / 2, 0);
		Button pb = new Button(UI.scale(50), "Play") {
			protected void depress() {}
			protected void unpress() {}
			public void click() {play();}
		    };
		prev = add(new Label("Sound to play on notification:"), prev.pos("bl").adds(0, 5));
		nb = new NotifBox(w - pb.sz.x - UI.scale(15));
		addhl(prev.pos("bl").adds(0, 2), w, Frame.with(nb, false), pb);
		pack();
	    }

	    public class NotifBox extends Dropbox<NotificationSetting> {
		private final List<NotificationSetting> items = new ArrayList<>();

		public NotifBox(int w) {
		    super(w, 8, UI.scale(20));
		    items.add(NotificationSetting.nil);
		    for(NotificationSetting notif : NotificationSetting.builtin)
			items.add(notif);
		    if(conf.filens != null)
			items.add(new NotificationSetting(conf.filens));
		    items.add(NotificationSetting.other);
		    for(NotificationSetting item : items) {
			if(item.act(conf)) {
			    sel = item;
			    break;
			}
		    }
		}

		protected NotificationSetting listitem(int idx) {return(items.get(idx));}
		protected int listitems() {return(items.size());}

		protected void drawitem(GOut g, NotificationSetting item, int idx) {
//		    g.atext(item.name, Coord.of(0, g.sz().y / 2), 0.0, 0.5);
			g.aimage(Text.renderstroked(item.name).tex(), Coord.of(0, g.sz().y / 2), 0.0, 0.5);
		}

		private void selectwav() {
		    java.awt.EventQueue.invokeLater(() -> {
			    JFileChooser fc = new JFileChooser();
			    fc.setFileFilter(new FileNameExtensionFilter("PCM wave file", "wav"));
			    if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
				return;
			    for(Iterator<NotificationSetting> i = items.iterator(); i.hasNext();) {
				NotificationSetting item = i.next();
				if(item.wav != null)
				    i.remove();
			    }
			    NotificationSetting ws = new NotificationSetting(fc.getSelectedFile().toPath());
			    items.add(items.indexOf(NotificationSetting.other), ws);
			    change(ws);
			});
		}

		public void change(NotificationSetting item) {
		    super.change(item);
		    if(item == NotificationSetting.other) {
			selectwav();
		    } else {
			conf.resns = item.res;
			conf.filens = item.wav;
			if(save != null)
			    save.run();
		    }
		}
	    }

	    private void play() {
		NotificationSetting sel = nb.sel;
		if(sel == null) sel = NotificationSetting.nil;
		if(sel.res != null)
		    resnotif(sel.res).accept(ui);
		else if(sel.wav != null)
		    wavnotif(sel.wav).accept(ui);
	    }
	}

		private GobIconCategoryList iconCategories;
		private Dropbox iconPresetsDropbox;
		private String selectedPreset = null;
		private ArrayList<String> enabledIcons = null;
		private TextEntry newPresetName = null;
		Window confirmOverwriteWnd = null;

		public SettingsWindow(Settings conf, Runnable save) {
			super(Coord.z, "Map Icon Settings");
			this.conf = conf;
			this.save = save;
			PackCont.LinPack.VPack left = new PackCont.LinPack.VPack();
			PackCont.LinPack root;
			add(root = new PackCont.LinPack.HPack(), Coord.z).margin(UI.scale(5)).packpar(true);
			root.last(left, 0).margin(UI.scale(5)).packpar(true);
			root.last(new VRuler(UI.scale(500)), 0);
			root.last(this.cont = new PackCont.LinPack.VPack(), 0).margin(UI.scale(5)).packpar(true);
			toggleAll = cont.last(new CheckBox("Select All") {
				@Override
				public void changed(boolean val) {
					try {
						list.items().forEach(icon -> {
							// ND: First check if it's a mandatory icon, and keep it enabled at all times
							if (Config.mandatoryAlwaysEnabledMapIcons.keySet().stream().anyMatch(icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t::equals))
								icon.conf.show = true;
							else
								icon.conf.show = val;
						});
						if (save != null)
							save.run();
					} catch (Loading ignored){} // ND: It crashes if you click on "Select all" while some buttons are still loading. This should prevent it. Idk what are the side effects of this, nor do I care.
				}}, 0);
			list = cont.last(new IconList(UI.scale(280, 500)), 0);

			left.last(iconCategories = new GobIconCategoryList(UI.scale(190), 13, elh){
				@Override
				public void change(GobIconCategoryList.GobCategory item) {
					super.change(item);
					SettingsWindow.this.category = item;
					list.reorder = true;
				}
			}, 0).change(GobIconCategoryList.GobCategory.ALL);

			left.last(new HRuler(190), 0);
			left.last(new CheckBox("Notify new icon discovery") {
				{this.a = conf.notify;}

				public void changed(boolean val) {
					conf.notify = val;
					if(save != null)
						save.run();
				}
			}, UI.scale(5));
			left.last(new Label(">> Map Icon Presets <<"), UI.scale(36));
			Widget selectPresetLabel = left.last(new Label("Select Preset:"), UI.scale(0));
			iconPresetsDropbox = new Dropbox<String>(UI.scale(116), 10, UI.scale(17)) {
				{
					super.change(0);
					selectedPreset = "";
				}
				@Override
				protected String listitem(int i) {
					List<String> keys = new ArrayList<String>(mapIconPresets.keySet());
					if (keys.size() > 0)
						return keys.get(i);
					else return "";
				}
				@Override
				protected int listitems() {
					return mapIconPresets.keySet().size();
				}
				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
				}
				@Override
				public void change(String item) {
					super.change(item);
					selectedPreset = item;
				}
			};

			left.last(new Button(UI.scale(170), "Load Selected Preset", false).action(() -> {
				if (!selectedPreset.equals("")) {
					iconCategories.change(GobIconCategoryList.GobCategory.ALL);
					enabledIcons = new ArrayList<String>(mapIconPresets.get(selectedPreset));
					if (future != null)
						future.cancel(true);
					future = executor.scheduleWithFixedDelay(this::applyPreset, 200, 300, TimeUnit.MILLISECONDS);
					applyPreset();
				} else {
					ui.gui.error("Please select a preset to load!");
				}
			}), UI.scale(10));

			left.last(new Button(UI.scale(170), "Delete Selected Preset", false).action(() -> {
				if (!selectedPreset.equals("")) {
					mapIconPresets.remove(selectedPreset);
					ui.gui.msg(selectedPreset + " map icons preset has been deleted!");
					selectedPreset = "";
					iconPresetsDropbox.change(0);
					savePresetsToFile();
				} else {
					ui.gui.error("Please select a preset to delete!");
				}
			}), UI.scale(10));

			left.last(new Label(""), UI.scale(0));
			Widget newPresetWidget = left.last(new Label("New Preset:"), UI.scale(8));
			newPresetName = new TextEntry(UI.scale(120), ""){

			};
			left.last(new Button(UI.scale(170), "Save New Preset", false).action(() -> {
				if (newPresetName.text().equals(""))
					ui.gui.error("Please set a name for the new map icons preset!");
				else if (newPresetName.text().trim().length() == 0)
					ui.gui.error("Brother don't just use a bunch of spaces as the preset name, that's stupid. Give it a nice name.");
				else if (mapIconPresets.keySet().stream().anyMatch(newPresetName.text()::equals)) {
//					ui.gui.error("A preset named " + "\"" + newPresetName.text() + "\"" + " already exists. Please choose a different name, or delete the old one.");
					SettingsWindow thisSettingsWindow = this;
					if (confirmOverwriteWnd != null){
						confirmOverwriteWnd.remove();
						confirmOverwriteWnd = null;
					}
					confirmOverwriteWnd = new Window(UI.scale(new Coord(235, 90)), "Preset already exists!") {
						{
							Widget prev;
							add(new Label("Are you sure you want to overwrite the preset?"), UI.scale(new Coord(10, 10)));
							prev = add(new Label("Preset Name: "), UI.scale(new Coord(10, 30)));
							add(new Label(newPresetName.text()), prev.pos("ur").adds(10, 0)).setcolor(new Color(255, 205, 0,255));

							Button add = new Button(UI.scale(90), "Overwrite") {
								@Override
								public void click() {
									iconCategories.change(GobIconCategoryList.GobCategory.ALL);

									if (future != null)
										future.cancel(true);
									future = executor.scheduleWithFixedDelay(thisSettingsWindow::savePreset, 200, 300, TimeUnit.MILLISECONDS);

									parent.reqdestroy();
								}
							};
							add(add, UI.scale(new Coord(15, 60)));

							Button cancel = new Button(UI.scale(90), "No! Cancel!") {
								@Override
								public void click() {
									parent.reqdestroy();
								}
							};
							add(cancel, UI.scale(new Coord(130, 60)));
						}

						@Override
						public void wdgmsg(Widget sender, String msg, Object... args) {
							if (msg.equals("close"))
								reqdestroy();
							else
								super.wdgmsg(sender, msg, args);
						}

					};
					ui.gui.add(confirmOverwriteWnd, new Coord((ui.gui.sz.x - confirmOverwriteWnd.sz.x) / 2, (ui.gui.sz.y - confirmOverwriteWnd.sz.y*3) / 2));
					confirmOverwriteWnd.show();
				} else {
					iconCategories.change(GobIconCategoryList.GobCategory.ALL);

					if (future != null)
						future.cancel(true);
					future = executor.scheduleWithFixedDelay(this::savePreset, 200, 300, TimeUnit.MILLISECONDS);
				}
			}), UI.scale(10));

			cont.pack();
			left.pack();
			left.add(iconPresetsDropbox, selectPresetLabel.pos("ur").adds(3, 2));
			left.add(newPresetName, newPresetWidget.pos("ur").adds(4, -1));
			root.pack();
			updateAllCheckbox();
		}

		private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		private Future<?> future;
		private void updateAllCheckbox() {
			if(toggleAll == null) {
				return;
			}
			List<? extends Icon> items = list != null ? list.items() : null;
			toggleAll.a = items != null
					&& !items.isEmpty()
					&& items.stream().allMatch(icon -> icon.conf.show);
		}

		private void applyPreset(){
			if (!list.reorder){
				future.cancel(true);
				list.items().forEach(icon -> {
					if (Config.mandatoryAlwaysEnabledMapIcons.keySet().stream().anyMatch(icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t::equals))
						icon.conf.show = true;
					if (enabledIcons.stream().anyMatch(icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t::equals))
						icon.conf.show = true;
					else
						icon.conf.show = false;
				});
				if (save != null)
					save.run();
				ui.gui.msg(selectedPreset + " map icons preset has been set!");
			}
		}

		private void savePreset(){
			if (!list.reorder){
				future.cancel(true);
				String presetName = newPresetName.text();
				mapIconPresets.put(presetName, new ArrayList<String>() {{
					list.items().forEach(icon -> {
						if (icon.conf.show) {
							add(icon.conf.res.loadsaved(Resource.remote()).layer(Resource.tooltip).t);
						}
					});
				}});
				ui.gui.msg(presetName + " map icons preset has been saved!");
				newPresetName.settext("");
				savePresetsToFile();
			}
		}

	}


    @OCache.DeltaType(OCache.OD_ICON)
    public static class $icon implements OCache.Delta {
	public void apply(Gob g, OCache.AttrDelta msg) {
	    int resid = msg.uint16();
	    Indir<Resource> res;
	    if(resid == 65535) {
		g.delattr(GobIcon.class);
	    } else {
		int ifl = msg.uint8();
		g.setattr(new GobIcon(g, OCache.Delta.getres(g, resid)));
	    }
	}
    }

	public static void initPresets() {
		load();
	}

	public static void load() {
		mapIconPresets.clear();
		File config = new File("Map_Icons_Presets/yourSavedPresets");
		if (!config.exists()) {
			defaultPresets();
		} else {
			loadPresetsFromFile(config);
		}
	}
	private static void loadPresetsFromFile(File config) {
		try {
			mapIconPresets.put("", null);
			for (String s : Files.readAllLines(Paths.get(config.toURI()), StandardCharsets.UTF_8)) {
				String[] split = s.split("(;)");
				if (!mapIconPresets.containsKey(split[0])) {
					mapIconPresets.put(split[0], new ArrayList<String>() {{
						for (int x = 1; x < split.length; x++) {
							add(split[x]);
						}
					}});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void defaultPresets() {
		mapIconPresets.clear();
		loadPresetsFromFile(new File("Map_Icons_Presets/defaultPresets"));
	}

	public static void savePresetsToFile() {
		try {
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(new File("Map_Icons_Presets/yourSavedPresets").toURI()), StandardCharsets.UTF_8);
			for (int x = 1; x < mapIconPresets.keySet().size(); x++) { // ND: Start at 1, cause 0 is always an empty string added in the code when settings are loaded
				String presetName = ((String) mapIconPresets.keySet().toArray()[x]);
				StringBuilder enabledIcons = new StringBuilder();
				for (String icon : mapIconPresets.get(presetName)){
					enabledIcons.append(";").append(icon);
				}
				bw.write(presetName + enabledIcons + "\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
