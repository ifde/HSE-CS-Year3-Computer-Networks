package mydrive.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();
        int messageType = in.readByte();
        
        if (messageType == MessageType.CLIENT_ID) {
            if (in.readableBytes() < 3) {
                in.resetReaderIndex();
                return;
            }
            int idLength = in.readShort();
            if (in.readableBytes() < idLength) {
                in.resetReaderIndex();
                return;
            }
            byte[] idBytes = new byte[idLength];
            in.readBytes(idBytes);
            String clientId = new String(idBytes);
            out.add(new ClientIdMessage(clientId));
            
        } else if (messageType == MessageType.FILE_LIST) {
            if (in.readableBytes() < 3) {
                in.resetReaderIndex();
                return;
            }
            int fileCount = in.readShort();
            FileListMessage msg = new FileListMessage();
            
            for (int i = 0; i < fileCount; i++) {
                if (in.readableBytes() < 2) {
                    in.resetReaderIndex();
                    return;
                }
                int fileNameLength = in.readShort();
                if (in.readableBytes() < fileNameLength + 8 + 16) {
                    in.resetReaderIndex();
                    return;
                }
                byte[] fileNameBytes = new byte[fileNameLength];
                in.readBytes(fileNameBytes);
                String fileName = new String(fileNameBytes);
                long fileSize = in.readLong();
                byte[] checksum = new byte[16];
                in.readBytes(checksum);
                
                msg.addFile(fileName, fileSize, checksum);
            }
            out.add(msg);
            
        } else if (messageType == MessageType.FILE_RESPONSE) {
            if (in.readableBytes() < 3) {
                in.resetReaderIndex();
                return;
            }
            int missingCount = in.readShort();
            FileResponseMessage msg = new FileResponseMessage();
            
            for (int i = 0; i < missingCount; i++) {
                if (in.readableBytes() < 2) {
                    in.resetReaderIndex();
                    return;
                }
                int fileNameLength = in.readShort();
                if (in.readableBytes() < fileNameLength) {
                    in.resetReaderIndex();
                    return;
                }
                byte[] fileNameBytes = new byte[fileNameLength];
                in.readBytes(fileNameBytes);
                String fileName = new String(fileNameBytes);
                msg.addMissingFile(fileName);
            }
            out.add(msg);
            
        } else if (messageType == MessageType.FILE_CHUNK) {
            if (in.readableBytes() < 11) {
                in.resetReaderIndex();
                return;
            }
            int fileNameLength = in.readShort();
            if (in.readableBytes() < fileNameLength + 8) {
                in.resetReaderIndex();
                return;
            }
            byte[] fileNameBytes = new byte[fileNameLength];
            in.readBytes(fileNameBytes);
            String fileName = new String(fileNameBytes);
            long fileSize = in.readLong();
            
            int chunkLength = in.readableBytes();
            if (chunkLength > 0) {
                byte[] chunkData = new byte[chunkLength];
                in.readBytes(chunkData);
                out.add(new FileChunkMessage(fileName, fileSize, chunkData));
            }
        }
    }
}
