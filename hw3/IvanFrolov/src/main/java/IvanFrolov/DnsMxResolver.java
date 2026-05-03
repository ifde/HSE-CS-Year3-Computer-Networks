package IvanFrolov;

import java.util.ArrayList;
import java.util.List;

public class DnsMxResolver {
    public void resolveMxIp(String domain) throws Exception {
        String providerDns = Config.getProviderDnsIp();
        if (providerDns == null || providerDns.isEmpty()) {
            System.out.println("Error: provider.dns.ip is empty in config.properties");
            return;
        }

        try (DnsClientSession session = new DnsClientSession()) {
            // Step 1: MX lookup
            DnsParser.DnsMessage mxResp = session.query(providerDns, domain, 15, true);
        if (mxResp == null) {
            System.out.println("No DNS response");
            return;
        }

        List<String> mxHosts = new ArrayList<>();
        for (DnsParser.DnsRecord r : mxResp.answers) {
            if (r.type == 15) {
                // "pref exchange"
                String[] parts = r.data.split(" ", 2);
                if (parts.length == 2) {
                    mxHosts.add(parts[1]);
                }
            }
        }

        if (mxHosts.isEmpty()) {
            System.out.println("MX not found for " + domain);
            return;
        }

        // Step 2: Resolve A/AAAA for each MX host
        for (String mx : mxHosts) {
            boolean printed = false;

            DnsParser.DnsMessage aResp = session.query(providerDns, mx, 1, true);
            if (aResp != null) {
                for (DnsParser.DnsRecord r : aResp.answers) {
                    if (r.type == 1) {
                        System.out.println(mx + " : " + domain + " -> " + r.data);
                        printed = true;
                    }
                }
            }

            if (!printed) {
                DnsParser.DnsMessage aaaaResp = session.query(providerDns, mx, 28, true);
                if (aaaaResp != null) {
                    for (DnsParser.DnsRecord r : aaaaResp.answers) {
                        if (r.type == 28) {
                            System.out.println(mx + " : " + domain + " -> " + r.data);
                            printed = true;
                        }
                    }
                }
            }

            if (!printed) {
                System.out.println(mx + " : " + domain + " -> (IP not found)");
            }
        }
        }
    }
}
