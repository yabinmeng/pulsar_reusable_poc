package com.example.pulsarworkshop.common.exception;

public class WorkshopRuntimException extends RuntimeException {
    public WorkshopRuntimException(String paramName, String errDesc) {
        super("Invalid setting for parameter (" + paramName + "): " + errDesc);
        this.printStackTrace();
    }

    public WorkshopRuntimException(String fullErrDesc) {
        super(fullErrDesc);
    }
}
