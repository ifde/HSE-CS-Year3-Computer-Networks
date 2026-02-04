package task1;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.nio.ByteBuffer;

public class Client {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Client <IP> <port> <N> <M> <Q>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);
        int m = Integer.parseInt(args[3]);
        int q = Integer.parseInt(args[4]);

        Random random = new Random();
        int[] sizes = new int[m];
        double[] avgTimes = new double[m];

        try (Socket socket = new Socket(ip, port)) {
            socket.setTcpNoDelay(true);

            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            for (int k = 0; k < m; k++) {
                int dataLength = n * k + 8;
                sizes[k] = dataLength;
                long totalIterationTime = 0;

                for (int j = 0; j < q; j++) {
                    byte[] dataToSend = new byte[dataLength];
                    random.nextBytes(dataToSend);

                    ByteBuffer bb = ByteBuffer.allocate(4);
                    bb.putInt(dataLength);
                    out.write(bb.array());
                    out.write(dataToSend);
                    out.flush();

                    long startTime = System.currentTimeMillis();

                    String serverAnswer = in.readLine();
                    if (serverAnswer == null) break;

                    long endTime = System.currentTimeMillis();
                    totalIterationTime += (endTime - startTime);
                }

                avgTimes[k] = totalIterationTime / (double) q;
            }

            System.out.println("\nOutput");
            System.out.println("Bytes\tAverageTime");
            for (int i = 0; i < m; i++) {
                System.out.printf("%d \t %.4f %n", sizes[i], avgTimes[i]);
            }

            // Saving to results.csv
            try (PrintWriter writer = new PrintWriter(new FileWriter("results.csv"))) {
                writer.println("Bytes,AverageTime");
                for (int i = 0; i < m; i++) {
                    writer.printf("%d,%.4f%n", sizes[i], avgTimes[i]);
                }
                System.out.println("Saved to results.csv");
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }
}