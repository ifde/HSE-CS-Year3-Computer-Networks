package IvanFrolov;

import java.util.Scanner;

/**
 * Основной класс приложения
 */
public class App {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        DnsSniffer snifferService = new DnsSniffer();
        DnsMxResolver mxService = new DnsMxResolver();
        DnsRootChecker rootService = new DnsRootChecker();

        System.out.println("--- Приложение для управления DNS пакетами ---");
        System.out.println(
            "Commands: 1 (Захват все DNS пакетов),\n" +
            " 2 (Найти IP адрес почтового сервиса MX),\n" +
            " 3 (DNS: корневой сервер vs провайдер),\n" +
            " exit"
        );

        while (true) {
            System.out.print("\nEnter command: ");
            String cmd = scanner.nextLine();

            if (cmd.equals("1")) {
                System.out.print("Enter duration (sec): ");
                int sec = Integer.parseInt(scanner.nextLine());
                snifferService.captureDns(sec);
            }
            else if (cmd.equals("2")) {
                System.out.print("Enter domain D: ");
                String domain = scanner.nextLine();
                mxService.resolveMxIp(domain);
            }
            else if (cmd.equals("3")) {
                System.out.print("Enter root DNS IP (empty = from config): ");
                String rootIp = scanner.nextLine();
                if (rootIp == null || rootIp.trim().isEmpty()) {
                    rootIp = Config.getRootDnsIp();
                }

                System.out.print("Enter provider DNS IP (empty = from config): ");
                String providerIp = scanner.nextLine();
                if (providerIp == null || providerIp.trim().isEmpty()) {
                    providerIp = Config.getProviderDnsIp();
                }

                rootService.check(rootIp, providerIp);
            }
            else if (cmd.equals("exit")) {
                break;
            }
        }
    }
}
