package haven;

import haven.resutil.Curiosity;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class StudydeskInfo extends Widget {

    private static final Color CURIOTARGET = new Color(0, 230, 0);
    private static final Color CURIOLOW = new Color(230, 50, 50);
    private static final Color CURIOHIGH = new Color(0, 130, 0);

    public Inventory studyInv;
    public int curiocount = 0;

    public Label curiosliderlabel, studyhours;
    public HSlider curioslider;

    private Collection curios = new ArrayList(); //add curios from tables to this before parsing
    //private Collection finalCurios = new ArrayList(); //parsed out list for checking against the curios you should be studying from Config.curiolist
    private Collection curioCounter = new ArrayList(); //used to see if the number of curios on the table changes to redraw the addons

    private static int currentSliderValue = Utils.getprefi("curiotimetarget", 600);


    public StudydeskInfo(int x, int y, Inventory studyInv) {
        this.sz = new Coord(x, y);
        this.studyInv = studyInv;

        curiosliderlabel = add(new Label("Curio Time Target:"), UI.scale(new Coord(0, 55)));
        studyhours = add(new Label(""), UI.scale(new Coord(110, 55)));

        curioslider = add(new HSlider(x-UI.scale(10), 0, 10080, currentSliderValue) {
            public void added() {
                updateLabel();
            }

            protected void attach(UI ui) {
                super.attach(ui);
                val = currentSliderValue;
            }

            public void changed() {
                Utils.setprefi("curiotimetarget", val);
                currentSliderValue = val;
                updateLabel();
            }

            private void updateLabel() {
                int days = (int)Math.floor((val / 60d) / 24d);
                int hours = (val / 60) - (days * 24);
                studyhours.settext(String.format(days == 1 ? (hours == 1 ? "%d Day and %d Hour" : "%d Day and %d Hours") : (hours == 1 ? "%d Days and %d Hour" : "%d Days and %d Hours"), days, hours));
            }
        }, UI.scale(new Coord(0, 75)));

    }

    private static final Coord totalLPCoord = UI.scale(new Coord(0, 15));
    private static final Coord totalAttentionCoord = UI.scale(new Coord(0, 0));


    public void draw(GOut g) {
        super.draw(g);
        int sizeY = 0;
        int y = UI.scale(40);
        int totalLP = 0;
        int totalAttn = 0;
        HashMap<String, Double> studyTimes = new HashMap<String, Double>();
        HashMap<String, Integer> AttnTotal = new HashMap<String, Integer>();
        List<Curio> curiolist = new ArrayList<>();
        for (WItem wItem : studyInv.getAllItems()) {
            try {
                Curiosity ci = ItemInfo.find(Curiosity.class, wItem.item.info());
                totalLP += ci.exp;
                        curiolist.add(new Curio(wItem.item.getname(),
                                studyTimes.get(wItem.item.getname()) == null ? wItem.item.studytime : studyTimes.get(wItem.item.getname()) + wItem.item.studytime,
                                ci.exp));
                studyTimes.put(wItem.item.getname(), studyTimes.get(wItem.item.getname()) == null ? wItem.item.studytime : studyTimes.get(wItem.item.getname()) + wItem.item.studytime);
                AttnTotal.put(wItem.item.getname(), AttnTotal.get(wItem.item.getname()) == null ? ci.mw : AttnTotal.get(wItem.item.getname()));
            } catch (NullPointerException | Loading qq) {
            }
        }
        g.image(Text.render("Total LP: " + String.format("%,d", totalLP)).tex(), totalLPCoord);

        List<Map.Entry<String, Integer>> lst2 = AttnTotal.entrySet().stream().sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).collect(Collectors.toList());
        for (Map.Entry<String, Integer> entry : lst2) {
            totalAttn += entry.getValue();
        }
        g.image(Text.render("Total Attention: " + String.format("%,d", totalAttn)).tex(), totalAttentionCoord);

        //iterates the curio list to only print out total study times for unique curios
        List<Map.Entry<String, Double>> lst = studyTimes.entrySet().stream().sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).collect(Collectors.toList());
        for (Map.Entry<String, Double> entry : lst) {
            curioCounter.add(entry.getKey());
            int LP = 0;
            for (Curio c : curiolist) {
                if (c.curioName.equals(entry.getKey()))
                    LP += c.lpgain;
            }
            String imageText = entry.getKey() + ": " + sensibleTimeFormat(entry.getValue()) + " - " + sensibleLPFormat(LP);
            Coord imageCoord = new Coord(UI.scale(10), y);
            if (entry.getValue() > currentSliderValue * 3) {
                g.image(Utils.outline2(Text.render(imageText, CURIOHIGH).img, Color.BLACK), imageCoord);
            } else if (entry.getValue() < currentSliderValue) {
                g.image(Utils.outline2(Text.render(imageText, CURIOLOW).img, Color.BLACK), imageCoord);
            } else {
                g.image(Utils.outline2(Text.render(imageText, CURIOTARGET).img, Color.BLACK), imageCoord);
            }
            y += UI.scale(15);
            sizeY += UI.scale(15);
            curios.add(entry.getKey());
        }

        if (curiocount != curioCounter.size()) {

            studyhours.move(new Coord(UI.scale(110), y + UI.scale(15)));
            curiosliderlabel.move(new Coord(0, y + UI.scale(15)));
            curioslider.move(new Coord(0, y + UI.scale(39)));

            sizeY += UI.scale(100);
            resize(this.sz.x, sizeY);

            parent.pack();
        }

    }


    // Input time as minutes
    String sensibleTimeFormat(Double time) {
        StringBuilder sb = new StringBuilder();
        int days = new Double(time / 1440).intValue();
        time -= days * 1440;
        int hours = new Double(time / 60).intValue();
        time -= hours * 60;
        int minutes = time.intValue();
        if (days > 0) {
            sb.append(days + "d ");
        }
        sb.append(hours + "h ");
        sb.append(minutes + "m");
        return sb.toString();
    }

    String sensibleLPFormat(int LP) {
        StringBuilder sb = new StringBuilder();
        int thousands = new Double(LP / 1000).intValue();

        if (thousands > 0) {
            sb.append(thousands + "k LP");
        } else
            sb.append(LP + " LP");
        return sb.toString();
    }

    public class Curio {
        private String curioName;
        private double studyTime;
        private int lpgain;

        public Curio(String curioName, double studyTime, int lpgain) {
            this.curioName = curioName;
            this.studyTime = studyTime;
            this.lpgain = lpgain;
        }
    }
}
