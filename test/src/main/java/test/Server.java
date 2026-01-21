package test;

import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Server {
    public static void main(String[] args) {
        int port = 10000;
        System.out.println("test");
        try (ServerSocket ss = new ServerSocket(port)) {
            Socket s = ss.accept();

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream());

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

            System.out.println("Server created, yes!");

            while (in.readLine() != null) {
                String answer = LocalDateTime.now().format(fmt);

                System.out.println("Got a message from a client");

                out.println(answer);
                out.flush();
            }
        } catch (Exception e) {
            ;
        }
    }
}