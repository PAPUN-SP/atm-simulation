package com.atm;

import java.time.LocalDateTime;

/** Immutable record of one account operation. */
public record Transaction(LocalDateTime time, String type, double amount, double balanceAfter) { }
