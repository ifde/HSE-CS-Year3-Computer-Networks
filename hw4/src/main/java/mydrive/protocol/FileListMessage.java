package mydrive.protocol;

import java.util.ArrayList;
import java.util.List;

public class FileListMessage implements Message {
    public static class FileInfo {
        public String fileName;
        public long fileSize;
        public byte[] checksum;

        public FileInfo(String fileName, long fileSize, byte[] checksum) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.checksum = checksum;
        }
    }

    private List<FileInfo> files = new ArrayList<>();

    public void addFile(String fileName, long fileSize, byte[] checksum) {
        files.add(new FileInfo(fileName, fileSize, checksum));
    }

    public List<FileInfo> getFiles() {
        return files;
    }
}
