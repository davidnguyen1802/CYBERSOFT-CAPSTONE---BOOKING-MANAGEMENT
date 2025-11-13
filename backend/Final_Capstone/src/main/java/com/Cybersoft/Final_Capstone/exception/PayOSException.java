package com.Cybersoft.Final_Capstone.exception;

public class PayOSException extends RuntimeException {
    public PayOSException(String message) {
        super(message);
    }

    public PayOSException(String message, Throwable cause) {
        super(message, cause);
    }
}
