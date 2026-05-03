package mydrive.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mydrive.protocol.FileResponseMessage;
import mydrive.protocol.Message;

import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    private List<String> missingFiles = new ArrayList<>();
    private volatile boolean responseReceived = false;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (msg instanceof FileResponseMessage) {
            FileResponseMessage response = (FileResponseMessage) msg;
            missingFiles = response.getMissingFiles();
            responseReceived = true;
            System.out.println("Received missing files list: " + missingFiles);
        }
    }

    public List<String> getMissingFiles() {
        return missingFiles;
    }

    public boolean isResponseReceived() {
        return responseReceived;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
