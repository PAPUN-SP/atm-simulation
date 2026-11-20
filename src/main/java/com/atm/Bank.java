package com.atm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bank service: accounts, sessions and all ATM business rules. */
public class Bank {
    public static final double MAX_WITHDRAW = 20000, MAX_DEPOSIT = 50000;
    public static final int MAX_TRIES = 3, NOTE_MULTIPLE = 100;

    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, String> sessions = new HashMap<>(); // token -> account number

    public Bank() {
        add(new Account("1001", "Soumendra", "1234", 5000));
        add(new Account("1002", "Bob Rao", "4321", 12000));
        add(new Account("1003", "Carol Singh", "1111", 250));
    }

    private void add(Account a) { accounts.put(a.getNumber(), a); }

    public synchronized String login(String accNo, String pin) {
        Account a = accounts.get(accNo == null ? "" : accNo);
        if (a == null) throw new AtmException(401, "Invalid account number or PIN");
        if (a.isLocked()) throw new AtmException(403, "Card blocked after too many wrong PINs");
        if (!a.checkPin(pin)) {
            a.recordFailure(MAX_TRIES);
            int left = MAX_TRIES - a.getFailedAttempts();
            throw new AtmException(401, a.isLocked() ? "Card blocked after too many wrong PINs"
                    : "Invalid account number or PIN (" + left + " attempt(s) left)");
        }
        a.resetFailures();
        String token = UUID.randomUUID().toString();
        sessions.put(token, accNo);
        return token;
    }

    public synchronized Account auth(String token) {
        String no = token == null ? null : sessions.get(token);
        if (no == null) throw new AtmException(401, "Session expired. Please log in again");
        return accounts.get(no);
    }

    public synchronized void logout(String token) { sessions.remove(token); }

    public synchronized double withdraw(Account a, double amt) {
        if (amt <= 0 || amt % NOTE_MULTIPLE != 0)
            throw new AtmException(400, "Amount must be a multiple of " + NOTE_MULTIPLE);
        if (amt > MAX_WITHDRAW) throw new AtmException(400, "Limit per withdrawal is " + (int) MAX_WITHDRAW);
        a.withdraw(amt);
        return a.getBalance();
    }

    public synchronized double deposit(Account a, double amt) {
        if (amt <= 0) throw new AtmException(400, "Enter a valid amount");
        if (amt > MAX_DEPOSIT) throw new AtmException(400, "Limit per deposit is " + (int) MAX_DEPOSIT);
        a.deposit(amt);
        return a.getBalance();
    }

    public synchronized void changePin(Account a, String oldPin, String newPin) {
        if (!a.checkPin(oldPin)) throw new AtmException(401, "Current PIN is incorrect");
        if (newPin == null || !newPin.matches("\\d{4}")) throw new AtmException(400, "New PIN must be 4 digits");
        if (newPin.equals(oldPin)) throw new AtmException(400, "New PIN must differ from the old one");
        a.changePin(newPin);
    }

    public synchronized List<Transaction> statement(Account a) { return a.miniStatement(5); }
}
