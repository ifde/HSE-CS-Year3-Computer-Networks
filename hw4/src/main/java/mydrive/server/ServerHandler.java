package mydrive.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mydrive.protocol.*;
import mydrive.util.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private String clientId;
    private String storageDir;
    private static final Map<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, byte[]>> CLIENT_FILES = new ConcurrentHashMap<>();

    public ServerHandler(String storageDir) {
        this.storageDir = storageDir;
    }

    private Object getLockForFile(String filePath) {
        return FILE_LOCKS.computeIfAbsent(filePath, k -> new Object());
    }

    private Map<String, byte[]> getClientFileCache(String clientId) {
        return CLIENT_FILES.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (msg instanceof ClientIdMessage) {
            ClientIdMessage m = (ClientIdMessage) msg;
            clientId = m.getClientId();
            String clientDir = storageDir + File.separator + clientId;
            FileUtils.getOrCreateDirectory(clientDir);
            System.out.println("Client connected: " + clientId);
            loadServerFiles(clientId, clientDir);

        } else if (msg instanceof FileListMessage) {
            FileListMessage m = (FileListMessage) msg;
            FileResponseMessage response = new FileResponseMessage();

            Map<String, byte[]> clientFileCache = getClientFileCache(clientId);
            for (FileListMessage.FileInfo file : m.getFiles()) {
                byte[] serverChecksum = clientFileCache.get(file.fileName);
                if (serverChecksum == null || !Arrays.equals(serverChecksum, file.checksum)) {
                    response.addMissingFile(file.fileName);
                }
            }

            ctx.writeAndFlush(response);
            System.out.println("Sent file response for client: " + clientId + ", missing: " + response.getMissingFiles().size());

        } else if (msg instanceof FileChunkMessage) {
            FileChunkMessage m = (FileChunkMessage) msg;
            String clientDir = storageDir + File.separator + clientId;
            String filePath = clientDir + File.separator + m.getFileName();

            synchronized (getLockForFile(filePath)) {
                File file = new File(filePath);
                if (!file.exists()) {
                    new FileOutputStream(filePath).close();
                }

                try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
                    fos.write(m.getChunkData());
                    fos.flush();
                }

                long expectedSize = m.getFileSize();
                if (file.length() >= expectedSize) {
                    byte[] checksum = FileUtils.calculateMD5(file);
                    Map<String, byte[]> clientFileCache = getClientFileCache(clientId);
                    clientFileCache.put(m.getFileName(), checksum);
                    System.out.println("File received: " + m.getFileName() + " (" + file.length() + " bytes)");
                }
            }
        }
    }

    private void loadServerFiles(String clientId, String clientDir) throws Exception {
        File dir = new File(clientDir);
        Map<String, byte[]> clientFileCache = getClientFileCache(clientId);
        if (dir.exists()) {
            for (File file : dir.listFiles((d, name) -> new File(d, name).isFile())) {
                byte[] checksum = FileUtils.calculateMD5(file);
                clientFileCache.put(file.getName(), checksum);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
