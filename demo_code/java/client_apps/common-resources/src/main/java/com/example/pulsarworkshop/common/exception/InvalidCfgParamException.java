package com.example.pulsarworkshop.utilities.exception;

public class InvalidCfgParamException extends RuntimeException {
    public InvalidCfgParamException(String paramName, String errDesc) {
        super("Invalid setting for parameter (" + paramName + "): " + errDesc);
        this.printStackTrace();
    }

    public InvalidCfgParamException(String fullErrDesc) {
        super(fullErrDesc);
    }
}
