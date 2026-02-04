package task1;

import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.nio.ByteBuffer;

public class Server {
    public static void main(String[] args) {
        int port = 10000;
        System.out.println("Server created, yes!");
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReceiveBufferSize(16384);
            Socket s = ss.accept();

            InputStream in = new BufferedInputStream(s.getInputStream());
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

            while (true) {
                // Let's find out what is the size of the buffer
                byte[] lengthBytes = new byte[4];
                int read = readFully(in, lengthBytes, 4);
                int dataLength = ByteBuffer.wrap(lengthBytes).getInt();

                // Then we will read only the buffer size
                byte[] buffer = new byte[dataLength];
                read = readFully(in, buffer, dataLength);

                String answer = LocalDateTime.now().format(fmt);
                System.out.println("Got " + dataLength + " bytes.");

                out.println(answer);
            }
        } catch (Exception e) {
            ;
        }
    }

    // This function just reads exactly "len" bytes from the input stream
    public static int readFully(InputStream in, byte[] b, int len) throws IOException {
        int total = 0;
        while (total < len) {
            // (len - total) is the maximum number of bytes to read
            // r is the actual number of bytes read
            int r = in.read(b, total, len - total);
            if (r == -1) return total;
            total += r;
        }
        return total;
    }
}