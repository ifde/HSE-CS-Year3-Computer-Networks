package IvanFrolov;

public class DnsRootChecker {
    public void check(String rootDnsIp, String providerDnsIp) throws Exception {
        if (rootDnsIp == null || rootDnsIp.isEmpty()) {
            System.out.println("Error: root DNS IP is empty");
            return;
        }
        if (providerDnsIp == null || providerDnsIp.isEmpty()) {
            System.out.println("Error: provider DNS IP is empty");
            return;
        }

        String[] domains = new String[] {"github.com", "hse.ru", "draw.io"};

        try (DnsClientSession session = new DnsClientSession()) {
            System.out.println("\n=== A) Root DNS server: " + rootDnsIp + " ===");
            for (String d : domains) {
                System.out.println("\n--- " + d + " ---");
                DnsParser.DnsMessage resp = session.query(rootDnsIp, d, 1, false);
                if (resp == null) {
                    System.out.println("No DNS response");
                    continue;
                }
                DnsPrinter.printDnsMessage(resp);
            }

            System.out.println("\n=== B) Provider DNS server: " + providerDnsIp + " ===");
            for (String d : domains) {
                System.out.println("\n--- " + d + " ---");
                DnsParser.DnsMessage resp = session.query(providerDnsIp, d, 1, true);
                if (resp == null) {
                    System.out.println("No DNS response");
                    continue;
                }
                DnsPrinter.printDnsMessage(resp);
            }
        }
    }
}
