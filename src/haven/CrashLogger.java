package haven;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final int CRASH_EXIT_CODE = 1337;

    private static final Set<String> EXCLUDED_THREADS = new HashSet<>(
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
                    "OceanShorelineScout",
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
        if (EXCLUDED_THREADS.contains(t.getName())) {
            e.printStackTrace();
            logCrash(e);
            return;
        }

        logCrash(e);

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "A critical error occurred:\n" + e.toString(),
                "Application Crash", JOptionPane.ERROR_MESSAGE));

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        System.exit(CRASH_EXIT_CODE);
    }

    private void logCrash(Throwable throwable) {
        File logDir = new File("Logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }

        String logFilename = String.format("crash_log_%tF_%<tH%<tM%<tS.txt", System.currentTimeMillis());

        File logFile = new File(logDir, logFilename);

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(logFile))) {
            throwable.printStackTrace(writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
