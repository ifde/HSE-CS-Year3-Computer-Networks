package IvanFrolov;

import java.util.Scanner;

/**
 * Основной класс приложения
 */
public class App {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        ArpScanner scannerService = new ArpScanner();
        ArpSender senderService = new ArpSender();

        System.out.println("--- Приложение для управления ARP пакетами ---");
        System.out.println("Commands: 1 (Захват все ARP пакетов),\n 2 (Найти MAC адрес роутера),\n 3 (Статисика),\n exit");

        while (true) {
            System.out.print("\nEnter command: ");
            String cmd = scanner.nextLine();

            if (cmd.equals("1")) {
                System.out.print("Enter duration (sec): ");
                int sec = Integer.parseInt(scanner.nextLine());
                scannerService.captureArp(sec); 
            } else if (cmd.equals("2")) {
                senderService.findRouterMac();
            } else if (cmd.equals("3")) {
                System.out.print("Enter duration (sec): ");
                int sec = Integer.parseInt(scanner.nextLine());
                scannerService.collectStats(sec);
            } else if (cmd.equals("exit")) {
                break;
            }
        }
    }
}
