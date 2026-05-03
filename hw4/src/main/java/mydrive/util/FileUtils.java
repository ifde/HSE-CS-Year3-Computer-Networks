package mydrive.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class FileUtils {
    private static final int BUFFER_SIZE = 8192;

    public static byte[] calculateMD5(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        return digest.digest();
    }

    public static File getOrCreateDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
