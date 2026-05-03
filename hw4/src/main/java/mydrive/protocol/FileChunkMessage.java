package mydrive.protocol;

public class FileChunkMessage implements Message {
    private String fileName;
    private long fileSize;
    private byte[] chunkData;

    public FileChunkMessage(String fileName, long fileSize, byte[] chunkData) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunkData = chunkData;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public byte[] getChunkData() {
        return chunkData;
    }
}
