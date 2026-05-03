package mydrive.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        if (msg instanceof ClientIdMessage) {
            ClientIdMessage m = (ClientIdMessage) msg;
            out.writeByte(MessageType.CLIENT_ID);
            out.writeShort(m.getClientId().length());
            out.writeBytes(m.getClientId().getBytes());
            
        } else if (msg instanceof FileListMessage) {
            FileListMessage m = (FileListMessage) msg;
            out.writeByte(MessageType.FILE_LIST);
            out.writeShort(m.getFiles().size());
            for (FileListMessage.FileInfo file : m.getFiles()) {
                out.writeShort(file.fileName.length());
                out.writeBytes(file.fileName.getBytes());
                out.writeLong(file.fileSize);
                out.writeBytes(file.checksum);
            }
            
        } else if (msg instanceof FileResponseMessage) {
            FileResponseMessage m = (FileResponseMessage) msg;
            out.writeByte(MessageType.FILE_RESPONSE);
            out.writeShort(m.getMissingFiles().size());
            for (String fileName : m.getMissingFiles()) {
                out.writeShort(fileName.length());
                out.writeBytes(fileName.getBytes());
            }
            
        } else if (msg instanceof FileChunkMessage) {
            FileChunkMessage m = (FileChunkMessage) msg;
            out.writeByte(MessageType.FILE_CHUNK);
            out.writeShort(m.getFileName().length());
            out.writeBytes(m.getFileName().getBytes());
            out.writeLong(m.getFileSize());
            out.writeBytes(m.getChunkData());
        }
    }
}
