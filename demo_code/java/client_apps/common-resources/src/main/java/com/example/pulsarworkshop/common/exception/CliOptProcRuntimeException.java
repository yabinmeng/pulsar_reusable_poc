package com.example.pulsarworkshop.utilities.exception;

public class CliOptProcRuntimeException extends RuntimeException {

    int systemErrExitCode;

    public CliOptProcRuntimeException(int errorCode, String errorDescription) {
        super(errorDescription);
        this.systemErrExitCode = errorCode;
        if (errorCode != 0) {
            this.printStackTrace();
        }
    }

    public int getSystemErrExitCode() { return systemErrExitCode; }
}
