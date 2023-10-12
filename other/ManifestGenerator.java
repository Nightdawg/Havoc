package other;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ManifestGenerator {

    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final String BIN_FOLDER_NAME = "bin";

    public static void main(String[] args) {
        try {
            File binDir = new File(BIN_FOLDER_NAME);
            if (!binDir.exists()) {
                if (!binDir.mkdirs()) {
                    System.err.println("Failed to create bin directory");
                    return;
                }
            }
            File manifestFile = new File(binDir, MANIFEST_FILE_NAME);
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            JSONObject manifestJson = new JSONObject();
            processDirectory(binDir, binDir, sha256Digest, manifestJson);
            try (PrintWriter writer = new PrintWriter(new FileWriter(manifestFile))) {
                writer.write(manifestJson.toString(4)); // 4 is the number of spaces for indentation
            }
            System.out.println("Manifest file generated successfully: " + manifestFile.getAbsolutePath());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void processDirectory(File baseDir, File currentDir, MessageDigest sha256Digest, JSONObject manifestJson) throws IOException {
        File[] files = currentDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(baseDir, file, sha256Digest, manifestJson);
            } else {
                String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
                String fileHash = hashFile(file, sha256Digest);
                JSONObject fileJson = new JSONObject();
                fileJson.put("hash", fileHash);
                manifestJson.put(relativePath, fileJson);
            }
        }
    }

    private static String hashFile(File file, MessageDigest digest) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] hashedBytes = digest.digest(fileBytes);
        return bytesToHex(hashedBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
