package com.example.aiadapterloganalyzer.agent;

public record DiagnosisPromptRequest(
        String logs,
        LogSignals signals,
        String architectureContext
) {
}
