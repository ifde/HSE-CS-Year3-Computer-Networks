package IvanFrolov;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class DnsMessageBuilder {
    private static Random random = new Random();

    public static class DnsQuery {
        public int txId;
        public int srcPort;
        public byte[] payload;
    }

    public static DnsQuery buildQuery(String qname, int qtype, boolean recursionDesired) throws Exception {
        if (qname == null) throw new Exception("Domain is null");
        qname = normalizeName(qname);

        int txId = random.nextInt(0x10000);
        int flags = 0;
        flags |= (0 << 15); // QR = 0 (query)
        flags |= (0 << 11); // OPCODE = 0
        flags |= (0 << 10); // AA
        flags |= (0 << 9);  // TC
        flags |= ((recursionDesired ? 1 : 0) << 8); // RD
        flags |= (0 << 7);  // RA
        flags |= (0 << 4);  // Z
        flags |= 0;         // RCODE

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(shortToBytes(txId));
        out.write(shortToBytes(flags));
        out.write(shortToBytes(1)); // QDCOUNT
        out.write(shortToBytes(0)); // ANCOUNT
        out.write(shortToBytes(0)); // NSCOUNT
        out.write(shortToBytes(0)); // ARCOUNT

        out.write(encodeName(qname));
        out.write(shortToBytes(qtype));
        out.write(shortToBytes(1)); // QCLASS = IN

        DnsQuery query = new DnsQuery();
        query.txId = txId;
        query.payload = out.toByteArray();
        query.srcPort = 40000 + random.nextInt(20000);
        return query;
    }

    private static String normalizeName(String name) {
        name = name.trim();
        if (name.endsWith(".")) name = name.substring(0, name.length() - 1);
        return name;
    }

    private static byte[] shortToBytes(int value) {
        return ByteBuffer.allocate(2).putShort((short) value).array();
    }

    private static byte[] encodeName(String name) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (name.isEmpty()) {
            out.write(0);
            return out.toByteArray();
        }

        String[] labels = name.split("\\.");
        for (String label : labels) {
            byte[] b = label.getBytes(StandardCharsets.US_ASCII);
            if (b.length == 0 || b.length > 63) {
                throw new Exception("Bad label in domain: " + label);
            }
            out.write((byte) b.length);
            out.write(b);
        }
        out.write(0);
        return out.toByteArray();
    }
}
