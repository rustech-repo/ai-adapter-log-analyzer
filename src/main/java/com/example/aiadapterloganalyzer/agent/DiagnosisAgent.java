package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.IncidentAnalysisService;
import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class DiagnosisAgent implements IncidentAnalysisService {

    private final LogSignalExtractor logSignalExtractor;
    private final ArchitectureContext architectureContext;
    private final LlmDiagnosisClient llmDiagnosisClient;
    private final DiagnosisValidator diagnosisValidator;

    DiagnosisAgent(
            LogSignalExtractor logSignalExtractor,
            ArchitectureContext architectureContext,
            LlmDiagnosisClient llmDiagnosisClient,
            DiagnosisValidator diagnosisValidator
    ) {
        this.logSignalExtractor = logSignalExtractor;
        this.architectureContext = architectureContext;
        this.llmDiagnosisClient = llmDiagnosisClient;
        this.diagnosisValidator = diagnosisValidator;
    }

    @Override
    public IncidentAnalysisResponse analyze(String logs) {
        LogSignals signals = logSignalExtractor.extract(logs);
        log.info("Analyzing incident id={}, adapters={}, signalTypes={}",
                signals.incidentId(), signals.affectedAdapters(), signals.signalTypes());
        DiagnosisPromptRequest request = new DiagnosisPromptRequest(logs, signals, architectureContext.promptContext());

        String rawDiagnosis = llmDiagnosisClient.generateDiagnosis(request);
        try {
            IncidentAnalysisResponse response = diagnosisValidator.parseAndValidate(rawDiagnosis, signals, architectureContext);
            log.info("Diagnosis validated for incident id={}", response.incidentId());
            return response;
        } catch (DiagnosisValidationException firstFailure) {
            log.warn("Diagnosis validation failed for incident id={}, requesting repair: {}",
                    signals.incidentId(), firstFailure.getMessage());
            String repaired = llmDiagnosisClient.repairDiagnosis(new RepairPromptRequest(
                    rawDiagnosis,
                    firstFailure.getMessage(),
                    signals,
                    architectureContext.promptContext()
            ));
            try {
                IncidentAnalysisResponse response = diagnosisValidator.parseAndValidate(repaired, signals, architectureContext);
                log.info("Repaired diagnosis validated for incident id={}", response.incidentId());
                return response;
            } catch (DiagnosisValidationException secondFailure) {
                log.warn("Repaired diagnosis validation failed for incident id={}: {}",
                        signals.incidentId(), secondFailure.getMessage());
                throw secondFailure;
            } catch (JsonProcessingException secondFailure) {
                throw new DiagnosisValidationException("Repaired model output is not valid JSON", secondFailure);
            }
        } catch (JsonProcessingException firstFailure) {
            log.warn("Diagnosis JSON parsing failed for incident id={}, requesting repair: {}",
                    signals.incidentId(), firstFailure.getOriginalMessage());
            String repaired = llmDiagnosisClient.repairDiagnosis(new RepairPromptRequest(
                    rawDiagnosis,
                    "Model output is not valid JSON: " + firstFailure.getOriginalMessage(),
                    signals,
                    architectureContext.promptContext()
            ));
            try {
                IncidentAnalysisResponse response = diagnosisValidator.parseAndValidate(repaired, signals, architectureContext);
                log.info("Repaired diagnosis validated for incident id={}", response.incidentId());
                return response;
            } catch (JsonProcessingException secondFailure) {
                throw new DiagnosisValidationException("Repaired model output is not valid JSON", secondFailure);
            }
        }
    }
}
