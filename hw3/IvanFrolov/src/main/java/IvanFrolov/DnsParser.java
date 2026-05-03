package IvanFrolov;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DnsParser {
    public static class DnsMessage {
        public int txId;
        public int flags;
        public int qdCount;
        public int anCount;
        public int nsCount;
        public int arCount;

        public List<DnsQuestion> questions = new ArrayList<>();
        public List<DnsRecord> answers = new ArrayList<>();
        public List<DnsRecord> authorities = new ArrayList<>();
        public List<DnsRecord> additionals = new ArrayList<>();

        public boolean isResponse() { return ((flags >> 15) & 1) == 1; }
        public int rcode() { return flags & 0x0F; }
    }

    public static class DnsQuestion {
        public String name;
        public int type;
        public int qclass;
    }

    public static class DnsRecord {
        public String name;
        public int type;
        public int rclass;
        public long ttl;
        public String data;
    }

    public static DnsMessage parse(byte[] msg) throws Exception {
        if (msg == null || msg.length < 12) {
            throw new Exception("DNS message too short");
        }

        DnsMessage m = new DnsMessage();
        int off = 0;
        m.txId = readU16(msg, off); off += 2;
        m.flags = readU16(msg, off); off += 2;
        m.qdCount = readU16(msg, off); off += 2;
        m.anCount = readU16(msg, off); off += 2;
        m.nsCount = readU16(msg, off); off += 2;
        m.arCount = readU16(msg, off); off += 2;

        for (int i = 0; i < m.qdCount; i++) {
            NameResult nr = readName(msg, off, new HashSet<>());
            off = nr.nextOffset;
            DnsQuestion q = new DnsQuestion();
            q.name = nr.name;
            q.type = readU16(msg, off); off += 2;
            q.qclass = readU16(msg, off); off += 2;
            m.questions.add(q);
        }

        for (int i = 0; i < m.anCount; i++) {
            RecordResult rr = readRecord(msg, off);
            off = rr.nextOffset;
            m.answers.add(rr.record);
        }

        for (int i = 0; i < m.nsCount; i++) {
            RecordResult rr = readRecord(msg, off);
            off = rr.nextOffset;
            m.authorities.add(rr.record);
        }

        for (int i = 0; i < m.arCount; i++) {
            RecordResult rr = readRecord(msg, off);
            off = rr.nextOffset;
            m.additionals.add(rr.record);
        }

        return m;
    }

    private static class NameResult {
        public String name;
        public int nextOffset;
        public NameResult(String name, int nextOffset) {
            this.name = name;
            this.nextOffset = nextOffset;
        }
    }

    private static class RecordResult {
        public DnsRecord record;
        public int nextOffset;
        public RecordResult(DnsRecord record, int nextOffset) {
            this.record = record;
            this.nextOffset = nextOffset;
        }
    }

    private static RecordResult readRecord(byte[] msg, int off) throws Exception {
        NameResult nr = readName(msg, off, new HashSet<>());
        off = nr.nextOffset;

        DnsRecord r = new DnsRecord();
        r.name = nr.name;
        r.type = readU16(msg, off); off += 2;
        r.rclass = readU16(msg, off); off += 2;
        r.ttl = readU32(msg, off); off += 4;
        int rdlen = readU16(msg, off); off += 2;
        int rdataOff = off;
        off += rdlen;

        r.data = parseRdata(msg, rdataOff, rdlen, r.type);
        return new RecordResult(r, off);
    }

    private static String parseRdata(byte[] msg, int off, int rdlen, int type) throws Exception {
        if (off + rdlen > msg.length) {
            throw new Exception("Bad RDLENGTH");
        }

        if (type == 1 && rdlen == 4) {
            return InetAddress.getByAddress(new byte[] {msg[off], msg[off+1], msg[off+2], msg[off+3]}).getHostAddress();
        }
        if (type == 28 && rdlen == 16) {
            byte[] a = new byte[16];
            System.arraycopy(msg, off, a, 0, 16);
            return InetAddress.getByAddress(a).getHostAddress();
        }
        if (type == 2 || type == 5) {
            // NS or CNAME
            return readName(msg, off, new HashSet<>()).name;
        }
        if (type == 15) {
            // MX: preference + exchange
            if (rdlen < 3) return "";
            int pref = readU16(msg, off);
            String exch = readName(msg, off + 2, new HashSet<>()).name;
            return pref + " " + exch;
        }

        return bytesToHex(msg, off, rdlen);
    }

    private static NameResult readName(byte[] msg, int off, Set<Integer> visited) throws Exception {
        StringBuilder sb = new StringBuilder();
        int nextOffset = -1;

        while (true) {
            if (off >= msg.length) {
                throw new Exception("Name out of bounds");
            }

            int len = msg[off] & 0xFF;

            if (len == 0) {
                off += 1;
                break;
            }

            int labelType = (len & 0xC0);
            if (labelType == 0xC0) {
                if (off + 1 >= msg.length) {
                    throw new Exception("Bad compression pointer");
                }
                int pointer = ((len & 0x3F) << 8) | (msg[off + 1] & 0xFF);
                if (visited.contains(pointer)) {
                    throw new Exception("Compression loop");
                }
                visited.add(pointer);

                NameResult nr = readName(msg, pointer, visited);
                if (sb.length() > 0 && nr.name.length() > 0) sb.append(".");
                sb.append(nr.name);

                nextOffset = off + 2;
                break;
            }

            if (labelType != 0) {
                throw new Exception("Unsupported label type");
            }

            off += 1;
            if (off + len > msg.length) {
                throw new Exception("Label out of bounds");
            }
            if (sb.length() > 0) sb.append(".");
            for (int i = 0; i < len; i++) {
                sb.append((char) (msg[off + i] & 0xFF));
            }
            off += len;
        }

        if (nextOffset == -1) nextOffset = off;
        return new NameResult(sb.toString(), nextOffset);
    }

    private static int readU16(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static long readU32(byte[] b, int off) {
        return ((long)(b[off] & 0xFF) << 24) | ((long)(b[off + 1] & 0xFF) << 16) | ((long)(b[off + 2] & 0xFF) << 8) | (long)(b[off + 3] & 0xFF);
    }

    private static String bytesToHex(byte[] b, int off, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int v = b[off + i] & 0xFF;
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", v));
        }
        return sb.toString();
    }
}
