package test;

import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Client {
    public static void main(String[] args) {
        int port = 10000;

        try {
            Socket s = new Socket("127.0.0.1", port);

            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream());

            for (int i = 0; i < 10; ++i) {
                out.println("random");
                out.flush();

                String serverAnswer = in.readLine();
                System.out.println("Got a reply: " + serverAnswer);

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            ;
        }
    }
}