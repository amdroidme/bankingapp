package com.sat.revolut.exception;

public class LowBalanceException extends RuntimeException {

    public LowBalanceException(String s) {
        super(s);
    }
}
