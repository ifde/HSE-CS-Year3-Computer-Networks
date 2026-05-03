package mydrive.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import mydrive.protocol.*;
import mydrive.util.FileUtils;

import java.io.*;
import java.util.*;

public class MyDriveClientParallel {
    private String host;
    private int port;
    private String localDir;
    private String clientId;
    private int maxConnections;
    private long startTime;
    private long endTime;
    private EventLoopGroup group;

    public MyDriveClientParallel(String host, int port, String localDir, String clientId, int maxConnections) {
        this.host = host;
        this.port = port;
        this.localDir = localDir;
        this.clientId = clientId;
        this.maxConnections = maxConnections;
    }

    public void sync() throws Exception {
        startTime = System.currentTimeMillis();
        group = new NioEventLoopGroup();

        Channel channel = connect();
        try {
            sendClientId(channel);
            Thread.sleep(300);

            Map<String, FileInfo> localFiles = scanLocalDirectory();
            sendFileList(channel, localFiles);
            Thread.sleep(300);

            List<String> missingFiles = waitForResponse(channel);
            sendMissingFiles(channel, localFiles, missingFiles);
            
            Thread.sleep(2000);

        } finally {
            channel.close().sync();
            group.shutdownGracefully().sync();
        }

        endTime = System.currentTimeMillis();
        System.out.println("Sync completed in " + (endTime - startTime) + " ms");
    }

    private Channel connect() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MessageDecoder());
                        pipeline.addLast(new MessageEncoder());
                        pipeline.addLast(new ClientHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        return future.channel();
    }

    private void sendClientId(Channel channel) {
        ClientIdMessage msg = new ClientIdMessage(clientId);
        channel.writeAndFlush(msg);
        System.out.println("Sent client ID: " + clientId);
    }

    private Map<String, FileInfo> scanLocalDirectory() throws Exception {
        Map<String, FileInfo> files = new HashMap<>();
        File dir = new File(localDir);

        if (!dir.exists()) {
            System.out.println("Local directory does not exist");
            return files;
        }

        for (File file : dir.listFiles((d, name) -> new File(d, name).isFile())) {
            byte[] checksum = FileUtils.calculateMD5(file);
            files.put(file.getName(), new FileInfo(file.getName(), file.length(), checksum));
        }

        System.out.println("Scanned " + files.size() + " files");
        return files;
    }

    private void sendFileList(Channel channel, Map<String, FileInfo> files) {
        FileListMessage msg = new FileListMessage();
        for (FileInfo file : files.values()) {
            msg.addFile(file.name, file.size, file.checksum);
        }
        channel.writeAndFlush(msg);
        System.out.println("Sent file list with " + files.size() + " files");
    }

    private List<String> waitForResponse(Channel channel) throws InterruptedException {
        ClientHandler handler = channel.pipeline().get(ClientHandler.class);
        long maxWait = 5000;
        long elapsed = 0;
        while (!handler.isResponseReceived() && elapsed < maxWait) {
            Thread.sleep(100);
            elapsed += 100;
        }
        return handler.getMissingFiles();
    }

    private void sendMissingFiles(Channel channel, Map<String, FileInfo> localFiles, List<String> missingFiles) throws Exception {
        System.out.println("Sending " + missingFiles.size() + " files");
        for (String fileName : missingFiles) {
            FileInfo file = localFiles.get(fileName);
            if (file != null) {
                sendFile(channel, new File(localDir, fileName), file.size);
                Thread.sleep(100);
            }
        }
    }

    private void sendFile(Channel channel, File file, long expectedSize) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                FileChunkMessage msg = new FileChunkMessage(file.getName(), expectedSize, chunk);
                channel.writeAndFlush(msg).sync();
            }
        }
        System.out.println("Sent file: " + file.getName() + " (" + file.length() + " bytes)");
    }

    public static void main(String[] args) throws Exception {
        String configFile = "config.properties";
        if (args.length > 0) {
            configFile = args[0];
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }

        String host = props.getProperty("server.host", "localhost");
        int port = Integer.parseInt(props.getProperty("server.port", "9999"));
        String localDir = props.getProperty("local.dir", "./sync");
        String clientId = props.getProperty("client.id", "client1");
        int maxConnections = Integer.parseInt(props.getProperty("max.connections", "4"));

        new MyDriveClientParallel(host, port, localDir, clientId, maxConnections).sync();
    }

    public static class FileInfo {
        String name;
        long size;
        byte[] checksum;

        FileInfo(String name, long size, byte[] checksum) {
            this.name = name;
            this.size = size;
            this.checksum = checksum;
        }
    }
}
