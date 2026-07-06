package com.example.aiadapterloganalyzer.agent;

public interface LlmDiagnosisClient {

    String generateDiagnosis(DiagnosisPromptRequest request);

    String repairDiagnosis(RepairPromptRequest request);
}
