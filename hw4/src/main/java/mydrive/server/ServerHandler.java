package mydrive.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mydrive.protocol.*;
import mydrive.util.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private String clientId;
    private String storageDir;
    private Map<String, byte[]> serverFiles = new HashMap<>(); // храним имя и checksum

    public ServerHandler(String storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (msg instanceof ClientIdMessage) {
            ClientIdMessage m = (ClientIdMessage) msg;
            clientId = m.getClientId();
            String clientDir = storageDir + File.separator + clientId;
            FileUtils.getOrCreateDirectory(clientDir);
            System.out.println("Client connected: " + clientId);
            loadServerFiles(clientDir);

        } else if (msg instanceof FileListMessage) {
            FileListMessage m = (FileListMessage) msg;
            FileResponseMessage response = new FileResponseMessage();

            String clientDir = storageDir + File.separator + clientId;
            for (FileListMessage.FileInfo file : m.getFiles()) {
                byte[] serverChecksum = serverFiles.get(file.fileName);
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

            File file = new File(filePath);
            if (!file.exists()) {
                new FileOutputStream(filePath).close(); // creates an empty file
            }

            // пишет данные в файл
            try (FileOutputStream fos = new FileOutputStream(filePath, true)) {
                fos.write(m.getChunkData());
                fos.flush();
            }

            long expectedSize = m.getFileSize();
            if (file.length() >= expectedSize) {
                byte[] checksum = FileUtils.calculateMD5(file);
                serverFiles.put(m.getFileName(), checksum);
                System.out.println("File received: " + m.getFileName() + " (" + file.length() + " bytes)");
            }
        }
    }

    private void loadServerFiles(String clientDir) throws Exception {
        File dir = new File(clientDir);
        if (dir.exists()) {
            // только файлы (не директории)
            for (File file : dir.listFiles((d, name) -> new File(d, name).isFile())) {
                byte[] checksum = FileUtils.calculateMD5(file);
                serverFiles.put(file.getName(), checksum);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
