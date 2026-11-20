package com.atm;

import java.util.Scanner;

/** Console version of the ATM (the original project brief). */
public class ConsoleApp {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner in = new Scanner(System.in);
        System.out.println("=== ATM === (demo: 1001/1234, 1002/4321)");
        String token = null;
        while (token == null) {
            System.out.print("Account number: "); String no = in.nextLine().trim();
            System.out.print("PIN: "); String pin = in.nextLine().trim();
            try { token = bank.login(no, pin); }
            catch (AtmException e) { System.out.println("! " + e.getMessage()); if (e.status == 403) return; }
        }
        Account a = bank.auth(token);
        System.out.println("Welcome, " + a.getHolder() + "!");
        while (true) {
            System.out.print("\n1.Withdraw 2.Deposit 3.Balance 4.Mini Statement 5.Change PIN 6.Exit > ");
            String c = in.nextLine().trim();
            try {
                switch (c) {
                    case "1" -> { System.out.print("Amount: ");
                        System.out.printf("Collect cash. Balance: %.2f%n", bank.withdraw(a, Double.parseDouble(in.nextLine()))); }
                    case "2" -> { System.out.print("Amount: ");
                        System.out.printf("Deposited. Balance: %.2f%n", bank.deposit(a, Double.parseDouble(in.nextLine()))); }
                    case "3" -> System.out.printf("Balance: %.2f%n", a.getBalance());
                    case "4" -> bank.statement(a).forEach(t -> System.out.printf("%s  %-10s %10.2f  bal %10.2f%n",
                            t.time().withNano(0), t.type(), t.amount(), t.balanceAfter()));
                    case "5" -> { System.out.print("Old PIN: "); String o = in.nextLine();
                        System.out.print("New PIN: "); bank.changePin(a, o, in.nextLine()); System.out.println("PIN changed."); }
                    case "6" -> { bank.logout(token); System.out.println("Thank you!"); return; }
                    default -> System.out.println("Invalid option");
                }
            } catch (AtmException e) { System.out.println("! " + e.getMessage());
            } catch (NumberFormatException e) { System.out.println("! Enter a valid number"); }
        }
    }
}
