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

import java.awt.*;
import java.util.*;
import java.awt.event.KeyEvent;
import java.util.List;

import static haven.CharWnd.attrf;

public class LoginScreen extends Widget {
    public static final Text.Foundry
	textf = new Text.Foundry(Text.sans, 18).aa(true),//ND: Increased size from 16 to 18
	textfs = new Text.Foundry(Text.sans, 15).aa(true);//ND: Increased size from 14 to 15
    public static final Tex bg = Resource.loadtex("gfx/loginscr");
	public static final Position bgc = new Position(UI.scale(533, 250));// ND: This affects only the login screen username/password location.
    public final Credbox login;
    public final String hostname;
    private Text error, progress;
    private Button optbtn;
    private OptWnd opts = new OptWnd(false);
	AccountList accounts;
	private Window updateWindow;
	private static boolean updateWindowShown = false;

    private String getpref(String name, String def) {
	return(Utils.getpref(name + "@" + hostname, def));
    }

    public LoginScreen(String hostname) {
	super(bg.sz());
	this.hostname = hostname;
	setfocustab(true);
	add(new Img(bg), Coord.z);
	optbtn = adda(new Button(UI.scale(100), "Options"), pos("cbl").add(10, -10), 0, 1);
	optbtn.setgkey(GameUI.kb_opt);
	adda(login = new Credbox(), bgc.adds(0, 10), 0.5, 0.0).hide();
	accounts = add(new AccountList(10));
	adda(new StatusLabel(hostname, 0.5), bgc.x, bg.sz().y, 0.5, 1.4); // ND: This adds the server status and player count.
	GameUI.swimon = false;
	GameUI.trackon = false;
	GameUI.crimeon = false;
	MenuGrid.toggleStuff = true;
	Gob.alarmPlayed.clear();
	Gob.batsLeaveMeAlone = false;
	Gob.batsFearMe = false;
	updateWindow = new Window(Coord.z, "Update Available!", true) {
		{
			Widget prev;
			prev = add(new Label("A new client version is available!"), UI.scale(new Coord(74, 3)));
			prev = add(new Label("Please remember to update your client to avoid bugs & crashes."), prev.pos("bl").adds(0, 8).x(0));
			Button close = new Button(UI.scale(120), "Close", false) {
				@Override
				public void click() {
					parent.reqdestroy();
				}
			};
			add(close, prev.pos("bl").adds(0, 10).adds(92, 2));
			pack();
		}

		@Override
		public void drag(Coord off) {
			// ND: Don't do anything
		}
		@Override
		public void wdgmsg(Widget sender, String msg, Object... args) {
			if (msg.equals("close"))
				reqdestroy();
			else
				super.wdgmsg(sender, msg, args);
		}
	};
    }

    //public static final KeyBinding kb_savtoken = KeyBinding.get("login/savtoken", KeyMatch.forchar('R', KeyMatch.M)); // ND: Why the fuck are there keybinds for these? Someone might press one of those by mistake.
    //public static final KeyBinding kb_deltoken = KeyBinding.get("login/deltoken", KeyMatch.forchar('F', KeyMatch.M)); // ND: No drink button keybind, BUT OH BOY WE COULD REALLY USE A REMEMBER/FORGET ACCOUNT KEYBIND!
    public class Credbox extends Widget {
	public final UserEntry user;
	private final TextEntry pass;
	private final CheckBox saveaccount;
	private final Button fbtn;
	private final IButton exec;
	private final Widget pwbox, tkbox;
	private byte[] token = null;
	private boolean inited = false;

	public class UserEntry extends TextEntry {
	    private final List<String> history = new ArrayList<>();
	    private int hpos = -1;
	    private String hcurrent;

	    private UserEntry(int w) {
		super(w, "");
		history.addAll(Utils.getprefsl("saved-tokens@" + hostname, new String[] {}));
	    }

	    protected void changed() {
//		checktoken();
		//savetoken.set(token != null); ND: commented this, so the "remember me" doesn't untick whenever you write inside the username input.
	    }

	    public void settext2(String text) {
		rsettext(text);
		changed();
	    }

	    public boolean keydown(KeyEvent ev) {
		if(ConsoleHost.kb_histprev.key().match(ev)) {
		    if(hpos < history.size() - 1) {
			if(hpos < 0)
			    hcurrent = text();
			settext2(history.get(++hpos));
		    }
		} else if(ConsoleHost.kb_histnext.key().match(ev)) {
		    if(hpos >= 0) {
			if(--hpos < 0)
			    settext2(hcurrent);
			else
			    settext2(history.get(hpos));
		    }
		} else {
		    return(super.keydown(ev));
		}
		return(true);
	    }

