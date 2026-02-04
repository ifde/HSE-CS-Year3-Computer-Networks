package edu.hse.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 1000;
    // private static final String HOST = "rya.nc";
    // private static final int PORT = 1987;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PORT)) {

            InputStream socketIn = socket.getInputStream();
            OutputStream socketOut = socket.getOutputStream();

            socketOut.write("Hello from the other side\n".getBytes());

            byte[] buffer = new byte[1024];
            int read;
            while ((read = socketIn.read(buffer)) != -1) {
                System.out.write(buffer, 0, read);
                System.out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

