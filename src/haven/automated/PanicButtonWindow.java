package haven.automated;

import haven.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class PanicButtonWindow extends Window {
    private GameUI gui;
    public PanicButtonWindow(GameUI gui) {
        super(UI.scale(140, 50), "Ping everyone?");
        this.gui = gui;

        add(new Button(UI.scale(70), "YES") {
            @Override
            public void click() {
                contactDiscordBot();
                notifyVillageChat();
                gui.panicButtonWindow.stop();
            }
        }, UI.scale(35,15));
    }

    public void contactDiscordBot(){
        if(!OptWnd.panicButtonApiTokenTextEntry.text().equals("") && !OptWnd.panicButtonDiscordChannelIDTextEntry.text().equals("")
                && !OptWnd.panicButtonDiscordMessageTextEntry.text().equals("") && !OptWnd.panicButtonYourNicknameTextEntry.text().equals("")) {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("message", OptWnd.panicButtonDiscordMessageTextEntry.text());
            jsonPayload.put("channel_id", OptWnd.panicButtonDiscordChannelIDTextEntry.text());
            jsonPayload.put("username", OptWnd.panicButtonYourNicknameTextEntry.text());
            try {
                URL apiUrl = new URL("https://discord.havocandhearth.net/send_message");
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("x-security-token", OptWnd.panicButtonApiTokenTextEntry.text());
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    gui.msg("Discord notification sent.");
                } else {
                    gui.error("Failed to send discord message. Contact client devs...");
                }
                connection.disconnect();
            } catch (IOException e) {
                gui.error("Failed to send message. Contact client devs.");
            }
        }
    }

    public void notifyVillageChat(){
        if(!OptWnd.panicButtonVillageNameTextEntry.text().equals("")) {
            try {
                Map<String, ChatUI.MultiChat> chats = gui.chat.getMultiChannels();
                for(String name : chats.keySet()){
                    if(name.equals(OptWnd.panicButtonVillageNameTextEntry.text())){

                        String message = OptWnd.panicButtonDiscordMessageTextEntry.text().equals("") ? "Need Help (Panic Button)" : OptWnd.panicButtonDiscordMessageTextEntry.text();
                        if(!OptWnd.panicButtonYourNicknameTextEntry.text().equals("")){{
                            message = OptWnd.panicButtonYourNicknameTextEntry.text().concat(": " + message);
                        }}
                        ChatUI.MultiChat village = chats.get(name);
                        village.wdgmsg("msg", message);
                    }
                }
            } catch (Exception ignored){}
        }
    }


    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop();
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        reqdestroy();
        gui.panicButtonWindow = null;
    }
}
