package edu.hse.net;

import java.net.InetAddress;

public class Ping {
    public static void main(String[] args) {
        try {
            InetAddress addr = InetAddress.getByName("77.88.8.8");
            while (true) {
                long start = System.currentTimeMillis();
                Boolean ok = addr.isReachable(5000);
                long duration = System.currentTimeMillis() - start;

                System.out.printf("%s reachable: %b %dms\n", addr.getHostAddress(), ok, duration);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
