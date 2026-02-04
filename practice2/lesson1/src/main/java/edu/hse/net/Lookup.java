package edu.hse.net;

import java.net.InetAddress;
import java.net.Inet6Address;

public class Lookup {
    public static void main(String[] args) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName("ya.ru");
            for (InetAddress addr : addrs) {
                System.out.printf("%s -> ", addr.getHostName());
                if (addr instanceof Inet6Address) {
                    System.out.print("[IPv6] ");
                }
                System.out.println(addr.getHostAddress());
            }
            System.out.println("===");
            InetAddress addr = InetAddress.getByName("213.180.193.56");
            System.out.printf("%s -> %s\n", addr.getHostAddress(), addr.getHostName());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
