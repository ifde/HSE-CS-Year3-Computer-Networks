package IvanFrolov;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static Properties prop = new Properties();

    static {
        try (FileInputStream child = new FileInputStream("config.properties")) {
            prop.load(child);
        } catch (IOException e) {
            System.err.println("Error: Could not find config.properties");
        }
    }

    public static String getMyIp() { return prop.getProperty("my.ip"); }
    public static String getMyMac() { return prop.getProperty("my.mac"); }
    public static String getRouterMac() { return prop.getProperty("router.mac"); }

    public static String getProviderDnsIp() { return prop.getProperty("provider.dns.ip"); }
    public static String getRootDnsIp() { return prop.getProperty("root.dns.ip"); }

    public static String getSendMode() {
        String v = prop.getProperty("send.mode");
        if (v == null || v.isEmpty()) return "l3";
        return v.trim().toLowerCase();
    }
}
