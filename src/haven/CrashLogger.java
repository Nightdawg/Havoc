package haven;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final int CRASH_EXIT_CODE = 1337;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
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
