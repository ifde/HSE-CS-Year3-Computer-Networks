package IvanFrolov;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;
import java.net.InetAddress;

public class ArpSender {
    public void findRouterMac() throws Exception {
        String myIp = Config.getMyIp();
        String routerIp = Config.getRouterIp();
        MacAddress myMac = MacAddress.getByName(Config.getMyMac());

        PcapNetworkInterface nif = Pcaps.getDevByAddress(InetAddress.getByName(myIp));
        if (nif == null) {
            System.out.println("Ошибка: интерфейс с IP " + myIp + " не найден!");
            return;
        }
        System.out.println("Используется интерфейс: " + nif.getName() + " (" + nif.getDescription() + ")");

        PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

        ArpPacket.Builder arpBuilder = new ArpPacket.Builder()
            .hardwareType(ArpHardwareType.ETHERNET)
            .protocolType(EtherType.IPV4)
            .hardwareAddrLength((byte) MacAddress.SIZE_IN_BYTES)
            .protocolAddrLength((byte) 4)
            .operation(ArpOperation.REQUEST)
            .srcHardwareAddr(myMac)
            .srcProtocolAddr(InetAddress.getByName(myIp))
            .dstHardwareAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
            .dstProtocolAddr(InetAddress.getByName(routerIp));

        EthernetPacket.Builder ethBuilder = new EthernetPacket.Builder()
            .dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
            .srcAddr(myMac)
            .type(EtherType.ARP)
            .payloadBuilder(arpBuilder)
            .paddingAtBuild(true);

        handle.sendPacket(ethBuilder.build());
        System.out.println("ARP Request sent to " + routerIp);
        handle.close();
    }
}
