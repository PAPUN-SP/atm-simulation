package com.atm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A bank account: holds balance, PIN, lock state and transaction history. */
public class Account {
    private final String number;
    private final String holder;
    private String pin;
    private double balance;
    private int failedAttempts;
    private boolean locked;
    private final List<Transaction> history = new ArrayList<>();

    public Account(String number, String holder, String pin, double openingBalance) {
        this.number = number; this.holder = holder; this.pin = pin; this.balance = openingBalance;
    }

    public String getNumber() { return number; }
    public String getHolder() { return holder; }
    public double getBalance() { return balance; }
    public boolean isLocked() { return locked; }
    public int getFailedAttempts() { return failedAttempts; }

    public boolean checkPin(String candidate) { return pin.equals(candidate); }
    public void recordFailure(int maxTries) { if (++failedAttempts >= maxTries) locked = true; }
    public void resetFailures() { failedAttempts = 0; }

    public void deposit(double amount) {
        balance += amount;
        log("DEPOSIT", amount);
    }

    public void withdraw(double amount) {
        if (amount > balance) throw new AtmException(400, "Insufficient balance");
        balance -= amount;
        log("WITHDRAW", amount);
    }

    public void changePin(String newPin) {
        this.pin = newPin;
        log("PIN_CHANGE", 0);
    }

    /** Most recent transactions first. */
    public List<Transaction> miniStatement(int count) {
        int from = Math.max(0, history.size() - count);
        List<Transaction> last = new ArrayList<>(history.subList(from, history.size()));
        java.util.Collections.reverse(last);
        return last;
    }

    private void log(String type, double amount) {
        history.add(new Transaction(LocalDateTime.now(), type, amount, balance));
    }
}
