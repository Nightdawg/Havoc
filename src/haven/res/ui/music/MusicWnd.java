/* Preprocessed source code */
package haven.res.ui.music;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.awt.event.KeyEvent;
import haven.Audio.CS;

import javax.sound.midi.*;

/* >wdg: MusicWnd */
@haven.FromResource(name = "ui/music", version = 34)
public class MusicWnd extends Window
{
	public static final Tex[] tips;
	public static final Map<Integer, Integer> keys;
	public static final int[] nti;
	public static final int[] shi;
	public static final int[] ntp;
	public static final int[] shp;
	public static final Tex[] ikeys;
	public final boolean[] cur;
	public final int[] act;
	public final double start;
	public double latcomp;
	public double tempo = 1;
	public int actn;
	public Label tempoLabel = new Label("Tempo Factor: " + tempo);
	public HafenMidiplayer hafenMidiplayer;
	Thread midiThread;


	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;


	public MusicWnd(final String s, final int n) {
		super(MusicWnd.ikeys[0].sz().mul(MusicWnd.nti.length, 1), s, true);
//		System.out.println(s);
		this.cur = new boolean[36];
		this.latcomp = 0.15;
		this.act = new int[n];
		this.start = System.currentTimeMillis() / 1000.0;
		this.resize(MusicWnd.ikeys[0].sz().x*(MusicWnd.nti.length),sz.y);

		hafenMidiplayer = new HafenMidiplayer(this);
		midiThread = new Thread(hafenMidiplayer, "HafenMidiPlayer");
		midiThread.start();
		TextEntry midiEntry = new TextEntry(UI.scale(200),"music/example.mid");
		add(midiEntry,UI.scale(10),sz.y-UI.scale(140));
		midiEntry.focusctl = false;
		midiEntry.hasfocus = false;

		Button startButton = new Button(UI.scale(100), "Play") {
			@Override
			public void click() {
				try {
					hafenMidiplayer.startPlaying(midiEntry.buf.line());
					parent.setfocus(this);
				} catch (Exception e) {
//					System.out.println(e);
				}
			}
		};
		add(startButton, new Coord(UI.scale(10), sz.y-UI.scale(120)));

		Button stopButton = new Button(UI.scale(100), "Stop") {
			@Override
			public void click() {
				try {
					hafenMidiplayer.stopPlaying();
					parent.setfocus(this);
				} catch (Exception e) {
//					System.out.println(e);
				}
			}
		};
		add(stopButton, new Coord(UI.scale(110), sz.y-UI.scale(120)));
		setfocus(stopButton);

		Button partyButton = new Button(UI.scale(100), "Party playing") {
			public void click() {
				for (Widget w = ui.gui.chat.lchild; w != null; w = w.prev) {
					if (w instanceof ChatUI.MultiChat) {
						ChatUI.MultiChat chat = (ChatUI.MultiChat) w;
						if (chat.name().equals("Party")) {
							String timetoplay = ""+(System.currentTimeMillis()+1000);
							chat.send("HFMPL@@@"+timetoplay+"|"+midiEntry.buf.line());
							break;
						}
					}
				}
			}


		};
		add(partyButton, new Coord(UI.scale(440), sz.y-UI.scale(120)) );

		HSlider tempoHSlider = new HSlider(UI.scale(200), 0, 20, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int) (tempo * 10);
			}

