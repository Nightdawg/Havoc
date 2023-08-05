package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoFlowerRepeater implements Runnable{
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static String option;
    private GameUI gui;
    private boolean stop = false;
    private int ping;
    private final String name;
    private List<GItem> items;

    public AutoFlowerRepeater(GameUI gui, String name) {
        scheduler.schedule(() -> stop = true, 1500, TimeUnit.MILLISECONDS);
        this.gui = gui;
        this.ping = GameUI.getPingValue() != null ? GameUI.getPingValue() + 10 : 100;
        option = "";
        this.name = name;
        items = iterateThroughItems(name);
    }

    public List<GItem> iterateThroughItems(String name){
        List<GItem> items = new ArrayList<>();
        for(WItem item : gui.maininv.getAllItems()){
            if(item.item != null && item.item.getres() != null && item.item.getres().name.equals(name)){
                items.add(item.item);
            }
        }
        return items;
    }

    @Override
    public void run() {
        while(!stop){
            if(option != null && !option.equals("")){
                scheduler.shutdown();
                int counter = 0;
                while(!items.isEmpty() && counter <= 4){
                    counter++;
                    for(GItem item : items){
                        FlowerMenu.setNextSelection(option);
                        item.wdgmsg("iact", Coord.z, 3);
                        ping = GameUI.getPingValue() != null ? GameUI.getPingValue() + 10 : 100;
                    }
                    items = iterateThroughItems(name);
                }
                stop = true;
            }
            sleep(ping);
        }
        option = null;
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {}
    }
}
