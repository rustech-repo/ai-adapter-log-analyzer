package com.example.aiadapterloganalyzer.agent;

public class DiagnosisValidationException extends RuntimeException {

    public DiagnosisValidationException(String message) {
        super(message);
    }

    public DiagnosisValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