	    public void init(String name) {
		history.remove(name);
		settext2(name);
	    }
	}

	private Credbox() {
	    super(UI.scale(200, 150));
	    setfocustab(true);
	    //Widget prev = add(new Label("User name", textf), 0, 0); //ND: replaced this with stroked text
		Widget prev = add(new Label("Username", textf){{setstroked(Color.BLACK);}}, 0, 0);
	    add(user = new UserEntry(this.sz.x), prev.pos("bl").adds(0, 1));
	    setfocus(user);

	    add(pwbox = new Widget(Coord.z), user.pos("bl").adds(0, 10));
	    //pwbox.add(prev = new Label("Password", textf), Coord.z); //ND: replaced this with stroked text
		pwbox.add(prev = new Label("Password", textf){{setstroked(Color.BLACK);}}, Coord.z);
	    pwbox.add(pass = new TextEntry(this.sz.x, ""), prev.pos("bl").adds(0, 1)).pw = true;
	    pwbox.add(saveaccount = new CheckBox("Save Account", true), pass.pos("bl").adds(0, 10));
		saveaccount.set(true); //ND: Set this to true from the beginning. If they don't want to save, untick it.
	    //savetoken.setgkey(kb_savtoken); //ND: Stupid keybind.
//	    saveaccount.settip("Saving an account does not save your password, but rather " +
//			     "a randomly generated token that will be used to log in. " +
//			     "You can manage your saved tokens in your Account Settings.",
//			     true);
	    pwbox.pack();
	    pwbox.hide();

	    add(tkbox = new Widget(new Coord(this.sz.x, 0)), user.pos("bl").adds(0, 10));
	    //tkbox.add(prev = new Label("Login saved", textfs), UI.scale(0, 25)); ND: replaced this with stroked text
		tkbox.add(prev = new Label("Login saved", textfs){{setstroked(Color.BLACK);}}, UI.scale(0, 25));
	    tkbox.adda(fbtn = new Button(UI.scale(100), "Forget me"), prev.pos("mid").x(this.sz.x), 1.0, 0.5).action(() -> {
			forget();
			if (accounts.getAccountFromName(user.text()) != null) {
				accounts.remove(accounts.getAccountFromName(user.text()));
			}
			user.rsettext("");
		});
	    //fbtn.setgkey(kb_deltoken); //ND: Stupider keybind.
	    tkbox.pack();
	    tkbox.hide();

	    adda(exec = new IButton("gfx/hud/buttons/login", "u", "d", "o") {
		    protected void depress() {ui.sfx(Button.clbtdown.stream());}
		    protected void unpress() {ui.sfx(Button.clbtup.stream());}
		    public void click() {enter();}
		},
		pos("cmid").y(Math.max(pwbox.pos("bl").y, tkbox.pos("bl").y)).adds(0, 35), 0.5, 0.0);
	    pack();
	}

	private void init() {
	    if(inited)
		return;
	    inited = true;
		//user.init(getpref("loginname", "")); // ND: This line sets the user text if the "remember me" is checked. I don't want that, since we have the accounts on the left side.
											   // This way, if a new account needs to be added, you don't need to clear the box.
	}

	private void checktoken() {
	    if(this.token != null) {
		Arrays.fill(this.token, (byte)0);
		this.token = null;
	    }
	    byte[] token = Bootstrap.gettoken(user.text(), hostname);
	    if(token == null) {
		tkbox.hide();
		pwbox.show();
	    } else {
		tkbox.show();
		pwbox.hide();
		this.token = token;
	    }
	}

	private void forget() {
	    String nm = user.text();
	    Bootstrap.settoken(nm, hostname, null);
	    //savetoken.set(false); //ND: commented this, so the "remember me" doesn't untick whenever you click on "forget me".
	    checktoken();
	}

	private void enter() {
	    if(user.text().equals("")) {
		setfocus(user);
	    } else if(pwbox.visible && pass.text().equals("")) {
		setfocus(pass);
	    } else {
		if(saveaccount.state()) {
			AccountList.storeAccount(user.text(), pass.text());
		}
		LoginScreen.this.wdgmsg("login", creds(), pwbox.visible && saveaccount.state());
	    }
	}

