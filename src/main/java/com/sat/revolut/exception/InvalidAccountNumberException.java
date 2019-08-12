package com.sat.revolut.exception;

public class InvalidAccountNumberException extends Exception {
    public InvalidAccountNumberException(String account_number_is_invalid) {
        super(account_number_is_invalid);
    }
}
