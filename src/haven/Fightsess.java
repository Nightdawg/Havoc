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
import haven.sprites.CurrentTargetSprite;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class Fightsess extends Widget {
	public static final Text.Foundry keybindsFoundry = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 14);
    private static final Coord off = new Coord(UI.scale(32), UI.scale(32));
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(off)).div(2);
    public static final int actpitch = UI.scale(52);
	public static final Text.Foundry ipAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 14);
	public static final Text.Foundry openingAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 10);
	public static final Text.Foundry cleaveAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 10);
	public static HashSet<String> maneuvers =  new HashSet<>(Arrays.asList(
			"paginae/atk/toarms", "paginae/atk/shield", "paginae/atk/parry",
			"paginae/atk/oakstance", "paginae/atk/dorg", "paginae/atk/chinup",
			"paginae/atk/bloodlust", "paginae/atk/combmed"));
	public static HashMap<String, Long> nonAttackDefences = new HashMap<String, Long>()
	{{
		put("paginae/atk/regain", 2100L);
		put("paginae/atk/zigzag", 3000L);
		put("paginae/atk/yieldground", 1800L);
		put("paginae/atk/sidestep", 1500L);
		put("paginae/atk/qdodge", 1500L);
		put("paginae/atk/jump", 1500L);
		put("paginae/atk/artevade", 2400L);
		put("paginae/atk/dash", 3200L); // ND: Assuming 5/5 cards are used, this is the minimum cooldown. Nobody uses dash, but just to be safe, I added it here.

		// ND: Every other "defense" move is actually an attack, which is affected by agility, so we don't add them here.
	}};


	// ND: All of these stupid values need to be hardcoded this way. There are ranges between the cooldown is set.
	// The server doesn't send you a decimal cooldown, and each move and melee attack seems to have absolutely no pattern for those ranges.
	// What, you're using a b12? Well 125% attack speed means there are more cooldown ranges too, cause fuck you lmao. Same for cutblade.
	// I'm not bothering with the pickaxe. What moron would bring a pickaxe to combat anyway?
	public static HashMap<String, Double> meleeAttackMoves = new HashMap<String, Double>()
	{{
		put("paginae/atk/cleave", 80D);
		put("paginae/atk/chop", 40D);
//		put("paginae/atk/fullcircle", 40D); // ND: Full Circle attacks multiple targets.
		put("paginae/atk/barrage", 20D);
		put("paginae/atk/ravenbite", 40D);
		put("paginae/atk/sideswipe", 25D);
		put("paginae/atk/sting", 50D);
//		put("paginae/atk/sos", 50D); // ND: Storm of Swords attacks multiple targets.
	}};

	public static HashMap<String, Double> unarmedAttackMoves = new HashMap<String, Double>()
	{{
		put("paginae/atk/flex", 30D);
		put("paginae/atk/gojug", 40D); // ND: Jugular tooltip says cooldown is 45, but it's actually 40. SMH.
		put("paginae/atk/haymaker", 50D);
		put("paginae/atk/kick", 45D);
		put("paginae/atk/knockteeth", 35D);
		put("paginae/atk/lefthook", 40D);
		put("paginae/atk/lowblow", 50D);
		put("paginae/atk/oppknock", 45D);
		put("paginae/atk/pow", 30D);
//		put("paginae/atk/punchboth", 40D); // ND: Punch 'em Both attacks 2 targets.
		put("paginae/atk/ripapart", 60D);
		put("paginae/atk/stealthunder", 40D);
		put("paginae/atk/takedown", 50D);
		put("paginae/atk/uppercut", 30D);
	}};
	public static HashMap<Double, HashMap<Double, ArrayList<Double>>> attackCooldownNumbers = new HashMap<Double, HashMap<Double, ArrayList<Double>>>(){{
		put(20D, new HashMap<Double, ArrayList<Double>>(){{
			put (18D, new ArrayList<Double>(){{add(0D);add(0.579D);}});
			put (19D, new ArrayList<Double>(){{add(0.580D);add(0.837D);}});
			put (20D, new ArrayList<Double>(){{add(0.838D);add(1.188D);}});
			put (21D, new ArrayList<Double>(){{add(1.189D);add(1.659D);}});
			put (22D, new ArrayList<Double>(){{add(1.660D);add(2D);}});
		}});
		put(25D, new HashMap<Double, ArrayList<Double>>(){{
			put (23D, new ArrayList<Double>(){{add(0D);add(0.648D);}});
			put (24D, new ArrayList<Double>(){{add(0.649D);add(0.868D);}});
			put (25D, new ArrayList<Double>(){{add(0.869D);add(1.148D);}});
			put (26D, new ArrayList<Double>(){{add(1.149D);add(1.503D);}});
			put (27D, new ArrayList<Double>(){{add(1.504D);add(1.948D);}});
			put (28D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put(30D, new HashMap<Double, ArrayList<Double>>(){{
			put (27D, new ArrayList<Double>(){{add(0D);add(0.543D);}});
			put (28D, new ArrayList<Double>(){{add(0.544D);add(0.698D);}});
			put (29D, new ArrayList<Double>(){{add(0.699D);add(0.889D);}});
			put (30D, new ArrayList<Double>(){{add(0.890D);add(1.122D);}});
			put (31D, new ArrayList<Double>(){{add(1.123D);add(1.407D);}});
			put (32D, new ArrayList<Double>(){{add(1.408D);add(1.751D);}});
			put (33D, new ArrayList<Double>(){{add(1.752D);add(2D);}});
		}});
		put(35D, new HashMap<Double, ArrayList<Double>>(){{
			put (32D, new ArrayList<Double>(){{add(0D);add(0.595D);}});
			put (33D, new ArrayList<Double>(){{add(0.596D);add(0.735D);}});
			put (34D, new ArrayList<Double>(){{add(0.736D);add(0.904D);}});
			put (35D, new ArrayList<Double>(){{add(0.905D);add(1.104D);}});
			put (36D, new ArrayList<Double>(){{add(1.105D);add(1.341D);}});
			put (37D, new ArrayList<Double>(){{add(1.342D);add(1.620D);}});
			put (38D, new ArrayList<Double>(){{add(1.621D);add(1.948D);}});
			put (39D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put(40D, new HashMap<Double, ArrayList<Double>>(){{
			put (36D, new ArrayList<Double>(){{add(0D);add(0.526D);}});
			put (37D, new ArrayList<Double>(){{add(0.527D);add(0.636D);}});
			put (38D, new ArrayList<Double>(){{add(0.637D);add(0.765D);}});
			put (39D, new ArrayList<Double>(){{add(0.766D);add(0.915D);}});
			put (40D, new ArrayList<Double>(){{add(0.916D);add(1.090D);}});
			put (41D, new ArrayList<Double>(){{add(1.091D);add(1.293D);}});
			put (42D, new ArrayList<Double>(){{add(1.294D);add(1.528D);}});
			put (43D, new ArrayList<Double>(){{add(1.529D);add(1.798D);}});
			put (44D, new ArrayList<Double>(){{add(1.799D);add(2D);}});
		}});
		put(45D, new HashMap<Double, ArrayList<Double>>(){{
			put (41D, new ArrayList<Double>(){{add(0D);add(0.567D);}});
			put (42D, new ArrayList<Double>(){{add(0.568D);add(0.670D);}});
			put (43D, new ArrayList<Double>(){{add(0.671D);add(0.788D);}});
			put (44D, new ArrayList<Double>(){{add(0.789D);add(0.924D);}});
			put (45D, new ArrayList<Double>(){{add(0.925D);add(1.080D);}});
			put (46D, new ArrayList<Double>(){{add(1.081D);add(1.258D);}});
			put (47D, new ArrayList<Double>(){{add(1.259D);add(1.460D);}});
			put (48D, new ArrayList<Double>(){{add(1.461D);add(1.689D);}});
			put (49D, new ArrayList<Double>(){{add(1.690D);add(1.948D);}});
			put (50D, new ArrayList<Double>(){{add(1.949D);add(2D);}});

		}});
		put(50D, new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
		put(60D, new HashMap<Double, ArrayList<Double>>(){{
			put (54D, new ArrayList<Double>(){{add(0D);add(0.510D);}});
			put (55D, new ArrayList<Double>(){{add(0.511D);add(0.579D);}});
			put (56D, new ArrayList<Double>(){{add(0.580D);add(0.656D);}});
			put (57D, new ArrayList<Double>(){{add(0.657D);add(0.742D);}});
			put (58D, new ArrayList<Double>(){{add(0.743D);add(0.837D);}});
			put (59D, new ArrayList<Double>(){{add(0.838D);add(0.943D);}});
			put (60D, new ArrayList<Double>(){{add(0.944D);add(1.059D);}});
			put (61D, new ArrayList<Double>(){{add(1.060D);add(1.188D);}});
			put (62D, new ArrayList<Double>(){{add(1.189D);add(1.330D);}});
			put (63D, new ArrayList<Double>(){{add(1.331D);add(1.487D);}});
			put (64D, new ArrayList<Double>(){{add(1.488D);add(1.659D);}});
			put (65D, new ArrayList<Double>(){{add(1.660D);add(1.847D);}});
			put (66D, new ArrayList<Double>(){{add(1.848D);add(2D);}});
		}});
		put(80D, new HashMap<Double, ArrayList<Double>>(){{
			put (72D, new ArrayList<Double>(){{add(0D);add(0.502D);}});
			put (73D, new ArrayList<Double>(){{add(0.503D);add(0.552D);}});
			put (74D, new ArrayList<Double>(){{add(0.553D);add(0.607D);}});
			put (75D, new ArrayList<Double>(){{add(0.608D);add(0.666D);}});
			put (76D, new ArrayList<Double>(){{add(0.667D);add(0.731D);}});
			put (77D, new ArrayList<Double>(){{add(0.732D);add(0.800D);}});
			put (78D, new ArrayList<Double>(){{add(0.801D);add(0.875D);}});
			put (79D, new ArrayList<Double>(){{add(0.876D);add(0.957D);}});
			put (80D, new ArrayList<Double>(){{add(0.958D);add(1.044D);}});
			put (81D, new ArrayList<Double>(){{add(1.045D);add(1.138D);}});
			put (82D, new ArrayList<Double>(){{add(1.139D);add(1.240D);}});
			put (83D, new ArrayList<Double>(){{add(1.241D);add(1.349D);}});
			put (84D, new ArrayList<Double>(){{add(1.350D);add(1.466D);}});
			put (85D, new ArrayList<Double>(){{add(1.467D);add(1.592D);}});
			put (86D, new ArrayList<Double>(){{add(1.593D);add(1.727D);}});
			put (87D, new ArrayList<Double>(){{add(1.728D);add(1.872D);}});
			put (88D, new ArrayList<Double>(){{add(1.873D);add(2D);}});
		}});
	}};

	public static HashMap<String, HashMap<Double, ArrayList<Double>>> b12AttackCooldownNumbers = new HashMap<String, HashMap<Double, ArrayList<Double>>>(){{
		put("paginae/atk/barrage", new HashMap<Double, ArrayList<Double>>(){{
			put (23D, new ArrayList<Double>(){{add(0D);add(0.648D);}});
			put (24D, new ArrayList<Double>(){{add(0.649D);add(0.868D);}});
			put (25D, new ArrayList<Double>(){{add(0.869D);add(1.148D);}});
			put (26D, new ArrayList<Double>(){{add(1.149D);add(1.503D);}});
			put (27D, new ArrayList<Double>(){{add(1.504D);add(1.948D);}});
			put (28D, new ArrayList<Double>(){{add(1.949D);add(2D);}});
		}});
		put("paginae/atk/sideswipe", new HashMap<Double, ArrayList<Double>>(){{
			put (28D, new ArrayList<Double>(){{add(0D);add(0.524D);}});
			put (29D, new ArrayList<Double>(){{add(0.525D);add(0.668D);}});
			put (30D, new ArrayList<Double>(){{add(0.669D);add(0.843D);}});
			put (31D, new ArrayList<Double>(){{add(0.844D);add(1.057D);}});
			put (32D, new ArrayList<Double>(){{add(1.058D);add(1.315D);}});
			put (33D, new ArrayList<Double>(){{add(1.316D);add(1.626D);}});
			put (34D, new ArrayList<Double>(){{add(1.627D);add(1.998D);}});
			put (35D, new ArrayList<Double>(){{add(1.999D);add(2D);}});
		}});
		put("paginae/atk/chop", new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
		put("paginae/atk/ravenbite", new HashMap<Double, ArrayList<Double>>(){{
			put (45D, new ArrayList<Double>(){{add(0D);add(0.516D);}});
			put (46D, new ArrayList<Double>(){{add(0.517D);add(0.601D);}});
			put (47D, new ArrayList<Double>(){{add(0.602D);add(0.698D);}});
			put (48D, new ArrayList<Double>(){{add(0.699D);add(0.807D);}});
			put (49D, new ArrayList<Double>(){{add(0.808D);add(0.932D);}});
			put (50D, new ArrayList<Double>(){{add(0.933D);add(1.072D);}});
			put (51D, new ArrayList<Double>(){{add(1.073D);add(1.229D);}});
			put (52D, new ArrayList<Double>(){{add(1.230D);add(1.407D);}});
			put (53D, new ArrayList<Double>(){{add(1.408D);add(1.605D);}});
			put (54D, new ArrayList<Double>(){{add(1.606D);add(1.828D);}});
			put (55D, new ArrayList<Double>(){{add(1.829D);add(2D);}});
		}});
//		put("paginae/atk/sting", new HashMap<Double, ArrayList<Double>>(){{}}); // B12 can't sting
		put("paginae/atk/cleave", new HashMap<Double, ArrayList<Double>>(){{
			put (91D, new ArrayList<Double>(){{add(0D);add(0.536D);}});
			put (92D, new ArrayList<Double>(){{add(0.537D);add(0.579D);}});
			put (93D, new ArrayList<Double>(){{add(0.580D);add(0.624D);}});
			put (94D, new ArrayList<Double>(){{add(0.625D);add(0.673D);}});
			put (95D, new ArrayList<Double>(){{add(0.674D);add(0.724D);}});
			put (96D, new ArrayList<Double>(){{add(0.725D);add(0.779D);}});
			put (97D, new ArrayList<Double>(){{add(0.780D);add(0.837D);}});
			put (98D, new ArrayList<Double>(){{add(0.838D);add(0.899D);}});
			put (99D, new ArrayList<Double>(){{add(0.900D);add(0.965D);}});
			put (100D, new ArrayList<Double>(){{add(0.966D);add(1.035D);}});
			put (101D, new ArrayList<Double>(){{add(1.036D);add(1.109D);}});
			put (102D, new ArrayList<Double>(){{add(1.110D);add(1.188D);}});
			put (103D, new ArrayList<Double>(){{add(1.189D);add(1.272D);}});
			put (104D, new ArrayList<Double>(){{add(1.273D);add(1.360D);}});
			put (105D, new ArrayList<Double>(){{add(1.361D);add(1.454D);}});
			put (106D, new ArrayList<Double>(){{add(1.455D);add(1.553D);}});
			put (107D, new ArrayList<Double>(){{add(1.554D);add(1.659D);}});
			put (108D, new ArrayList<Double>(){{add(1.660D);add(1.770D);}});
			put (109D, new ArrayList<Double>(){{add(1.771D);add(1.887D);}});
			put (110D, new ArrayList<Double>(){{add(1.888D);add(2D);}});
		}});
	}};

	public static HashMap<String, HashMap<Double, ArrayList<Double>>> cutbladeAttackCooldownNumbers = new HashMap<String, HashMap<Double, ArrayList<Double>>>(){{
		put("paginae/atk/barrage", new HashMap<Double, ArrayList<Double>>(){{
			put (22D, new ArrayList<Double>(){{add(0D);add(0.636D);}});
			put (23D, new ArrayList<Double>(){{add(0.637D);add(0.862D);}});
			put (24D, new ArrayList<Double>(){{add(0.863D);add(1.155D);}});
			put (25D, new ArrayList<Double>(){{add(1.156D);add(1.528D);}});
			put (26D, new ArrayList<Double>(){{add(1.529D);add(2D);}});

		}}); // barrage
		put("paginae/atk/sideswipe", new HashMap<Double, ArrayList<Double>>(){{
			put (27D, new ArrayList<Double>(){{add(0D);add(0.543D);}});
			put (28D, new ArrayList<Double>(){{add(0.544D);add(0.698D);}});
			put (29D, new ArrayList<Double>(){{add(0.699D);add(0.889D);}});
			put (30D, new ArrayList<Double>(){{add(0.890D);add(1.122D);}});
			put (31D, new ArrayList<Double>(){{add(1.123D);add(1.407D);}});
			put (32D, new ArrayList<Double>(){{add(1.408D);add(1.751D);}});
			put (33D, new ArrayList<Double>(){{add(1.752D);add(2D);}});
		}});
		put("paginae/atk/chop", new HashMap<Double, ArrayList<Double>>(){{
			put (43D, new ArrayList<Double>(){{add(0D);add(0.502D);}});
			put (44D, new ArrayList<Double>(){{add(0.503D);add(0.588D);}});
			put (45D, new ArrayList<Double>(){{add(0.589D);add(0.687D);}});
			put (46D, new ArrayList<Double>(){{add(0.688D);add(0.800D);}});
			put (47D, new ArrayList<Double>(){{add(0.801D);add(0.929D);}});
			put (48D, new ArrayList<Double>(){{add(0.930D);add(1.075D);}});
			put (49D, new ArrayList<Double>(){{add(1.076D);add(1.240D);}});
			put (50D, new ArrayList<Double>(){{add(1.241D);add(1.426D);}});
			put (51D, new ArrayList<Double>(){{add(1.427D);add(1.636D);}});
			put (52D, new ArrayList<Double>(){{add(1.637D);add(1.872D);}});
			put (53D, new ArrayList<Double>(){{add(1.873D);add(2D);}});
		}});
//		put("paginae/atk/ravenbite", new HashMap<Double, ArrayList<Double>>(){{}}); // Cutblade can't ravenbite
		put("paginae/atk/sting", new HashMap<Double, ArrayList<Double>>(){{
			put (54D, new ArrayList<Double>(){{add(0D);add(0.510D);}});
			put (55D, new ArrayList<Double>(){{add(0.510D);add(0.579D);}});
			put (56D, new ArrayList<Double>(){{add(0.580D);add(0.656D);}});
			put (57D, new ArrayList<Double>(){{add(0.657D);add(0.742D);}});
			put (58D, new ArrayList<Double>(){{add(0.743D);add(0.837D);}});
			put (59D, new ArrayList<Double>(){{add(0.838D);add(0.943D);}});
			put (60D, new ArrayList<Double>(){{add(0.944D);add(1.059D);}});
			put (61D, new ArrayList<Double>(){{add(1.060D);add(1.188D);}});
			put (62D, new ArrayList<Double>(){{add(1.189D);add(1.330D);}});
			put (63D, new ArrayList<Double>(){{add(1.331D);add(1.487D);}});
			put (64D, new ArrayList<Double>(){{add(1.488D);add(1.659D);}});
			put (65D, new ArrayList<Double>(){{add(1.660D);add(1.847D);}});
			put (66D, new ArrayList<Double>(){{add(1.848D);add(2D);}});
		}});
		put("paginae/atk/cleave", new HashMap<Double, ArrayList<Double>>(){{
			put (87D, new ArrayList<Double>(){{add(0D);add(0.522D);}});
			put (88D, new ArrayList<Double>(){{add(0.523D);add(0.565D);}});
			put (89D, new ArrayList<Double>(){{add(0.566D);add(0.612D);}});
			put (90D, new ArrayList<Double>(){{add(0.613D);add(0.661D);}});
			put (91D, new ArrayList<Double>(){{add(0.662D);add(0.714D);}});
			put (92D, new ArrayList<Double>(){{add(0.715D);add(0.771D);}});
			put (93D, new ArrayList<Double>(){{add(0.772D);add(0.831D);}});
			put (94D, new ArrayList<Double>(){{add(0.832D);add(0.895D);}});
			put (95D, new ArrayList<Double>(){{add(0.896D);add(0.964D);}});
			put (96D, new ArrayList<Double>(){{add(0.965D);add(1.037D);}});
			put (97D, new ArrayList<Double>(){{add(1.038D);add(1.114D);}});
			put (98D, new ArrayList<Double>(){{add(1.115D);add(1.197D);}});
			put (99D, new ArrayList<Double>(){{add(1.198D);add(1.284D);}});
			put (100D, new ArrayList<Double>(){{add(1.285D);add(1.378D);}});
			put (101D, new ArrayList<Double>(){{add(1.379D);add(1.476D);}});
			put (102D, new ArrayList<Double>(){{add(1.477D);add(1.581D);}});
			put (103D, new ArrayList<Double>(){{add(1.582D);add(1.693D);}});
			put (104D, new ArrayList<Double>(){{add(1.694D);add(1.810D);}});
			put (105D, new ArrayList<Double>(){{add(1.811D);add(1.935D);}});
			put (106D, new ArrayList<Double>(){{add(1.936D);add(2D);}});
		}});
	}};

    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
	public Map<Fightview.Relation, Coord> relations = new HashMap<>();
    private Fightview fv;
	DecimalFormat agilityFormatter = new DecimalFormat("#0.000");
	int tickAlert = 0;

    public static class Action {
	public final Indir<Resource> res;
	public double cs, ct;

	public Action(Indir<Resource> res) {
	    this.res = res;
	}
    }

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int nact = (Integer)args[0];
		if(OptWnd.combatStartSoundEnabledCheckbox.a) {
			try {
				File file = new File("Alarms/" + OptWnd.combatStartSoundFilename.buf.line() + ".wav");
				if(file.exists()) {
					AudioInputStream in = AudioSystem.getAudioInputStream(file);
					AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
					AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
					Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
					((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, OptWnd.combatStartSoundVolumeSlider.val/50.0));
				}
			} catch(Exception ignored) {
			}
		}
	    return(new Fightsess(nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
	pho = -UI.scale(40);
	this.actions = new Action[nact];
    }

	protected void added() {
		fv = parent.getparent(GameUI.class).fv;
		presize();

	}

    public void presize() {
	resize(parent.sz);
	pcc = sz.div(2);
    }

    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null))
	    return;
	Coord3f raw = pl.placed.getc();
	if(raw == null)
	    return;
	pcc = map.screenxf(raw).round2();
	pho = (int)(map.screenxf(raw.add(0, 0, UI.scale(20))).round2().sub(pcc).y) - UI.scale(20);
    }

    private static class Effect implements RenderTree.Node {
	Sprite spr;
	RenderTree.Slot slot;
	boolean used = true;

	Effect(Sprite spr) {this.spr = spr;}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	}
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Collection<Effect> curfx = new ArrayList<>();

    private Effect fxon(long gobid, Resource fx, Effect cur) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return(null);
	Pipe.Op place;
	try {
	    place = gob.placed.curplace();
	} catch(Loading l) {
	    return(null);
	}
	if((cur == null) || (cur.slot == null)) {
	    try {
//		cur = new Effect(Sprite.create(null, fx, Message.nil));
		cur = new Effect(new CurrentTargetSprite(null));
		cur.slot = map.basic.add(cur.spr, place);
	    } catch(Loading l) {
		return(null);
	    }
	    curfx.add(cur);
	} else {
	    cur.slot.cstate(place);
	}
	cur.used = true;
	return(cur);
    }

    public void tick(double dt) {
	for(Iterator<Effect> i = curfx.iterator(); i.hasNext();) {
	    Effect fx = i.next();
	    if(!fx.used) {
		if(fx.slot != null) {
		    fx.slot.remove();
		    fx.slot = null;
		}
		i.remove();
	    } else {
		fx.used = false;
		fx.spr.tick(dt);
	    }
	}
    }

    public void destroy() {
	for(Effect fx : curfx) {
	    if(fx.slot != null)
		fx.slot.remove();
	}
	curfx.clear();
	super.destroy();
    }

	public static final Color ipInfoColorMe = new Color(0, 201, 4);
	public static final Color ipInfoColorEnemy = new Color(245, 0, 0);
	private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif.deriveFont(Font.BOLD), 22, new Color(0, 201, 4)).aa(true), 1, 1, new Color(0, 0, 0));
	private static final Text.Furnace ipfEnemy = new PUtils.BlurFurn(new Text.Foundry(Text.serif.deriveFont(Font.BOLD), 22, new Color(245, 0, 0)).aa(true), 1, 1, new Color(0, 0, 0));
	private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
		public String text(Integer v) {return("" + v);} // ND: Removed "IP" text. I only need to see the number, we already know it's the IP/Coins
		public Integer value() {return(fv.current.ip);}
	};
	private final Text.UText<?> oip = new Text.UText<Integer>(ipfEnemy) { // Changed this so I can give the enemy IP a different color
		public String text(Integer v) {return("" + v);} // ND: Removed "IP" text. I only need to see the number, we already know it's the IP/Coins
		public Integer value() {return(fv.current.oip);}
	};

    private static Coord actc(int i) {
	int rl = OptWnd.singleRowCombatMovesCheckBox.a ? 10 : 5;
	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(125) + ((i / rl) * actpitch)));
    }

    private static final Coord cmc = UI.scale(new Coord(0, 67));
    private static final Coord usec1 = UI.scale(new Coord(-65, 67));
    private static final Coord usec2 = UI.scale(new Coord(65, 67));
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;
    private Effect curtgtfx;
	public static boolean markCombatTargetSetting = true;
	public void draw(GOut g) {
		updatepos();
		relations.clear();
		MapView map = getparent(GameUI.class).map;
		if (map != null){
			for (Fightview.Relation rel : fv.lsrel) {
				try {
					Coord3f rawc = map.glob.oc.getgob(rel.gobid).placed.getc();
					rawc.z = rawc.z + 15;
					if (rawc == null)
						continue;
					relations.put(rel, map.screenxf(rawc).round2());
				} catch (NullPointerException ignore) {}
			}
		}
		GameUI gui = ui.gui;
		tickAlert++;
		if(tickAlert > 20){
			tickAlert = 0;
		}
		if (OptWnd.drawFloatingCombatDataOnOthersCheckBox.a) {
			try {
				for (Map.Entry<Fightview.Relation, Coord> entry : relations.entrySet()) {
					Fightview.Relation otherRelation = entry.getKey();
					Coord sc = entry.getValue();
					if (sc == null || (fv.current == otherRelation && !OptWnd.drawFloatingCombatDataOnCurrentTargetCheckBox.a)) {
						continue;
					}
					drawCombatData(g, otherRelation, sc);
				}
			} catch (NullPointerException ignored) {}
			relations.clear();
		}



		double x1 = gui.sz.x / 2.0;
		double y1 = gui.sz.y - ((gui.sz.y / 500.0) * OptWnd.combatUITopPanelHeightSlider.val);
		int x0 = (int)x1; // I have to do it like this, otherwise it's not consistent when resizing the window
		int y0 = (int)y1; // I have to do it like this, otherwise it's not consistent when resizing the window

		double bottom1 = gui.sz.y - ((gui.sz.y / 500.0) * OptWnd.combatUIBottomPanelHeightSlider.val);
		int bottom = (int)bottom1;// I have to do it like this, otherwise it's not consistent when resizing the window
		double now = Utils.rtime();

		if (OptWnd.useProperCombatUICheckBox.a) { // ND: I had to copy this entire crap from Buff.java, cause of how g.reclip adds the buffs
			ArrayList<Buff> myOpenings = new ArrayList<>(fv.buffs.children(Buff.class));
			myOpenings.sort((o2, o1) -> Integer.compare(getOpeningValue(o1), getOpeningValue(o2)));
			Buff maneuver = null;
			for (Buff buff : myOpenings) {
				try {
					if (buff.res != null && buff.res.get() != null) {
						String name = buff.res.get().name;
						if (maneuvers.contains(name)) {
							maneuver = buff;
							break;
						}
					}
				} catch (Loading ignored) {
				}
			}
			if (maneuver != null && myOpenings.size() > 1) {
				myOpenings.remove(maneuver);
				myOpenings.add(myOpenings.size(), maneuver);
			}
			int location = - Buff.cframe.sz().x - UI.scale(80);
			for (Buff buff : myOpenings) {
				try {
				Tex img = buff.res.get().flayer(Resource.imgc).tex();
				Coord isz = img.sz();
				g.chcolor(255, 255, 255, 255);
				Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
				int ameteri = 0; // Added
				if(ameter != null) {
					ameteri = (int) (100*ameter); // Added
					g.image(Buff.cframe, new Coord(x0 + location - UI.scale(3), y0 - UI.scale(20) - UI.scale(3)));
					g.chcolor(0, 0, 0, 255);
					g.frect(new Coord(x0 + location, y0 - UI.scale(20) + UI.scale(37) - UI.scale(3)), Buff.ametersz);
					g.chcolor(255, 255, 255, 255);
					g.frect(new Coord(x0 + location, y0 - UI.scale(20) + UI.scale(37) - UI.scale(3)), new Coord((int)Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
				} else {
					g.image(Buff.frame, new Coord(x0 + location - UI.scale(3), y0 - UI.scale(20) - UI.scale(3)));
				}
				if (Buff.NightdawgOPENINGS.containsKey(buff.res.get().name)) {
					g.chcolor(Buff.NightdawgOPENINGS.get(buff.res.get().name));
					g.frect(new Coord(x0 + location, y0 - UI.scale(20)), isz);
					g.chcolor(Color.WHITE);
					if(ameteri != buff.nmeter) {
						buff.ntext = null;
						buff.nmeter = ameteri;
					}
				} else {
					g.image(img, new Coord(x0 + location, y0 - UI.scale(20)));
				}
				if(buff.nmeter >= 0)
					g.aimage(buff.nmeter(), new Coord(x0 + location, y0 - UI.scale(20)).add(isz).sub(1, 1), 1, 1);
				location -= UI.scale(40);
				} catch (Loading ignored) {
				}
			}
		} else {
			for(Buff buff : fv.buffs.children(Buff.class))
				buff.draw(g.reclip(OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0 - UI.scale(20)) : pcc.add(- buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
		}

		if(fv.current != null) {
			if (OptWnd.useProperCombatUICheckBox.a) { // ND: I had to copy this entire crap from Buff.java, cause of how g.reclip adds the buffs
				ArrayList<Buff> enemyOpenings = new ArrayList<>(fv.current.buffs.children(Buff.class));
				enemyOpenings.sort((o1, o2) -> Integer.compare(getOpeningValue(o2), getOpeningValue(o1)));
				Buff maneuver = null;
				for (Buff buff : enemyOpenings) {
					try {
						if (buff.res != null && buff.res.get() != null) {
							String name = buff.res.get().name;
							if (maneuvers.contains(name)) {
								maneuver = buff;
								break;
							}
						}
					} catch (Loading ignored) {
					}
				}
				if (maneuver != null && enemyOpenings.size() > 1) {
					enemyOpenings.remove(maneuver);
					enemyOpenings.add(enemyOpenings.size(), maneuver);
				}
				int location = UI.scale(80);
				for (Buff buff : enemyOpenings) {
					try {
					String name = buff.res.get().name;
					int meterValue = getOpeningValue(buff);
					Tex img = buff.res.get().flayer(Resource.imgc).tex();
					Coord isz = img.sz();
					g.chcolor(255, 255, 255, 255);
					Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
					int ameteri = 0; // Added
					if(ameter != null) {
						ameteri = (int) (100*ameter); // Added
						g.image(Buff.cframe, new Coord(x0 + location - UI.scale(3), y0 - UI.scale(20) - UI.scale(3)));
						g.chcolor(0, 0, 0, 255);
						g.frect(new Coord(x0 + location, y0 - UI.scale(20) + UI.scale(37) - UI.scale(3)), Buff.ametersz);
						g.chcolor(255, 255, 255, 255);
						g.frect(new Coord(x0 + location, y0 - UI.scale(20) + UI.scale(37) - UI.scale(3)), new Coord((int)Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
					} else {
						g.image(Buff.frame, new Coord(x0 + location - UI.scale(3), y0 - UI.scale(20) - UI.scale(3)));
					}
					if (Buff.NightdawgOPENINGS.containsKey(name)) {
						g.chcolor(Buff.NightdawgOPENINGS.get(name));
						g.frect(new Coord(x0 + location, y0 - UI.scale(20)), isz);
						g.chcolor(Color.WHITE);
						if(ameteri != buff.nmeter) {
							buff.ntext = null;
							buff.nmeter = ameteri;
						}
					} else {
						if (name.equals("paginae/atk/combmed")){
							if(meterValue > 75){
								g.chcolor(255, 255-(tickAlert*20), 255-(tickAlert*20), 255);
							}
						}
						g.image(img, new Coord(x0 + location, y0 - UI.scale(20)));
						g.chcolor(255, 255, 255, 255);
					}
					if(buff.nmeter >= 0)
						g.aimage(buff.nmeter(), new Coord(x0 + location, y0 - UI.scale(20)).add(isz).sub(1, 1), 1, 1);
					location += UI.scale(40);
					} catch (Loading ignored) {
					}
				}
			} else {
				for(Buff buff : fv.current.buffs.children(Buff.class))
					buff.draw(g.reclip(OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 + buff.c.x + UI.scale(80), y0 - UI.scale(20)) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
			}

			g.aimage(ip.get().tex(), OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - UI.scale(40), y0 - UI.scale(30)) : pcc.add(-UI.scale(75), 0), 1, 0.5);
			g.aimage(oip.get().tex(), OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 + UI.scale(40), y0 - UI.scale(30)) : pcc.add(UI.scale(75), 0), 0, 0.5);

			if (OptWnd.markCurrentCombatTargetCheckBox.a) {
				if (!OptWnd.refreshCurrentTargetSpriteColor)
					curtgtfx = fxon(fv.current.gobid, tgtfx, curtgtfx);
				else
					OptWnd.refreshCurrentTargetSpriteColor = false;
			}
		}


		Coord cdc = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0, y0) : pcc.add(cmc);
		Coord cdc2 = new Coord(x0, y0 - UI.scale(40));
		Coord cdc3 = new Coord(x0, y0 + UI.scale(34));
		if(now < fv.atkct) {
			double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
			g.chcolor(225, 0, 0, 220);
			g.fellipse(cdc, UI.scale(OptWnd.useProperCombatUICheckBox.a ? new Coord(24, 24) : new Coord(22, 22)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
			g.chcolor();
			g.aimage(Text.renderstroked(fmt1DecPlace(fv.atkct - now)).tex(), cdc, 0.5, 0.5);
		}
		g.image(cdframe, OptWnd.useProperCombatUICheckBox.a ? new Coord(x0, y0).sub(cdframe.sz().div(2)) : cdc.sub(cdframe.sz().div(2)));

		try {
			Indir<Resource> lastact = fv.lastact;
			if(lastact != this.lastact1) {
				this.lastact1 = lastact;
				this.lastacttip1 = null;
			}
			double lastuse = fv.lastuse;
			if(lastact != null) {
				Tex ut = lastact.get().layer(Resource.imgc).tex();
				Coord useul = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - UI.scale(69), y0 - UI.scale(80)) : pcc.add(usec1).sub(ut.sz().div(2));
				g.image(ut, useul);
				g.image(useframe, useul.sub(useframeo));
				double a = now - lastuse;
				if(a < 1) {
					Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
					g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
					g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
					g.chcolor();
				}
			}
			if (!fv.currentChanged && lastact1 != null && (lastact1.get() != null && lastact1.get().name != null) && fv.current != null && fv.lastMoveUpdated) {
				Double lastMoveCooldown = fv.lastMoveCooldown;
				String lastMoveName = lastact1.get().name;
				if (unarmedAttackMoves.keySet().stream().anyMatch(lastMoveName::matches)) {
					Double moveDefaultCooldown = unarmedAttackMoves.get(lastMoveName);
					if (attackCooldownNumbers.keySet().stream().anyMatch(moveDefaultCooldown::equals)){
						if (attackCooldownNumbers.get(moveDefaultCooldown).keySet().stream().anyMatch(lastMoveCooldown::equals)){
							HashMap<Double, ArrayList<Double>> cooldowns = attackCooldownNumbers.get(moveDefaultCooldown);
							if (cooldowns != null) {
								Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
								Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
								if (minAgi != null && maxAgi != null) {
									fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
									fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
								}
							}
						}
					}
				} else if (meleeAttackMoves.keySet().stream().anyMatch(lastMoveName::matches)) {
					Double moveDefaultCooldown = meleeAttackMoves.get(lastMoveName);
					Gob gob = ui.sess.glob.oc.getgob(gui.map.plgob);
					boolean b12Equipped = false;
					boolean cutbladeEquipped = false;
					boolean pickaxeEquipped = false;
					if (gob != null) {
						for (GAttrib gobAttrib : gob.attr.values()) {
							if (gobAttrib instanceof Composite) {
								Composite c = (Composite) gobAttrib;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) { // ND: Check for the equipped weapon
										if (item.res.res.get().basename().equals("b12axe")){
											b12Equipped = true;
											break;
										} else if (item.res.res.get().basename().equals("cutblade")){
											cutbladeEquipped = true;
											break;
										} else if (item.res.res.get().basename().equals("pickaxe")){
											pickaxeEquipped = true;
											break;
										}
									}
								}
							}
						}
						if (!b12Equipped && !cutbladeEquipped && !pickaxeEquipped) { // ND: Default cooldowns, weapon has 100% attack speed
							if (attackCooldownNumbers.keySet().stream().anyMatch(moveDefaultCooldown::equals)){
								if (attackCooldownNumbers.get(moveDefaultCooldown).keySet().stream().anyMatch(lastMoveCooldown::equals)){
									HashMap<Double, ArrayList<Double>> cooldowns = attackCooldownNumbers.get(moveDefaultCooldown);
									if (cooldowns != null) {
										Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
										Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
										if (minAgi != null && maxAgi != null) {
											fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
											fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
										}
									}
								}
							}
						} else if (b12Equipped) {
							if (b12AttackCooldownNumbers.keySet().stream().anyMatch(lastMoveName::matches)) {
								HashMap<Double, ArrayList<Double>> cooldowns = b12AttackCooldownNumbers.get(lastMoveName);
								if (cooldowns != null) {
									Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
									Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
									if (minAgi != null && maxAgi != null) {
										fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
										fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
									}
								}
							}
						} else if (cutbladeEquipped) {
							if (cutbladeAttackCooldownNumbers.keySet().stream().anyMatch(lastMoveName::matches)) {
								HashMap<Double, ArrayList<Double>> cooldowns = cutbladeAttackCooldownNumbers.get(lastMoveName);
								if (cooldowns != null) {
									Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
									Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
									if (minAgi != null && maxAgi != null) {
										fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
										fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
									}
								}
							}
						}
					}
					}
				fv.lastMoveUpdated = false;
			}
		} catch(Exception ignored) {
		}
		g.aimage(Text.renderstroked(fmt2DecPlaces(fv.lastMoveCooldownSeconds)).tex(), cdc2, 0.5, 0.5);

		if (fv.current != null) {
			g.aimage(Text.renderstroked("Est. Agi: ").tex(), cdc3, 1, 0.5);
			if (fv.current.minAgi != 0 && fv.current.maxAgi != 2D) {
				g.aimage(Text.renderstroked("" + fv.current.minAgi + "x - " + fv.current.maxAgi + "x").tex(), cdc3, 0, 0.5);
			} else if (fv.current.minAgi == 0 && fv.current.maxAgi != 2D) {
				g.aimage(Text.renderstroked("<" + fv.current.maxAgi + "x", ipInfoColorMe, Color.BLACK).tex(), cdc3, 0, 0.5);
			} else if (fv.current.minAgi != 0 && fv.current.maxAgi == 2D) {
				g.aimage(Text.renderstroked(">" + fv.current.minAgi + "x", ipInfoColorEnemy, Color.BLACK).tex(), cdc3, 0, 0.5);
			} else {
				g.aimage(Text.renderstroked("Unknown").tex(), cdc3, 0, 0.5);
			}
		}

		if(fv.current != null) {
			try {
				Indir<Resource> lastact = fv.current.lastact;
				if(lastact != this.lastact2) {
					this.lastact2 = lastact;
					this.lastacttip2 = null;
				}
				double lastuse = fv.current.lastuse;
				if(lastact != null) {
					Tex ut = lastact.get().layer(Resource.imgc).tex();
					Coord useul = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 + UI.scale(69) - ut.sz().x, y0 - UI.scale(80)) : pcc.add(usec2).sub(ut.sz().div(2));
					g.image(ut, useul);
					g.image(useframe, useul.sub(useframeo));
					double a = now - lastuse;
					if(a < 1) {
						Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
						g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
						g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
						g.chcolor();
					}
				}
			} catch(Loading l) {
			}
		}
		for(int i = 0; i < actions.length; i++) {
			Coord ca = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - 18, bottom - UI.scale(150)).add(actc(i)) : pcc.add(actc(i));
			Action act = actions[i];
			try {
				if(act != null) {
					Tex img = act.res.get().layer(Resource.imgc).tex();
					Coord hsz = img.sz().div(2);
					g.image(img, ca);
					if(now < act.ct) {
						double a = (now - act.cs) / (act.ct - act.cs);
						g.chcolor(0, 0, 0, 132);
						g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
						g.chcolor();
					}
					if (OptWnd.showCombatHotkeysUICheckBox.a) {
					String keybindString = kb_acts[i].key().name();
					g.aimage(new TexI(Utils.outline2(keybindsFoundry.render(keybindString).img, Color.BLACK, true)), ca.add((int)(img.sz().x/2), img.sz().y + UI.scale(8)), 0.5, 0.5);
					}
					//
					if(i == use) {
						g.image(indframe, ca.sub(indframeo));
					} else if(i == useb) {
						g.image(indbframe, ca.sub(indbframeo));
					} else {
						g.image(actframe, ca.sub(actframeo));
					}
				}
			} catch(Loading l) {}
		}
		IMeter.Meter stam = gui.getmeter("stam", 0);
		IMeter.Meter hp = gui.getmeter("hp", 0);
		if (OptWnd.useProperCombatUICheckBox.a) {
			if (stam != null) {
				Coord msz = UI.scale(new Coord(150, 20));
				Coord sc = new Coord(x0 - msz.x/2,  y0 + UI.scale(70));
				drawStamMeterBar(g, stam, sc, msz);
			}
			if (hp != null) {
				Coord msz = UI.scale(new Coord(150, 20));
				Coord sc = new Coord(x0 - msz.x/2,  y0 + UI.scale(40));
				drawHealthMeterBar(g, hp, sc, msz);
			}
		}
	}

	private static final Color red = new Color(168, 0, 0, 255);
	private static final Color yellow = new Color(182, 165, 0, 255);
	private void drawHealthMeterBar(GOut g, IMeter.Meter m, Coord sc, Coord msz) {
		int w = msz.x;
		int w1 = (int) Math.ceil(w * m.a);
		int w2 = (int) Math.ceil(w * (IMeter.characterSoftHealthPercent/100));
		g.chcolor(yellow);
		g.frect(sc, new Coord(w1, msz.y));
		g.chcolor(red);
		g.frect(sc, new Coord(w2, msz.y));
		g.chcolor(barframe);
		g.line(new Coord(sc.x+w1, sc.y), new Coord(sc.x+w1, sc.y+msz.y), 1);
		g.rect(sc, new Coord(msz.x, msz.y));

		g.chcolor(Color.WHITE);
		g.aimage(Text.renderstroked((IMeter.characterCurrentHealth+" ("+(Utils.fmt1DecPlace((int)(m.a*100)))+"% HHP)"), Text.num12boldFnd).tex(), new Coord(sc.x+msz.x/2, sc.y+msz.y/2), 0.5, 0.5);
	}

	private void drawCombatData(GOut g, Fightview.Relation rels, Coord sc) {
		int scaledY = sc.y - UI.scale(90);
		Coord topLeftFrame = new Coord(sc.x - UI.scale(43), scaledY);
		Coord bgTopLeftFrame = new Coord(sc.x - UI.scale(41), scaledY);
		boolean openings;
		boolean cleaveUsed = false;
		long cleaveDuration = 4300;
		//Check if cleave indicator is needed
		if (rels.lastActCleave != null) {
			Gob gob = ui.sess.glob.oc.getgob(rels.gobid);
			for (GAttrib gobAttrib : gob.attr.values()) { // ND: Do the following stuff to figure out the *MINIMUM* cooldown someone can have after cleaving.
				if (gobAttrib instanceof Composite) {
					Composite c = (Composite) gobAttrib;
					if (c.comp.cequ.size() > 0) {
						for (Composited.ED item : c.comp.cequ) { // ND: Check for the equipped weapon
							if (item.res.res.get().basename().equals("b12axe")){ // 5.4
								cleaveDuration = 5400;
								break;
							}
							if (item.res.res.get().basename().equals("cutblade")){ // 5.2
								cleaveDuration = 5200;
								break;
							} else { // 4.3 seconds for anything else
								cleaveDuration = 4300;
							}
						}
					}
				}
			}
			cleaveUsed = System.currentTimeMillis() - rels.lastActCleave < cleaveDuration;
		}
		boolean defenseUsed = false;
		if (rels.lastActDefence != null) {
			defenseUsed = System.currentTimeMillis() - rels.lastActDefence < rels.lastDefenceDuration;
		}

		// ND: Check for openings depending if it's a player or not.
		Gob gob = ui.sess.glob.oc.getgob(rels.gobid);
		if (gob != null && gob.getres() != null){
			if (gob.getres().name.equals("gfx/borka/body")){ // ND: If it's a player, the first buff is the maneuver, always, so skip it.
				openings = rels.buffs.children(Buff.class).size() > 1;
			} else { // ND: Everything else doesn't have a maneuver.
				openings = rels.buffs.children(Buff.class).size() > 0;
			}
		} else {
			openings = rels.buffs.children(Buff.class).size() > 1;
		}

		if (OptWnd.combatInfoBackgroundToggledCheckBox.a) {
			g.chcolor(new Color(0, 0, 0, 80));
			g.frect(bgTopLeftFrame, UI.scale(new Coord(82, 24)));
		}

		//reset color
		g.chcolor(255, 255, 255, 255);

		//prepare colors for ip text
//		Color ipcol = rels.ip >= 6 ? new Color(0, 201, 4) : new Color(255, 255, 255);
//		Color oipcol = rels.oip >= 6 ? new Color(245, 0, 0) : new Color(255, 255, 255);

		//made different x offsets depending on how many digits coins have
		int ipOffset = rels.ip < 10 ? 21 : rels.ip < 100 ? 25 : 29;
		int oipOffset = rels.oip < 10 ? 77 : rels.oip < 100 ? 80 : 85;

		//add ip / oip text
 		g.aimage(Text.renderstroked(Integer.toString(rels.ip), ipInfoColorMe, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeftFrame.x + UI.scale(ipOffset), topLeftFrame.y + UI.scale(13)), 1, 0.5);
		g.aimage(Text.renderstroked(Integer.toString(rels.oip), ipInfoColorEnemy, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeftFrame.x + UI.scale(oipOffset), topLeftFrame.y + UI.scale(13)), 1, 0.5);
		//maneuver
		for (Buff buff : rels.buffs.children(Buff.class)) {
			try {
				if (buff.res != null && buff.res.get() != null) {
					String name = buff.res.get().name;
					if (maneuvers.contains(name)) {
						g.chcolor(0, 0, 0, 200);
						g.frect(new Coord(topLeftFrame.x + UI.scale(32), topLeftFrame.y + UI.scale(1)), UI.scale(new Coord(22, 22)));
						g.chcolor(255, 255, 255, 255);
						int meterValue = getOpeningValue(buff);
						Resource.Image img = buff.res.get().flayer(Resource.imgc);
						Tex maneuverTexture = new TexI(PUtils.uiscale(img.scaled, UI.scale(new Coord(20, 20))));
						if (name.equals("paginae/atk/combmed")){
							if(meterValue > 75){
								g.chcolor(255, 255-(tickAlert*20), 255-(tickAlert*20), 255);
							}
						}
						g.image(maneuverTexture, new Coord(topLeftFrame.x + UI.scale(33), topLeftFrame.y + UI.scale(2)));
						if(meterValue > 0){
							g.chcolor(0, 0, 0, 200);
							g.frect(new Coord(topLeftFrame.x + UI.scale(32), topLeftFrame.y + UI.scale(23)), UI.scale(new Coord(22, 6)));
							if(meterValue < 30){
								g.chcolor(255, 255, 255, 255);
							} else {
								g.chcolor(255, (255 - (255*meterValue)/100), (255 - (255*meterValue)/100), 255);
							}
							g.frect(new Coord(topLeftFrame.x + UI.scale(33), topLeftFrame.y + UI.scale(24)), UI.scale(new Coord((20 * meterValue)/100, 4)));
						}
					}
				}
			} catch (Loading ignored) {
			}
		}



		//openings only if has any
		if (openings) {
			if (OptWnd.combatInfoBackgroundToggledCheckBox.a) {
				g.chcolor(new Color(0, 0, 0, 80));
				g.frect(new Coord(bgTopLeftFrame.x, bgTopLeftFrame.y + UI.scale(24)), UI.scale(new Coord(82, 26)));
			}

			Map<String, Color> colorMap = new HashMap<>();
			colorMap.put("paginae/atk/offbalance", new Color(0, 128, 3));
			colorMap.put("paginae/atk/dizzy", new Color(39, 82, 191));
			colorMap.put("paginae/atk/reeling", new Color(217, 177, 20));
			colorMap.put("paginae/atk/cornered", new Color(192, 28, 28));

			List<TemporaryOpening> openingList = new ArrayList<>();
			for (Buff buff : rels.buffs.children(Buff.class)) {
				try {
					if (buff.res != null && buff.res.get() != null) {
						String name = buff.res.get().name;
						if (colorMap.containsKey(name)) {
							int meterValue = getOpeningValue(buff);
							openingList.add(new TemporaryOpening(meterValue, colorMap.get(name)));
						}
					}
				} catch (Loading ignored) {
				}
			}
			openingList.sort((o1, o2) -> Integer.compare(o2.value, o1.value));

			int openingOffsetX = 4;
			int iterator = 1;
			for (TemporaryOpening opening : openingList) {
				g.chcolor(0, 0, 0, 200);
				g.frect(new Coord(topLeftFrame.x + UI.scale(openingOffsetX) - UI.scale(1), topLeftFrame.y + UI.scale(30) - UI.scale(1)), UI.scale(new Coord(20, 20)));
				g.chcolor(opening.color);
				g.frect(new Coord(topLeftFrame.x + UI.scale(openingOffsetX), topLeftFrame.y + UI.scale(30)), UI.scale(new Coord(18, 18)));
				g.chcolor(255, 255, 255, 255);

				int valueOffset = opening.value < 10 ? 15 : opening.value< 100 ? 18 : 20;
				g.aimage(Text.renderstroked(String.valueOf(opening.value), openingAdditionalFont).tex(), new Coord(topLeftFrame.x + UI.scale(openingOffsetX) + UI.scale(valueOffset) - UI.scale(1), topLeftFrame.y + UI.scale(39)), 1, 0.5);
				if (iterator == 2) openingOffsetX += 20;
				else openingOffsetX += 20;
				iterator += 1;
			}
		}

		//add cleave cooldown indicator
		if (cleaveUsed) {
			long timer = ((cleaveDuration - (System.currentTimeMillis() - rels.lastActCleave)));
			if (OptWnd.combatInfoBackgroundToggledCheckBox.a) {
				g.chcolor(new Color(0, 0, 0, 80));
				g.frect(new Coord(bgTopLeftFrame.x, bgTopLeftFrame.y - UI.scale(13)), UI.scale(new Coord(82, 13)));
			}
			g.chcolor(new Color(0, 0, 0, 255));
			g.frect(new Coord(topLeftFrame.x + UI.scale(3), topLeftFrame.y - UI.scale(12)), UI.scale(new Coord((int) ((80 * timer)/cleaveDuration)+1, 13)));
			g.chcolor(new Color(213, 0, 0, 255));
			g.frect(new Coord(topLeftFrame.x + UI.scale(4), topLeftFrame.y - UI.scale(11)), UI.scale(new Coord((int) ((80 * timer)/cleaveDuration), 11)));
			g.chcolor(new Color(255, 255, 255, 255));
			g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeftFrame.x + UI.scale(52), topLeftFrame.y - UI.scale(6)), 1, 0.5);
		}

		//add defense cooldown indicator, just like cleave
		if (defenseUsed) {
			long timer = ((rels.lastDefenceDuration - (System.currentTimeMillis() - rels.lastActDefence)));
			if (OptWnd.combatInfoBackgroundToggledCheckBox.a) {
				g.chcolor(new Color(0, 0, 0, 80));
				g.frect(new Coord(bgTopLeftFrame.x, bgTopLeftFrame.y - UI.scale(13)), UI.scale(new Coord(82, 13)));
			}
			g.chcolor(new Color(0, 0, 0, 255));
			g.frect(new Coord(topLeftFrame.x + UI.scale(3), topLeftFrame.y - UI.scale(12)), UI.scale(new Coord((int) ((79 * timer)/rels.lastDefenceDuration)+1, 13)));
			g.chcolor(new Color(227, 136, 0, 255));
			g.frect(new Coord(topLeftFrame.x + UI.scale(4), topLeftFrame.y - UI.scale(11)), UI.scale(new Coord((int) ((79 * timer)/rels.lastDefenceDuration), 11)));
			g.chcolor(new Color(255, 255, 255, 255));
			g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeftFrame.x + UI.scale(52), topLeftFrame.y - UI.scale(6)), 1, 0.5);
		}
		g.chcolor(255, 255, 255, 255);
	}

	private int getOpeningValue(Buff buff) {
		Double meterDouble = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.getAmeteri().get();
		if (meterDouble != null) {
			return (int) (100 * meterDouble);
		}
		return 0;
	}

	public String getCooldownTime(long time) {
		double convertedTime = time / 1000.0;
		return String.format("%.1f", convertedTime);
	}

	private static final Color blu1 = new Color(3, 3, 80, 141);
	private static final Color blu2 = new Color(32, 32, 184, 90);
	private static final Color blu3 = new Color(14, 14, 213, 70);
	private static final Color barframe = new Color(255, 255, 255, 111);
	private void drawStamMeterBar(GOut g, IMeter.Meter m, Coord sc, Coord msz) {
		int w = msz.x;
		int w1 = (int) Math.ceil(w * m.a);
		int w2 = (int) (w * Math.max(m.a-0.25,0));
		int w3 = (int) (w * Math.max(m.a-0.50,0));

		g.chcolor(blu1);
		g.frect(sc, new Coord(w1, msz.y));
		g.chcolor(blu2);
		g.frect(new Coord(sc.x+(w*25)/100, sc.y), new Coord(w2, msz.y));
		g.chcolor(blu3);
		g.frect(new Coord(sc.x+(w*50)/100, sc.y), new Coord(w3, msz.y));
		g.chcolor(barframe);
		g.line(new Coord(sc.x+w1, sc.y), new Coord(sc.x+w1, sc.y+msz.y), 1);
		g.rect(sc, new Coord(msz.x, msz.y));
		g.chcolor(Color.WHITE);
		String staminaBarText = Utils.fmt1DecPlace((int)(m.a*100));
		Gob myself = ui.gui.map.player();
		if (myself != null && myself.imDrinking) {
			g.chcolor(new Color(0, 222, 0));
			staminaBarText = staminaBarText + " (Drinking)";
		}
		g.aimage(Text.renderstroked(staminaBarText, Text.num12boldFnd).tex(), new Coord(sc.x+msz.x/2, sc.y+msz.y/2), 0.5, 0.5);
	}
	public static String fmt1DecPlace(double value) {
		double rvalue = (double) Math.round(value * 10) / 10;
		return (rvalue % 1 == 0) ? Integer.toString((int) rvalue) : Double.toString(rvalue);
	}

	public static String fmt2DecPlaces(double value) {
		double rvalue = (double) Math.round(value * 100) / 100;
		return (rvalue % 1 == 0) ? Integer.toString((int) rvalue) : Double.toString(rvalue);
	}

    private Widget prevtt = null;
    private Text acttip = null;
    public static final String[] keytips = {"1", "2", "3", "4", "5", "Shift+1", "Shift+2", "Shift+3", "Shift+4", "Shift+5"};
	public Object tooltip(Coord c, Widget prev) {
		GameUI gui = ui.gui;
		double x1 = gui.sz.x / 2.0;
		double y1 = gui.sz.y - ((gui.sz.y / 500.0) * OptWnd.combatUITopPanelHeightSlider.val);
		int x0 = (int)x1; // I have to do it like this, otherwise it's not consistent when resizing the window
		int y0 = (int)y1; // I have to do it like this, otherwise it's not consistent when resizing the window

		double bottom1 = gui.sz.y - ((gui.sz.y / 500.0) * OptWnd.combatUIBottomPanelHeightSlider.val);
		int bottom = (int)bottom1;// I have to do it like this, otherwise it's not consistent when resizing the window
		for(Buff buff : fv.buffs.children(Buff.class)) {
			Coord dc = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0 - UI.scale(20)) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
			if(c.isect(dc, buff.sz)) {
				Object ret = buff.tooltip(c.sub(dc), prevtt);
				if(ret != null) {
					prevtt = buff;
					return(ret);
				}
			}
		}
		if(fv.current != null) {
			for(Buff buff : fv.current.buffs.children(Buff.class)) {
				Coord dc = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 + buff.c.x + UI.scale(80), y0 - UI.scale(20)) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);

				if(c.isect(dc, buff.sz)) {
					Object ret = buff.tooltip(c.sub(dc), prevtt);
					if(ret != null) {
						prevtt = buff;
						return(ret);
					}
				}
			}
		}
		final int rl = 5;
		for(int i = 0; i < actions.length; i++) {
			Coord ca = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - 18, bottom - UI.scale(150)).add(actc(i)).add(16, 16) : pcc.add(actc(i));
			Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
			try {
				if(act != null) {
					Tex img = act.get().layer(Resource.imgc).tex();
					ca = ca.sub(img.sz().div(2));
					if(c.isect(ca, img.sz())) {
						String tip = act.get().layer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + kb_acts[i].key().name() + "}})";
						if((acttip == null) || !acttip.text.equals(tip))
							acttip = RichText.render(tip, -1);
						return(acttip);
					}
				}
			} catch(Loading l) {}
		}
		try {
			Indir<Resource> lastact = this.lastact1;
			if(lastact != null) {
				Coord usesz = lastact.get().layer(Resource.imgc).sz;
				Coord lac = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 - UI.scale(69), y0 - UI.scale(80)).add(usesz.div(2)) : pcc.add(usec1);
				if(c.isect(lac.sub(usesz.div(2)), usesz)) {
					if(lastacttip1 == null)
						lastacttip1 = Text.render(lastact.get().layer(Resource.tooltip).t);
					return(lastacttip1);
				}
			}
		} catch(Loading l) {}
		try {
			Indir<Resource> lastact = this.lastact2;
			if(lastact != null) {
				Coord usesz = lastact.get().layer(Resource.imgc).sz;
				Coord lac = OptWnd.useProperCombatUICheckBox.a ? new Coord(x0 + UI.scale(69) - usesz.x, y0 - UI.scale(80)).add(usesz.div(2)) : pcc.add(usec2);
				if(c.isect(lac.sub(usesz.div(2)), usesz)) {
					if(lastacttip2 == null)
						lastacttip2 = Text.render(lastact.get().layer(Resource.tooltip).t);
					return(lastacttip2);
				}
			}
		} catch(Loading l) {}
		return(null);
	}

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = (Integer)args[0];
	    if(args.length > 1) {
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		actions[n] = new Action(res);
	    } else {
		actions[n] = null;
	    }
	} else if(msg == "acool") {
	    int n = (Integer)args[0];
	    double now = Utils.rtime();
	    actions[n].cs = now;
	    actions[n].ct = now + (((Number)args[1]).doubleValue() * 0.06);
	} else if(msg == "use") {
	    this.use = (Integer)args[0];
	    this.useb = (args.length > 1) ? ((Integer)args[1]) : -1;
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding[] kb_acts = {
	KeyBinding.get("fgt/0", KeyMatch.forcode(KeyEvent.VK_1, 0)),
	KeyBinding.get("fgt/1", KeyMatch.forcode(KeyEvent.VK_2, 0)),
	KeyBinding.get("fgt/2", KeyMatch.forcode(KeyEvent.VK_3, 0)),
	KeyBinding.get("fgt/3", KeyMatch.forcode(KeyEvent.VK_4, 0)),
	KeyBinding.get("fgt/4", KeyMatch.forcode(KeyEvent.VK_5, 0)),
	KeyBinding.get("fgt/5", KeyMatch.forcode(KeyEvent.VK_1, KeyMatch.S)),
	KeyBinding.get("fgt/6", KeyMatch.forcode(KeyEvent.VK_2, KeyMatch.S)),
	KeyBinding.get("fgt/7", KeyMatch.forcode(KeyEvent.VK_3, KeyMatch.S)),
	KeyBinding.get("fgt/8", KeyMatch.forcode(KeyEvent.VK_4, KeyMatch.S)),
	KeyBinding.get("fgt/9", KeyMatch.forcode(KeyEvent.VK_5, KeyMatch.S)),
    };
    public static final KeyBinding kb_relcycle =  KeyBinding.get("fgt-cycle", KeyMatch.nil);
	public static final KeyBinding kb_nearestTarget =  KeyBinding.get("fgt-nearestTarget", KeyMatch.forcode(KeyEvent.VK_SPACE, 0));

    /* XXX: This is a bit ugly, but release message do need to be
     * properly sequenced with use messages in some way. */
    private class Release implements Runnable {
	final int n;

	Release(int n) {
	    this.n = n;
	    Environment env = ui.getenv();
	    Render out = env.render();
	    out.fence(this);
	    env.submit(out);
	}


	public void run() {
	    wdgmsg("rel", n);
	}
    }

    private UI.Grab holdgrab = null;
    private int held = -1;
    public boolean globtype(char key, KeyEvent ev) {
	// ev = new KeyEvent((java.awt.Component)ev.getSource(), ev.getID(), ev.getWhen(), ev.getModifiersEx(), ev.getKeyCode(), ev.getKeyChar(), ev.getKeyLocation());
	{
	    int n = -1;
	    for(int i = 0; i < kb_acts.length; i++) {
		if(kb_acts[i].key().match(ev)) {
		    n = i;
		    break;
		}
	    }
	    int fn = n;
	    if((n >= 0) && (n < actions.length)) {
		MapView map = getparent(GameUI.class).map;
		Coord mvc = map.rootxlate(ui.mc);
		if(held >= 0) {
		    new Release(held);
		    held = -1;
		}
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Maptest(mvc) {
			    protected void hit(Coord pc, Coord2d mc) {
				wdgmsg("use", fn, 1, ui.modflags(), mc.floor(OCache.posres));
			    }

			    protected void nohit(Coord pc) {
				wdgmsg("use", fn, 1, ui.modflags());
			    }
			}.run();
		}
		if(holdgrab == null)
		    holdgrab = ui.grabkeys(this);
		held = n;
		return(true);
	    }
	}
	if(kb_nearestTarget.key().match(ev)) {
		targetNearestFoe(getparent(GameUI.class));
		return (true);
	}
	if(kb_relcycle.key().match(ev, KeyMatch.S)) {
	    if((ev.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
		Fightview.Relation cur = fv.current;
		if(cur != null) {
		    fv.lsrel.remove(cur);
		    fv.lsrel.addLast(cur);
		}
	    } else {
		Fightview.Relation last = fv.lsrel.getLast();
		if(last != null) {
		    fv.lsrel.remove(last);
		    fv.lsrel.addFirst(last);
		}
	    }
	    fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
	    return(true);
	}
	return(super.globtype(key, ev));
    }

    public boolean keydown(KeyEvent ev) {
	return(false);
    }

    public boolean keyup(KeyEvent ev) {
	if((holdgrab != null) && (kb_acts[held].key().match(ev, KeyMatch.MODS))) {
	    MapView map = getparent(GameUI.class).map;
	    new Release(held);
	    holdgrab.remove();
	    holdgrab = null;
	    held = -1;
	    return(true);
	}
	return(false);
    }

	private static class TemporaryOpening{
		private int value;
		private Color color;

		public TemporaryOpening(int value, Color color) {
			this.value = value;
			this.color = color;
		}
	}

	private void targetNearestFoe(GameUI gui) {
		//Select nearest enemy
		Fightview.Relation cur = fv.current;
		Fightview.Relation closestRel = fv.current;
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) {
			return;
		}

		for (Fightview.Relation rel : fv.lsrel) {
			Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
			if (gob != null && gob.rc != null) {
				if (closest == null) {
					closest = gob;
					closestRel = rel;
				} else if (player.rc.dist(gob.rc) < player.rc.dist(closest.rc)) {
					closest = gob;
					closestRel = rel;
				}
			}
		}

		if (cur != null) {
			fv.lsrel.remove(cur);
			fv.lsrel.addLast(cur);
		}

		if (closestRel != null) {
			fv.wdgmsg("bump", (int) closestRel.gobid);
		}
	}
}