	private void enter2() {
		if(user.text().equals("")) {
			setfocus(user);
		} else if(pwbox.visible && pass.text().equals("")) {
			setfocus(pass);
		} else {
			LoginScreen.this.wdgmsg("login", creds(), pwbox.visible && saveaccount.state());
		}
	}

	private AuthClient.Credentials creds() {
	    byte[] token = this.token;
	    AuthClient.Credentials ret;
	    if(token != null) {
		ret = new AuthClient.TokenCred(user.text(), Arrays.copyOf(token, token.length));
	    } else {
		String pw = pass.text();
		ret = null;
		parse: if(pw.length() == 64) {
		    byte[] ptok;
		    try {
			ptok = Utils.hex2byte(pw);
		    } catch(IllegalArgumentException e) {
			break parse;
		    }
		    ret = new AuthClient.TokenCred(user.text(), ptok);
		}
		if(ret == null)
		    ret = new AuthClient.NativeCred(user.text(), pw);
		pass.rsettext("");
	    }
	    return(ret);
	}

	public boolean keydown(KeyEvent ev) {
	    if(key_act.match(ev)) {
		enter();
		return(true);
	    }
	    return(super.keydown(ev));
	}

	public void show() {
	    if(!inited)
		init();
	    super.show();
	    checktoken();
	    if(pwbox.visible && !user.text().equals(""))
		setfocus(pass);
	}
    }

    public static class StatusLabel extends Widget {
	public final HttpStatus stat;
	public final double ax;

	public StatusLabel(String host, double ax) {
	    super(new Coord(UI.scale(150), FastText.h * 2));
	    this.stat = new HttpStatus(host);
	    this.ax = ax;
	}

	public void draw(GOut g) {
	    int x = (int)Math.round(sz.x * ax);
	    synchronized(stat) {
		if(!stat.syn || (stat.status == ""))
		    return;
		if(stat.status == "up") {
		    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Online");
		    FastText.aprintf(g, new Coord(x, FastText.h * 1), ax, 0, "Hearthlings connected: %,d", stat.users);
		} else if(stat.status == "down") {
		    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Offline");
		} else if(stat.status == "shutdown") {
		    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: Shutting down");
		} else if(stat.status == "crashed") {
		    FastText.aprintf(g, new Coord(x, FastText.h * 0), ax, 0, "Server status: It crashed bro");
		}
	    }
	}

	protected void added() {
	    stat.start();
	}

	public void dispose() {
	    stat.quit();
	}
    }

    private void mklogin() {
	login.show();
	progress(null);
    }

    private void error(String error) {
	if(this.error != null)
	    this.error = null;
	if(error != null)
	    this.error = textf.render(error, java.awt.Color.RED);
    }

    private void progress(String p) {
	if(progress != null)
	    progress = null;
	if(p != null)
	    progress = textf.render(p, java.awt.Color.WHITE);
    }

    private void clear() {
	login.hide();
	progress(null);
    }

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if(sender == accounts) {
			if("account".equals(msg)) {
				String name = (String) args[0];
				String pass = (String) args[1];
				login.user.settext2(name);
				login.pass.settext(pass);
				login.enter2();
			}
			return;
		}
		if(sender == optbtn) {
			if (!opts.attached)
				ui.root.adda(opts, 0.5, 0.5);
			else
				opts.show(!opts.visible());
			return;
		} else if(sender == opts) {
			// ND: Pretty sure this part never happens, ever
			opts.show(!opts.visible());
		}
		super.wdgmsg(sender, msg, args);
	}

	public void tick(double dt){
		if (!Config.ClientVersion.equals(Config.webClientVersion) && !updateWindowShown) {
			adda(updateWindow, 0.5, 0);
			updateWindowShown = true;
		}
		super.tick(dt);
	}

    public void cdestroy(Widget ch) {
	if(ch == opts) {
	    opts = null;
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "login") {
	    mklogin();
	} else if(msg == "error") {
	    error((String)args[0]);
	} else if(msg == "prg") {
	    error(null);
	    clear();
	    progress((String)args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void presize() {
	c = parent.sz.div(2).sub(sz.div(2));
    }

    protected void added() {
	presize();
	parent.setfocus(this);
    }

    public void draw(GOut g) {
	super.draw(g);
	if(error != null)
	    g.aimage(PUtils.strokeTex(error), bgc.adds(0, -20), 0.5, 0.0);
	if(progress != null)
	    g.aimage(PUtils.strokeTex(progress), bgc.adds(0, 50), 0.5, 0.0);
    }
}
