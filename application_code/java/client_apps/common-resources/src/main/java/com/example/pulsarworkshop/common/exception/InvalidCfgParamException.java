package com.example.pulsarworkshop.common.exception;

public class InvalidCfgParamException extends RuntimeException {

    private String errorDescription;

    public InvalidCfgParamException(String paramName, String errDesc) {
        super("Invalid setting for parameter (" + paramName + "): " + errDesc);
        this.errorDescription = "Invalid setting for parameter (" + paramName + "): " + errDesc;
    }

    public InvalidCfgParamException(String fullErrDesc) {
        super(fullErrDesc);
        this.errorDescription = fullErrDesc;
    }

    public String getErrorDescription() { return errorDescription; }

}
