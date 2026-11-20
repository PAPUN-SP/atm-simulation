package com.atm;

/** Business-rule failure carrying an HTTP-style status code. */
public class AtmException extends RuntimeException {
    public final int status;
    public AtmException(int status, String message) { super(message); this.status = status; }
}
