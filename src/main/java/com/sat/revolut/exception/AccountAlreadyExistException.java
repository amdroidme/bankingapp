package com.sat.revolut.exception;

public class AccountAlreadyExistException extends Exception {
    public AccountAlreadyExistException(String account_already_exist) {
        super(account_already_exist);
    }
}
