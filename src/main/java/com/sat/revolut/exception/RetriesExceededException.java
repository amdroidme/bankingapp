package com.sat.revolut.exception;

public class RetriesExceededException extends Exception {
    public RetriesExceededException(String s) {
        super(s);
    }
}
