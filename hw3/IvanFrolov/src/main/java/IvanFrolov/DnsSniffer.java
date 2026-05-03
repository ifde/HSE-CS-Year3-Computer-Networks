package IvanFrolov;

import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.packet.Packet;

public class DnsSniffer {
    public void captureDns(int durationSeconds) throws Exception {
        PcapNetworkInterface nif = PcapUtils.getNifByMyIp();
        System.out.println("Используется интерфейс: " + nif.getName() + " (" + nif.getDescription() + ")");

        PcapHandle handle = PcapUtils.openLive(nif, 100);
        handle.setFilter("udp port 53 or tcp port 53", org.pcap4j.core.BpfProgram.BpfCompileMode.OPTIMIZE);

        System.out.println("Запуск захвата DNS на " + durationSeconds + " сек...");

        PacketListener listener = new PacketListener() {
            @Override
            public void gotPacket(Packet packet) {
                if (packet == null) return;
                if (packet.get(org.pcap4j.packet.UdpPacket.class) == null && packet.get(org.pcap4j.packet.TcpPacket.class) == null) {
                    return;
                }

                System.out.println("\n------------------------------");
                DnsPrinter.printDnsPacket(packet);
            }
        };

        try {
            Thread captureThread = new Thread(() -> {
                try {
                    handle.loop(-1, listener);
                } catch (Exception e) {
                }
            });
            captureThread.start();

            Thread.sleep(durationSeconds * 1000L);
            handle.breakLoop();
            captureThread.join(2000);
        } catch (InterruptedException e) {
            handle.breakLoop();
        }

        handle.close();
    }
}
