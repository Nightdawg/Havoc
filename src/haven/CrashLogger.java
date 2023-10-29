package haven;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.net.URL;
import org.json.JSONObject;

public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final int CRASH_EXIT_CODE = 1337;

    public static final Set<String> EXCLUDED_THREADS = new HashSet<>(
            Arrays.asList(
                    "Add12Coal",
                    "Add9Coal",
                    "AggroNearestTarget",
                    "AutoTunneler",
                    "CheckpointManager",
                    "ClickNearestGate",
                    "CleanupBot",
                    "CloverScript",
                    "CoracleScript",
                    "DropItemsFromEnemy",
                    "EquipFromBelt",
                    "FishingBot",
                    "HarvestNearestDreamcatcher",
                    "InteractWithNearestObject",
                    "miningSafetyAssistantThread",
                    "OceanScoutBot",
                    "OreAndStoneCounter",
                    "Pathfinder",
                    "RecipeCollectorThread",
                    "RefillWaterContainers",
                    "Reaggro",
                    "TarKilnEmptierBot",
                    "TurnipBot"
            )
    );

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String stackTrace = getStackTraceAsString(e);

        // Always log the crash to a file, regardless of HTTP success/failure
        logCrash(t, stackTrace);

        // Always attempt to report the crash via HTTP
        reportCrash(MainFrame.username, Config.clientVersion, stackTrace, !EXCLUDED_THREADS.contains(t.getName()));

        if (!EXCLUDED_THREADS.contains(t.getName())) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "A critical error occurred:\n" + e.toString(),
                    "Application Crash", JOptionPane.ERROR_MESSAGE));

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {CrashLogger.logCrash(Arrays.toString(e.getStackTrace()));}

            System.exit(CRASH_EXIT_CODE);
        }
    }

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static void logCrash(String stackTrace) {
        logCrash(null, stackTrace);
    }

    private static void logCrash(Thread t, String stackTrace) {
        File logDir = new File("Logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }

        String logFilename = String.format("crash_log_%tF_%<tH%<tM%<tS.txt", System.currentTimeMillis());
        File logFile = new File(logDir, logFilename);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(logFile))) {
            if (t != null) {
                writer.println("Crash in thread: " + t.getName());
            }
            writer.println(stackTrace);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static void reportCrash(String username, String version, String log, boolean mainThread) {
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("username", username);
        jsonPayload.put("version", version);
        jsonPayload.put("log", log);
        jsonPayload.put("main_thread", mainThread);

        try {
            URL apiUrl = new URL("https://logs.havocandhearth.net/crash-log/create");
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-security-token", "d3f72d7bb40e38b6e8c1ebe8e");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_ACCEPTED) {
                System.out.println("Crash logged.");
            } else {
                System.out.println("Crash logged..");
            }
            connection.disconnect();
        } catch (IOException e) {
            System.out.println("Crash logged...");
        }
    }
}
