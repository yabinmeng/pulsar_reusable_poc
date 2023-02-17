package com.example.pulsarworkshop.common.exception;

public class CliOptProcRuntimeException extends RuntimeException {

    int errorExitCode;
    String errorDescription;

    public CliOptProcRuntimeException(int errorCode, String errorDescription) {
        super(errorDescription);
        this.errorExitCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorExitCode() { return errorExitCode; }

    public String getErrorDescription() { return errorDescription; }
}
