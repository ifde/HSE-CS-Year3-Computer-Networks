package IvanFrolov;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Rfc791Tos;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.packet.namednumber.UdpPort;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

public class DnsClient {
    private static Random random = new Random();

    public DnsParser.DnsMessage query(String dnsServerIp, String domain, int qtype, boolean recursionDesired) throws Exception {
        if (dnsServerIp == null || dnsServerIp.isEmpty()) {
            throw new Exception("DNS server IP is empty");
        }

        PcapNetworkInterface nif = PcapUtils.getNifByMyIp();
        System.out.println("Используется интерфейс: " + nif.getName() + " (" + nif.getDescription() + ")");

        DnsMessageBuilder.DnsQuery q = DnsMessageBuilder.buildQuery(domain, qtype, recursionDesired);

        String myIp = Config.getMyIp();
        MacAddress myMac = MacAddress.getByName(Config.getMyMac());
        Inet4Address srcIp = (Inet4Address) InetAddress.getByName(myIp);
        Inet4Address dstIp = (Inet4Address) InetAddress.getByName(dnsServerIp);

        UnknownPacket.Builder dnsPayload = new UnknownPacket.Builder().rawData(q.payload);

        UdpPacket.Builder udpBuilder = new UdpPacket.Builder();
        udpBuilder
            .srcPort(UdpPort.getInstance((short) q.srcPort))
            .dstPort(UdpPort.DOMAIN)
            .payloadBuilder(dnsPayload)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);

        IpV4Packet.Builder ipBuilder = new IpV4Packet.Builder();
        ipBuilder
            .version(IpVersion.IPV4)
            .tos(IpV4Rfc791Tos.newInstance((byte)0))
            .identification((short) random.nextInt(0x10000))
            .ttl((byte)64)
            .protocol(IpNumber.UDP)
            .srcAddr(srcIp)
            .dstAddr(dstIp)
            .payloadBuilder(udpBuilder)
            .correctChecksumAtBuild(true)
            .correctLengthAtBuild(true);

        String filter = "udp and src host " + dnsServerIp + " and src port 53 and dst host " + myIp + " and dst port " + q.srcPort;

        PcapHandle handle = PcapUtils.openLive(nif, 50);
        try {
            DataLinkType dlt = handle.getDlt();
            System.out.println("DLT: " + dlt);

            handle.setFilter(filter, org.pcap4j.core.BpfProgram.BpfCompileMode.OPTIMIZE);

            System.out.println(
                "DNS query TXID=" + q.txId +
                " sport=" + q.srcPort +
                " domain=" + domain +
                " type=" + qtype +
                " -> " + dnsServerIp +
                " (RD=" + (recursionDesired ? 1 : 0) + ")"
            );

            // Some interfaces (especially Wi-Fi on macOS) don't support injecting full Ethernet frames.
            if (dlt.equals(DataLinkType.EN10MB)) {
                String routerMacStr = Config.getRouterMac();
                if (routerMacStr == null || routerMacStr.isEmpty()) {
                    throw new Exception("config.properties: router.mac is empty (needed for Ethernet injection)");
                }
                MacAddress routerMac = MacAddress.getByName(routerMacStr);

                EthernetPacket.Builder ethBuilder = new EthernetPacket.Builder();
                ethBuilder
                    .dstAddr(routerMac)
                    .srcAddr(myMac)
                    .type(EtherType.IPV4)
                    .payloadBuilder(ipBuilder)
                    .paddingAtBuild(true);

                handle.sendPacket(ethBuilder.build());
            }
            else {
                // Fallback: inject at L3 (OS will wrap into link-layer frames).
                handle.sendPacket(ipBuilder.build());
            }

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 7000) {
                Packet p = handle.getNextPacket();
                if (p == null) continue;

                UdpPacket udp = p.get(UdpPacket.class);
                if (udp == null || udp.getPayload() == null) continue;

                byte[] payload = udp.getPayload().getRawData();
                if (payload == null || payload.length < 12) continue;

                DnsParser.DnsMessage msg;
                try {
                    msg = DnsParser.parse(payload);
                } catch (Exception e) {
                    continue;
                }

                if (msg.txId == q.txId && msg.isResponse()) {
                    return msg;
                }
            }

            return null;
        } finally {
            handle.close();
        }
    }
}
