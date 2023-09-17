package haven;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class CraftWindow extends Window {
	private static final IBox frame = new IBox("gfx/hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
	private final TabStrip tabStrip;
	private final Map<MenuGrid.Pagina, TabStrip.Button> tabs = new HashMap<MenuGrid.Pagina, TabStrip.Button>();
	public Makewindow makeWidget;
	private MenuGrid.Pagina lastAction;

	public CraftWindow() {
		super(Coord.z, "Crafting");
		tabStrip = add(new TabStrip() {
			protected void selected(Button button) {
				for (Map.Entry<MenuGrid.Pagina, Button> entry : tabs.entrySet()) {
					MenuGrid.Pagina pagina = entry.getKey();
					if (entry.getValue().equals(button) && pagina != lastAction) {
						ui.gui.wdgmsg("act", (Object[])pagina.act().ad);
						lastAction = null;
						break;
					}
				}
			}
		});
		//setLocal(true);
		//setHideOnClose(true);
		setfocusctl(true);
	}

	@Override
	protected void added() { // ND: Do this override to avoid preventDraggingOutside(), that function should not be called for the Craft Window
		parent.setfocus(this);
	}

	public void setLastAction(MenuGrid.Pagina value) {
		lastAction = value;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if((sender == this) && (msg == "close")) {
			hide();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	@Override
	public <T extends Widget> T add(T child) {
		child = super.add(child);
		if (child instanceof Makewindow) {
			if (lastAction != null) {
				addTab(lastAction);
			}
			makeWidget = (Makewindow) child;
			makeWidget.c = new Coord(5, tabStrip.sz.y + 5);
			makeWidget.resize(Math.max(makeWidget.sz.x, tabStrip.sz.x), makeWidget.sz.y);
		}
		return child;
	}

	@Override
	public void cdestroy(Widget w) {
		if (makeWidget == w) {
			makeWidget = null;
			if (visible)
				hide();
		}
	}

	@Override
	public void cdraw(GOut g) {
		super.cdraw(g);
		frame.draw(g, new Coord(0, Math.max(0, tabStrip.sz.y - 1)), csz().sub(0, tabStrip.sz.y));
	}

	@Override
	public void resize(Coord sz) {
		super.resize(sz.add(5, 5));
	}

	public boolean globtype(char ch, KeyEvent ev) {
		if(visible && ev.getKeyCode() == KeyEvent.VK_TAB && tabStrip.getButtonCount() > 0) {
			int nextIndex = (tabStrip.getSelectedButtonIndex() + 1) % tabStrip.getButtonCount();
			tabStrip.select(nextIndex);
			if(hasfocus)
				return true;
		}
		return super.globtype(ch, ev);
	}

	@Override
	public void hide() {
		super.hide();
		if (makeWidget != null)
			makeWidget.wdgmsg("close");
	}


	private void addTab(MenuGrid.Pagina pagina) {
		if (tabs.containsKey(pagina)) {
			TabStrip.Button old = tabs.get(pagina);
			tabStrip.remove(old);
		}
		Tex icon = new TexI(PUtils.convolvedown(lastAction.res.get().layer(Resource.imgc).img, UI.scale(new Coord(26, 26)), CharWnd.iconfilter));
		String text = "";
//		if (text.length() > 12)
//			text = text.substring(0, 12 - 2) + "..";
		TabStrip.Button added = tabStrip.insert(0, icon, text, lastAction.act().name);
		tabStrip.select(added);
		if (tabStrip.getButtonCount() > 12) {
			removeTab(tabStrip.getButtonCount() - 1);
		}
		tabs.put(lastAction, added);
	}

	private void removeTab(int index) {
		TabStrip.Button removed = tabStrip.remove(index);
		tabs.values().remove(removed);
	}
}
