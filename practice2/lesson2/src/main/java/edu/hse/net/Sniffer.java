package edu.hse.net;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;

import java.util.List;

public class Sniffer {

    public static void main(String[] args) throws Exception {
        System.out.println("Pcap4j version: " + Pcaps.libVersion());

        List<PcapNetworkInterface> networkInterfaces = Pcaps.findAllDevs();
        System.out.println("Available network interfaces:");
        networkInterfaces.forEach(nif -> System.out.println("  Name: " + nif.getName()));

        PcapNetworkInterface loopbackInterface = networkInterfaces.stream()
                .filter(PcapNetworkInterface::isLoopBack)
                //.filter(nif -> "eth0".equals(nif.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No loopback interface found"));

        System.out.println("Selected interface: " + loopbackInterface.getName());
        System.out.println("Interface is up: " + loopbackInterface.isUp());

        PcapHandle handle = loopbackInterface.openLive(
                65536,                    // Buffer size
                PcapNetworkInterface.PromiscuousMode.NONPROMISCUOUS,
                1000                      // Timeout in milliseconds
        );

        handle.setFilter(
                "tcp port 10000",
                BpfProgram.BpfCompileMode.OPTIMIZE
        );

        handle.loop(-1, (PacketListener) packet -> {
            IpV4Packet ipv4 = packet.get(IpV4Packet.class);
            TcpPacket tcp = packet.get(TcpPacket.class);

            if (ipv4 == null || tcp == null) {
                return;
            }

            String srcAddr = ipv4.getHeader().getSrcAddr().getHostAddress();
            int srcPort = tcp.getHeader().getSrcPort().valueAsInt();
            String dstAddr = ipv4.getHeader().getDstAddr().getHostAddress();
            int dstPort = tcp.getHeader().getDstPort().valueAsInt();

            System.out.println("=== PACKET CAPTURED ===");
            System.out.println("SRC: " + srcAddr + ":" + srcPort);
            System.out.println("DST: " + dstAddr + ":" + dstPort);

            byte[] payload = tcp.getPayload().getRawData();
            if (payload != null && payload.length > 0) {
                String asciiPayload = new String(payload);
                System.out.println("PAYLOAD (ASCII): " + asciiPayload);
            } else {
                System.out.println("PAYLOAD: (empty)");
            }
            System.out.println("-".repeat(30));
        });

        handle.close();
    }
}

