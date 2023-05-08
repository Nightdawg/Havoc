package haven;

import haven.render.Abortable;
import haven.render.MixColor;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

public class ColorOptionWidget extends Widget{
    ColorOptionButton cb;
    Label label;
    Color currentColor;
    Consumer<Color> function;

    public ColorOptionWidget(String text, String key, int horizontalButtonDistance, int Red, int Green, int Blue, int Alpha, Consumer<Color> function){
        cb = new ColorOptionButton(key, UI.scale(22), UI.scale(22), Red, Green, Blue, Alpha);
        label = new Label(text);
        this.function = function;
        add(label, new Coord(0,4));
        add(cb, new Coord(UI.scale(horizontalButtonDistance),0));
        pack();
    }

    public class ColorOptionButton extends Button {
        String clr;
        JColorChooser colorChooser;
        public ColorOptionButton(String key, int w , int h, int R, int G, int B, int Alpha){
            super(w,"");
            sz.y = h;
            this.clr = key;
            this.colorChooser = new JColorChooser();
            final AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
            for (final AbstractColorChooserPanel accp : panels) {
                if (!accp.getDisplayName().equals("RGB")) {
                    colorChooser.removeChooserPanel(accp);
                }
            }
            colorChooser.setPreviewPanel(new JPanel());
            String[] savedColorSetting = Utils.getprefsa(clr + "_colorSetting", new String[]{String.valueOf(R), String.valueOf(G), String.valueOf(B), String.valueOf(Alpha)});
            colorChooser.setColor(currentColor = new Color(Integer.parseInt(savedColorSetting[0]), Integer.parseInt(savedColorSetting[1]), Integer.parseInt(savedColorSetting[2]), Integer.parseInt(savedColorSetting[3])));
        }

        @Override
        public void draw(GOut g) {
            int delta = 2;
            Coord size = new Coord(sz.x-2*delta,sz.y-2*delta);
            g.chcolor(currentColor);
            g.frect(new Coord(delta,  delta), size);
            g.chcolor();
            g.chcolor(Color.BLACK);
            g.frect(new Coord(0,0), new Coord(sz.x,delta));
            g.frect(new Coord(0,sz.y-delta), new Coord(sz.x,delta));
            g.frect(new Coord(0,delta), new Coord(delta,size.y));
            g.frect(new Coord(sz.x-delta,delta), new Coord(delta,size.y));
            g.chcolor();
        }

        @Override
        public void click() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JDialog chooser = JColorChooser.createDialog(null, "SelectColor", true, colorChooser, new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            currentColor = colorChooser.getColor();
                            Utils.setprefsa(clr + "_colorSetting", new String[]{String.valueOf(currentColor.getRed()), String.valueOf(currentColor.getGreen()), String.valueOf(currentColor.getBlue()), String.valueOf(currentColor.getAlpha())});
                        }
                    }, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                        }
                    });
                    chooser.setVisible(true);
                    function.accept(currentColor);
                }
            }).start();
        }
    }
}
