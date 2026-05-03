package IvanFrolov;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.namednumber.DataLinkType;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Random;

public class DnsClientSession implements AutoCloseable {
    private static Random random = new Random();

    private final PcapNetworkInterface nif;
    private final PcapHandle sendHandle;
    private final PcapHandle recvHandle;
    private final DataLinkType dlt;

    private final String myIp;
    private final MacAddress myMac;
    private final MacAddress routerMac; // only for EN10MB

    public DnsClientSession() throws Exception {
        this.nif = PcapUtils.getNifByMyIp();
        System.out.println("Используется интерфейс: " + nif.getName() + " (" + nif.getDescription() + ")");

        // Two handles: one for sending, one for receiving.
        // This avoids some macOS libpcap crashes when mixing injection + frequent filter changes.
        this.sendHandle = PcapUtils.openLive(nif, 10);
        this.recvHandle = PcapUtils.openLive(nif, 50);

        this.dlt = sendHandle.getDlt();
        System.out.println("DLT: " + dlt);

        // Capture only DNS responses to our host; validate expected server IP in code.
        String filter = "udp and src port 53 and dst host " + Config.getMyIp();
        recvHandle.setFilter(filter, org.pcap4j.core.BpfProgram.BpfCompileMode.OPTIMIZE);

        this.myIp = Config.getMyIp();
        this.myMac = MacAddress.getByName(Config.getMyMac());

        if (dlt.equals(DataLinkType.EN10MB)) {
            String routerMacStr = Config.getRouterMac();
            if (routerMacStr == null || routerMacStr.isEmpty()) {
                throw new Exception("config.properties: router.mac is empty (needed for Ethernet injection)");
            }
            this.routerMac = MacAddress.getByName(routerMacStr);
        } else {
            this.routerMac = null;
        }
    }

    public DnsParser.DnsMessage query(String dnsServerIp, String domain, int qtype, boolean recursionDesired) throws Exception {
        if (dnsServerIp == null || dnsServerIp.isEmpty()) {
            throw new Exception("DNS server IP is empty");
        }

        DnsMessageBuilder.DnsQuery q = DnsMessageBuilder.buildQuery(domain, qtype, recursionDesired);

        Inet4Address srcIp = (Inet4Address) InetAddress.getByName(myIp);
        Inet4Address dstIp = (Inet4Address) InetAddress.getByName(dnsServerIp);

        System.out.println(
            "DNS query TXID=" + q.txId +
            " sport=" + q.srcPort +
            " domain=" + domain +
            " type=" + qtype +
            " -> " + dnsServerIp +
            " (RD=" + (recursionDesired ? 1 : 0) + ")"
        );

        // Build raw bytes for sending (Ethernet + IPv4 + UDP + DNS)
        byte[] frame = null;
        if (dlt.equals(DataLinkType.EN10MB)) {
            frame = RawPacketBuilder.buildEthernetIpv4Udp(
                routerMac.getAddress(),
                myMac.getAddress(),
                srcIp,
                dstIp,
                q.srcPort,
                53,
                q.payload
            );
        }
        else {
            throw new Exception("Unsupported DLT for sending: " + dlt);
        }

        // Try twice (helps with transient drops)
        for (int attempt = 1; attempt <= 2; attempt++) {
            sendHandle.sendPacket(frame);

            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 7000) {
                byte[] raw = recvHandle.getNextRawPacket();
                if (raw == null) continue;

                byte[] dnsPayload = extractDnsPayload(raw);
                if (dnsPayload == null) continue;

                DnsParser.DnsMessage msg;
                try {
                    msg = DnsParser.parse(dnsPayload);
                } catch (Exception e) {
                    continue;
                }

                if (msg.txId == q.txId && msg.isResponse()) {
                    return msg;
                }
            }
        }

        return null;
    }

    // Extract UDP payload (DNS) from a raw Ethernet frame.
    // Returns null if not IPv4+UDP or not port 53 response destined to our host.
    private byte[] extractDnsPayload(byte[] raw) {
        try {
            int ipOff = getIpOffset(raw);
            if (ipOff < 0) return null;

            if (ipOff + 20 > raw.length) return null;
            int verIhl = raw[ipOff] & 0xFF;
            int ver = (verIhl >> 4) & 0x0F;
            int ihl = verIhl & 0x0F;
            if (ver != 4) return null;
            int ipHdrLen = ihl * 4;
            if (ipHdrLen < 20) return null;
            if (ipOff + ipHdrLen > raw.length) return null;

            int proto = raw[ipOff + 9] & 0xFF;
            if (proto != 17) return null; // UDP

            String src = (raw[ipOff + 12] & 0xFF) + "." + (raw[ipOff + 13] & 0xFF) + "." + (raw[ipOff + 14] & 0xFF) + "." + (raw[ipOff + 15] & 0xFF);
            String dst = (raw[ipOff + 16] & 0xFF) + "." + (raw[ipOff + 17] & 0xFF) + "." + (raw[ipOff + 18] & 0xFF) + "." + (raw[ipOff + 19] & 0xFF);

            if (!dst.equals(myIp)) return null;
            if (src == null) return null;

            int udpOff = ipOff + ipHdrLen;
            if (udpOff + 8 > raw.length) return null;

            int srcPort = ((raw[udpOff] & 0xFF) << 8) | (raw[udpOff + 1] & 0xFF);
            int dstPort = ((raw[udpOff + 2] & 0xFF) << 8) | (raw[udpOff + 3] & 0xFF);
            if (srcPort != 53) return null;

            int udpLen = ((raw[udpOff + 4] & 0xFF) << 8) | (raw[udpOff + 5] & 0xFF);
            if (udpLen < 8) return null;
            int payloadLen = udpLen - 8;

            int payOff = udpOff + 8;
            if (payOff + payloadLen > raw.length) return null;

            byte[] payload = new byte[payloadLen];
            System.arraycopy(raw, payOff, payload, 0, payloadLen);
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private int getIpOffset(byte[] raw) {
        if (raw == null) return -1;
        if (dlt.equals(DataLinkType.EN10MB)) {
            if (raw.length < 14) return -1;
            int etherType = ((raw[12] & 0xFF) << 8) | (raw[13] & 0xFF);
            if (etherType != 0x0800) return -1;
            return 14;
        }
        return -1;
    }

    @Override
    public void close() {
        try {
            recvHandle.close();
        } catch (Exception e) {
        }
        try {
            sendHandle.close();
        } catch (Exception e) {
        }
    }
}
