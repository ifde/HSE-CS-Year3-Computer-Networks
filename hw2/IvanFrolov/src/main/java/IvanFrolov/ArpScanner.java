package IvanFrolov;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.util.MacAddress;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.pcap4j.packet.namednumber.ArpOperation;

public class ArpScanner {
    private String myIp = Config.getMyIp(); // свой IP
    private String routerIp = Config.getRouterIp(); // IP роутера
    
    // Поля статистики
    private int ethernetCount = 0;
    private int arpCount = 0;
    private int broadcastCount = 0;
    private int arpBroadcastCount = 0;
    private int gratuitousArpCount = 0;
    private int targetedPairsCount = 0;
    private long totalRouterBytes = 0;
    private Set<MacAddress> uniqueMacs = new HashSet<>();

    // Map для отслеживания ожидающих ответов
    private Map<String, Long> pendingRequests = new HashMap<>();

    private MacAddress myMacObj;
    private MacAddress routerMacObj;

    public void captureArp(int durationSeconds) throws Exception {
        startCapture(durationSeconds);
    }

    public void collectStats(int durationSeconds) throws Exception {
        startCapture(durationSeconds);
    }

    public void startCapture(int durationSeconds) throws Exception {
        myMacObj = MacAddress.getByName(Config.getMyMac());
        routerMacObj = MacAddress.getByName(Config.getRouterMac());

        // Сброс статистики перед началом
        ethernetCount = 0;
        arpCount = 0;
        broadcastCount = 0;
        arpBroadcastCount = 0;
        gratuitousArpCount = 0;
        targetedPairsCount = 0;
        totalRouterBytes = 0;
        uniqueMacs.clear();
        pendingRequests.clear();

        InetAddress addr = InetAddress.getByName(myIp);
        PcapNetworkInterface nif = Pcaps.getDevByAddress(addr);
        if (nif == null) {
            throw new Exception("Не удалось найти интерфейс для IP: " + myIp);
        }
        
        PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 100);

        System.out.println("Запуск захвата на " + durationSeconds + " сек...");

        // Используем слушатель пакетов вместо цикла getNextPacket для стабильности на Mac
        PacketListener listener = new PacketListener() {
            @Override
            public void gotPacket(Packet packet) {
                if (packet == null) return;

                ethernetCount++;
                EthernetPacket eth = packet.get(EthernetPacket.class);
                if (eth != null) {
                    uniqueMacs.add(eth.getHeader().getSrcAddr());
                    uniqueMacs.add(eth.getHeader().getDstAddr());

                    if (eth.getHeader().getDstAddr().equals(MacAddress.ETHER_BROADCAST_ADDRESS)) {
                        broadcastCount++;
                    }
                    checkRouterTraffic(eth, packet.length());
                }

                if (packet.contains(ArpPacket.class)) {
                    ArpPacket arp = packet.get(ArpPacket.class);
                    arpCount++;
                    System.out.println(arp);

                    if (eth != null && eth.getHeader().getDstAddr().equals(MacAddress.ETHER_BROADCAST_ADDRESS)) {
                        arpBroadcastCount++;
                    }
                    checkArpType(arp);
                }
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

        printStats();
        handle.close();
    }

    private void checkArpType(ArpPacket arp) {
        ArpPacket.ArpHeader header = arp.getHeader();
        String srcIp = header.getSrcProtocolAddr().getHostAddress();
        String dstIp = header.getDstProtocolAddr().getHostAddress();
        
        // Gratuitous ARP: Source IP == Target IP
        if (srcIp.equals(dstIp)) {
            gratuitousArpCount++;
        }
        
        if (header.getOperation().equals(ArpOperation.REQUEST)) {
            // Сохраняем запрос: кто спрашивает и кого
            String key = srcIp + "->" + dstIp;
            pendingRequests.put(key, System.currentTimeMillis());
        } 
        else if (header.getOperation().equals(ArpOperation.REPLY)) {
            // Для ответа отправитель и получатель меняются местами относительно запроса
            String lookFor = dstIp + "->" + srcIp;
            
            if (pendingRequests.containsKey(lookFor)) {
                targetedPairsCount++;
                pendingRequests.remove(lookFor);
            }
        }
    }

    private void checkRouterTraffic(EthernetPacket eth, int length) {
        MacAddress src = eth.getHeader().getSrcAddr();
        MacAddress dst = eth.getHeader().getDstAddr();

        if (myMacObj != null && routerMacObj != null) {
            if ((src.equals(myMacObj) && dst.equals(routerMacObj)) || 
                (src.equals(routerMacObj) && dst.equals(myMacObj))) {
                totalRouterBytes += length;
            }
        }
    }

    private void printStats() {
        System.out.println("\n--- Статистика за период ---");
        System.out.println("1. Ethernet фреймов: " + ethernetCount);
        System.out.println("2. ARP пакетов: " + arpCount);
        System.out.println("3. Уникальных MAC адресов: " + uniqueMacs.size());
        System.out.println("4. Широковещательных Ethernet: " + broadcastCount);
        System.out.println("5. Широковещательных ARP: " + arpBroadcastCount);
        System.out.println("6. Gratuitous ARP: " + gratuitousArpCount);
        System.out.println("7. ARP targeted pairs (replies): " + targetedPairsCount);
        System.out.println("8. Объем данных устройство-роутер (bytes): " + totalRouterBytes);
    }
}
