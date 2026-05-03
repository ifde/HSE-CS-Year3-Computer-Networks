package IvanFrolov;

import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import java.net.InetAddress;

public class PcapUtils {
    public static PcapNetworkInterface getNifByMyIp() throws Exception {
        String myIp = Config.getMyIp();
        if (myIp == null || myIp.isEmpty()) {
            throw new Exception("config.properties: my.ip is empty");
        }

        PcapNetworkInterface nif = Pcaps.getDevByAddress(InetAddress.getByName(myIp));
        if (nif == null) {
            throw new Exception("Не удалось найти интерфейс для IP: " + myIp);
        }
        return nif;
    }

    public static PcapHandle openLive(PcapNetworkInterface nif, int timeoutMs) throws Exception {
        return nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, timeoutMs);
    }
}
