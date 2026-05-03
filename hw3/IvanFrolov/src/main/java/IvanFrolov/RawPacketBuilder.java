package IvanFrolov;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Random;

public class RawPacketBuilder {
    private static Random random = new Random();

    public static byte[] buildEthernetIpv4Udp(
        byte[] dstMac,
        byte[] srcMac,
        Inet4Address srcIp,
        Inet4Address dstIp,
        int srcPort,
        int dstPort,
        byte[] udpPayload
    ) throws Exception {
        if (dstMac == null || dstMac.length != 6) throw new Exception("Bad dst MAC");
        if (srcMac == null || srcMac.length != 6) throw new Exception("Bad src MAC");
        if (udpPayload == null) udpPayload = new byte[0];

        int ipHeaderLen = 20;
        int udpHeaderLen = 8;
        int totalLen = ipHeaderLen + udpHeaderLen + udpPayload.length;

        ByteBuffer ip = ByteBuffer.allocate(ipHeaderLen);
        ip.put((byte) 0x45); // Version=4, IHL=5
        ip.put((byte) 0x00); // TOS
        ip.putShort((short) totalLen);
        ip.putShort((short) random.nextInt(0x10000)); // Identification
        ip.putShort((short) 0x0000); // Flags+Fragment
        ip.put((byte) 64); // TTL
        ip.put((byte) 17); // Protocol UDP
        ip.putShort((short) 0x0000); // Header checksum placeholder
        ip.put(srcIp.getAddress());
        ip.put(dstIp.getAddress());

        byte[] ipHeader = ip.array();
        short ipCsum = checksum16(ipHeader, 0, ipHeader.length);
        ipHeader[10] = (byte) ((ipCsum >> 8) & 0xFF);
        ipHeader[11] = (byte) (ipCsum & 0xFF);

        ByteBuffer udp = ByteBuffer.allocate(udpHeaderLen);
        udp.putShort((short) (srcPort & 0xFFFF));
        udp.putShort((short) (dstPort & 0xFFFF));
        udp.putShort((short) (udpHeaderLen + udpPayload.length));
        udp.putShort((short) 0x0000); // checksum placeholder
        byte[] udpHeader = udp.array();

        short udpCsum = udpChecksum(srcIp.getAddress(), dstIp.getAddress(), udpHeader, udpPayload);
        udpHeader[6] = (byte) ((udpCsum >> 8) & 0xFF);
        udpHeader[7] = (byte) (udpCsum & 0xFF);

        ByteBuffer eth = ByteBuffer.allocate(14 + totalLen);
        eth.put(dstMac);
        eth.put(srcMac);
        eth.putShort((short) 0x0800); // EtherType IPv4
        eth.put(ipHeader);
        eth.put(udpHeader);
        eth.put(udpPayload);

        return eth.array();
    }

    private static short udpChecksum(byte[] srcIp, byte[] dstIp, byte[] udpHeader, byte[] udpPayload) {
        int udpLen = udpHeader.length + udpPayload.length;

        ByteBuffer pseudo = ByteBuffer.allocate(12);
        pseudo.put(srcIp);
        pseudo.put(dstIp);
        pseudo.put((byte) 0);
        pseudo.put((byte) 17);
        pseudo.putShort((short) udpLen);

        int total = pseudo.capacity() + udpLen;
        byte[] buf = new byte[total + (total % 2)];
        System.arraycopy(pseudo.array(), 0, buf, 0, pseudo.capacity());
        System.arraycopy(udpHeader, 0, buf, pseudo.capacity(), udpHeader.length);
        System.arraycopy(udpPayload, 0, buf, pseudo.capacity() + udpHeader.length, udpPayload.length);

        short csum = checksum16(buf, 0, buf.length);
        if (csum == 0) return (short) 0xFFFF;
        return csum;
    }

    public static short checksum16(byte[] buf, int off, int len) {
        long sum = 0;
        int i = off;

        while (i + 1 < off + len) {
            int word = ((buf[i] & 0xFF) << 8) | (buf[i + 1] & 0xFF);
            sum += word;
            i += 2;
        }

        if (i < off + len) {
            int word = (buf[i] & 0xFF) << 8;
            sum += word;
        }

        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return (short) (~sum & 0xFFFF);
    }
}
