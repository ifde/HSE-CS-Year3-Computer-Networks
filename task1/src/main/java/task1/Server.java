package task1;

import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

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

            byte[] buffer = new byte[1024 * 64]; // 64KB


            while (true) {
                if (in.available() > 0) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) break;
                    
                    String answer = LocalDateTime.now().format(fmt);
                    System.out.println("Got " + bytesRead + " bytes.");

                    out.println(answer);
                }
            }
        } catch (Exception e) {
            ;
        }
    }
}