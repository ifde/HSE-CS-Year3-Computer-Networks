package IvanFrolov;

import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

public class DnsPrinter {
    public static void printDnsPacket(Packet packet) {
        EthernetPacket eth = packet.get(EthernetPacket.class);
        if (eth != null) {
            System.out.println("Ethernet: " + eth.getHeader().getSrcAddr() + " -> " + eth.getHeader().getDstAddr());
        }

        IpV4Packet ip = packet.get(IpV4Packet.class);
        if (ip != null) {
            System.out.println("IPv4: " + ip.getHeader().getSrcAddr() + " -> " + ip.getHeader().getDstAddr());
        }

        UdpPacket udp = packet.get(UdpPacket.class);
        if (udp != null) {
            System.out.println("UDP: " + udp.getHeader().getSrcPort() + " -> " + udp.getHeader().getDstPort());
            if (udp.getPayload() != null) {
                try {
                    DnsParser.DnsMessage msg = DnsParser.parse(udp.getPayload().getRawData());
                    printDnsMessage(msg);
                } catch (Exception e) {
                    System.out.println("DNS: parse error");
                }
            }
            return;
        }

        TcpPacket tcp = packet.get(TcpPacket.class);
        if (tcp != null) {
            System.out.println("TCP: " + tcp.getHeader().getSrcPort() + " -> " + tcp.getHeader().getDstPort());
            if (tcp.getPayload() != null) {
                byte[] raw = tcp.getPayload().getRawData();
                if (raw != null && raw.length >= 2 + 12) {
                    int dnsLen = ((raw[0] & 0xFF) << 8) | (raw[1] & 0xFF);
                    if (dnsLen > 0 && raw.length >= 2 + dnsLen) {
                        byte[] dns = new byte[dnsLen];
                        System.arraycopy(raw, 2, dns, 0, dnsLen);
                        try {
                            DnsParser.DnsMessage msg = DnsParser.parse(dns);
                            printDnsMessage(msg);
                        } catch (Exception e) {
                            System.out.println("DNS(TCP): parse error");
                        }
                    }
                }
            }
        }
    }

    public static void printDnsMessage(DnsParser.DnsMessage msg) {
        System.out.println(
            "DNS: txid=" + msg.txId +
            " qr=" + (msg.isResponse() ? 1 : 0) +
            " rcode=" + msg.rcode() +
            " qd=" + msg.qdCount +
            " an=" + msg.anCount +
            " ns=" + msg.nsCount +
            " ar=" + msg.arCount
        );

        for (DnsParser.DnsQuestion q : msg.questions) {
            System.out.println(" Q: " + q.name + " type=" + q.type + " class=" + q.qclass);
        }

        for (DnsParser.DnsRecord r : msg.answers) {
            System.out.println(" AN: " + r.name + " type=" + r.type + " ttl=" + r.ttl + " data=" + r.data);
        }

        for (DnsParser.DnsRecord r : msg.authorities) {
            System.out.println(" NS: " + r.name + " type=" + r.type + " ttl=" + r.ttl + " data=" + r.data);
        }

        for (DnsParser.DnsRecord r : msg.additionals) {
            System.out.println(" AR: " + r.name + " type=" + r.type + " ttl=" + r.ttl + " data=" + r.data);
        }
    }
}