			public void changed() {
				tempo = val / 10.0;
				hafenMidiplayer.setTempo((float)tempo);
				tempoLabel.settext(("Tempo Factor " + tempo));
				//System.out.println(tempo);
			}};
		add ( tempoLabel , new Coord(UI.scale(230), sz.y-UI.scale(130)));
		add(tempoHSlider , new Coord(UI.scale(230), sz.y-UI.scale(110)));
	}


	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && (msg == "close")) {
			hafenMidiplayer.kill();
			this.reqdestroy();
			midiThread.interrupt();
			super.wdgmsg(sender, msg, args);
		}
		super.wdgmsg(sender, msg, args);
	}

	public static Widget mkwidget(final UI ui, final Object[] array) {
		return (Widget)new MusicWnd((String)array[0], (int)array[1]);
	}

	protected void added() {
		super.added();
		this.ui.grabkeys((Widget)this);
	}

	public void cdraw(final GOut gOut) {
		final boolean[] array = new boolean[this.cur.length];
		for (int i = 0; i < this.actn; ++i) {
			try {
				array[this.act[i]] = true;
			} catch (ArrayIndexOutOfBoundsException e) {}
		}
		int n = 0;
		for (int j = 0; j < MusicWnd.nti.length; ++j) {
			final Coord coord = new Coord(MusicWnd.ikeys[0].sz().x * MusicWnd.ntp[j], 0);
			final int n2 = array[MusicWnd.nti[j] + n] ? 1 : 0;
			gOut.image(MusicWnd.ikeys[n2], coord);
			gOut.image(MusicWnd.tips[MusicWnd.nti[j]], coord.add((MusicWnd.ikeys[0].sz().x - MusicWnd.tips[MusicWnd.nti[j]].sz().x) / 2, MusicWnd.ikeys[0].sz().y - MusicWnd.tips[MusicWnd.nti[j]].sz().y - ((n2 != 0) ? UI.scale(9) : UI.scale(12))));
		}
		final int n3 = MusicWnd.ikeys[0].sz().x - MusicWnd.ikeys[2].sz().x / 2;
		for (int k = 0; k < MusicWnd.shi.length; ++k) {
			final Coord coord2 = new Coord(MusicWnd.ikeys[0].sz().x * MusicWnd.shp[k] + n3, 0);
			final boolean b = array[MusicWnd.shi[k] + n];
			gOut.image(MusicWnd.ikeys[b ? 3 : 2], coord2);
			gOut.image(MusicWnd.tips[MusicWnd.shi[k]], coord2.add((MusicWnd.ikeys[2].sz().x - MusicWnd.tips[MusicWnd.shi[k]].sz().x) / 2, MusicWnd.ikeys[2].sz().y - MusicWnd.tips[MusicWnd.shi[k]].sz().y - (b ? UI.scale(9) : UI.scale(12))));
		}
	}

	public boolean keydown(final KeyEvent keyEvent) {
		final double n = keyEvent.getWhen() / 1000.0 + this.latcomp;
		final Integer n2 = MusicWnd.keys.get(keyEvent.getKeyCode());
		if (n2 != null) {
			int n3 = n2;
			if (!this.cur[n3]) {
				if (this.actn >= this.act.length) {
					this.wdgmsg("stop", new Object[] { this.act[0], (float)(n - this.start) });
					for (int i = 1; i < this.actn; ++i) {
						this.act[i - 1] = this.act[i];
					}
					--this.actn;
				}
				this.wdgmsg("play", new Object[] { n3, (float)(n - this.start) });
				this.cur[n3] = true;
				this.act[this.actn++] = n3;
			}
		}
		super.keydown(keyEvent);
		return true;
	}

	private void stopnote(final double n, final int n2) {
		if (this.cur[n2]) {
			for (int i = 0; i < this.actn; ++i) {
				if (this.act[i] == n2) {
					this.wdgmsg("stop", new Object[] { n2, (float)(n - this.start) });
					--this.actn;
					while (i < this.actn) {
						this.act[i] = this.act[i + 1];
						++i;
					}
					break;
				}
			}
			this.cur[n2] = false;
		}
	}

	public boolean keyup(final KeyEvent keyEvent) {
		final double n = keyEvent.getWhen() / 1000.0 + this.latcomp;
		final Integer n2 = MusicWnd.keys.get(keyEvent.getKeyCode());
		if (n2 != null) {
			final int intValue = n2;
			this.stopnote(n, intValue);
			return true;
		}
		return true;
	}

	public boolean keydown(int hafenkey, long time) {
		final double n = time / 1000.0 + this.latcomp;
		final Integer n2 = hafenkey;
		if (n2 != null) {
			int n3 = n2;
			if (!this.cur[n3]) {
				if (this.actn >= this.act.length) {
					this.wdgmsg("stop", new Object[] { this.act[0], (float)(n - this.start) });
					for (int i = 1; i < this.actn; ++i) {
						this.act[i - 1] = this.act[i];
					}
					--this.actn;
				}
				this.wdgmsg("play", new Object[] { n3, (float)(n - this.start) });
				this.cur[n3] = true;
				this.act[this.actn++] = n3;
			}
			return true;
		}
		return true;
	}

	public boolean keyup(int hafenkey, long time) {
		final double n = time / 1000.0 + this.latcomp;
		final Integer n2 = hafenkey;
		if (n2 != null) {
			final int intValue = n2;
			this.stopnote(n, intValue);
			return true;
		}
		return true;
	}

	static {
		nti = new int[] {    0,  2,  4,  5,  7,  9, 11,
				12, 14, 16, 17, 19, 21, 23,
				24, 26, 28, 29, 31, 33, 35 };

		shi = new int[] {   1, 3, 6, 8, 10 ,
				12+1, 12+3, 12+6, 12+8, 12+10,
				24+1, 24+3, 24+6, 24+8, 24+10 };

		ntp = new int[] {   0, 1, 2, 3, 4, 5, 6 ,
				7+0, 7+1, 7+2, 7+3, 7+4, 7+5, 7+6 ,
				14+0, 14+1, 14+2, 14+3, 14+4, 14+5, 14+6 };
		shp = new int[] {   0, 1, 3, 4, 5,
				7+0, 7+1, 7+3, 7+4, 7+5,
				14+0, 14+1, 14+3, 14+4, 14+5 };
		final HashMap<Integer, Integer> keys2 = new HashMap<Integer, Integer>();
		keys2.put(49, 0);
		keys2.put(50, 1);
		keys2.put(51, 2);
		keys2.put(52, 3);
		keys2.put(53, 4);
		keys2.put(54, 5);
		keys2.put(55, 6);
		keys2.put(56, 7);
		keys2.put(57, 8);
		keys2.put(48, 9);
		keys2.put(81, 10);
		keys2.put(87, 11);

		keys2.put(69, 12+0);
		keys2.put(82, 12+1);
		keys2.put(84, 12+2);
		keys2.put(89, 12+3);
		keys2.put(85, 12+4);
		keys2.put(73, 12+5);
		keys2.put(79, 12+6);
		keys2.put(80, 12+7);
		keys2.put(65, 12+8);
		keys2.put(83, 12+9);
		keys2.put(68, 12+10);
		keys2.put(70, 12+11);

		keys2.put(71, 24+0);
		keys2.put(72, 24+1);
		keys2.put(74, 24+2);
		keys2.put(75, 24+3);
		keys2.put(76, 24+4);
		keys2.put(90, 24+5);
		keys2.put(88, 24+6);
		keys2.put(67, 24+7);
		keys2.put(86, 24+8);
		keys2.put(66, 24+9);
		keys2.put(78, 24+10);
		keys2.put(77, 24+11);
		final Tex[] ikeys2 = new Tex[4];
        /*for (int i = 0; i < 4; ++i) {
            ikeys2[i] = ((Resource.Image)Resource.classres((Class)haven.res.ui.music.MusicWnd.class).layer(Resource.imgc, (Object)i)).tex();
        }*/
		for (int i = 0; i < 4; i++) {
			ikeys2[i] = Resource.local().loadwait("ui/music").layer(Resource.imgc,i).tex();
		}

		/*final String s = "ZSXDCVGBHNJM";*/
		final String s =    "1234567890QW"+
				"ERTYUIOPASDF"+
				"GHJKLZXCVBNM";
		final Text.Foundry aa = new Text.Foundry(Text.fraktur.deriveFont(1, 16.0f)).aa(true);
		final Tex[] tips2 = new Tex[s.length()];
		for (int j = 0; j < MusicWnd.nti.length; ++j) {
			final int n = MusicWnd.nti[j];
			tips2[n] = aa.render(s.substring(n, n + 1), new Color(0, 0, 0)).tex();
		}
		for (int k = 0; k < MusicWnd.shi.length; ++k) {
			final int n2 = MusicWnd.shi[k];
			tips2[n2] = aa.render(s.substring(n2, n2 + 1), new Color(255, 255, 255)).tex();
		}
		keys = keys2;
		ikeys = ikeys2;
		tips = tips2;
	}


	public class CustomReceiver implements Receiver {

		public CustomReceiver() {

		}

		@Override
		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				//System.out.print("Channel: " + sm.getChannel() + " ");
				//SKIP PERCUSSION CHANNELS, MAYBE MAKE THIS TOGGLABLE?
				if (sm.getChannel() == 10 || sm.getChannel() == 11) {
					return;
				}
				if (sm.getCommand() == NOTE_ON) {
					int velocity = sm.getData2();
					if (velocity > 0) {
						keydown(getHafenKey(sm),System.currentTimeMillis());
					} else {
						keyup(getHafenKey(sm),System.currentTimeMillis());
					}

				} else if (sm.getCommand() == NOTE_OFF) {
					keyup(getHafenKey(sm),System.currentTimeMillis());
				} else {
					//System.out.println("Command:" + sm.getCommand());
				}
			} else {
				//System.out.println("Other message: " + message.getClass());
			}
		}

		public int getHafenKey(ShortMessage sm) {
			int key = sm.getData1();
			int octave = (key / 12)-1;
			int note = key % 12;
			while (octave > 5) {
				octave--;
			}
			while (octave < 3) {
				octave++;
			}
			//System.out.println(note);
			return note+(octave-3)*12;
		}

		@Override
		public void close() {

		}
	}

	public class HafenMidiplayer implements Runnable{
		MusicWnd musicWnd;
		public Receiver synthRcvr = new CustomReceiver();
		public Transmitter seqTrans;
		public Sequencer sequencer;
		public Sequence sequence;
		public boolean active = true;
		public boolean start = false;
		public boolean stop = false;
		public boolean kill = false;
		public boolean changedTempo = false;
		public boolean synchPlay = false;
		public long timeToPlay = 0;
		public String midiFile = "";
		public float tempo = 1f;
		public HafenMidiplayer (MusicWnd musicWnd) {
			this.musicWnd = musicWnd;
		}

		@Override
		public void run() {
			while (active) {
				if (start) {
					try {
						//System.out.println("Starting sequence..");
						sequence = MidiSystem.getSequence(new File(midiFile));
						sequencer = MidiSystem.getSequencer(false);
						seqTrans = sequencer.getTransmitter();
						seqTrans.setReceiver(synthRcvr);

						sequencer.open();
						sequencer.setSequence(sequence);
						sequencer.start();
						start = false;
					} catch (Exception e) {}
				}

				if (changedTempo && sequencer != null) {
					try {
						sequencer.setTempoFactor((float)tempo);
						changedTempo = false;
					} catch (Exception e) {
					}
				}
				if (stop) {
					try {
						sequencer.stop();
						stop = false;
					} catch (Exception e) {
					}
				}
				if (kill) {
					try {
						sequencer.stop();
						return;
					} catch (Exception e) {
					}
				}
				if (synchPlay) {
					try {
						System.out.println("SYNC PLAY " + midiFile + " in " + (timeToPlay-System.currentTimeMillis()) + " milliseconds");
						if (timeToPlay-System.currentTimeMillis() < 100 || timeToPlay-System.currentTimeMillis() > 1000) {
							ui.gui.error("Your clock is out of synch, go to windows clock internet time and update it with time.windows.com ");
							continue;
						}
						sequence = MidiSystem.getSequence(new File(midiFile));
						sequencer = MidiSystem.getSequencer(false);
						seqTrans = sequencer.getTransmitter();
						seqTrans.setReceiver(synthRcvr);

						sequencer.open();
						sequencer.setSequence(sequence);
						//wait until we should start
						Thread.sleep(timeToPlay-System.currentTimeMillis());
						sequencer.start();
						synchPlay = false;
					} catch (Exception e) {
					}
				}

				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
			}
		}


		public void startPlaying(String text) {
			midiFile = text;
			start = true;
		}
		public void stopPlaying() {
			stop = true;
		}

		public void kill() {kill = true;}

		public void setTempo(float tempo) {
			this.tempo = tempo;
			changedTempo = true;
		}

		public void synchPlay(long timeToPlay, String track) {
			this.midiFile = track;
			this.timeToPlay = timeToPlay;
			synchPlay = true;
		}

	}


}
