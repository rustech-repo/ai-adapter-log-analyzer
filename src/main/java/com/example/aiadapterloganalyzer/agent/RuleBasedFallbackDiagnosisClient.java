package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.dto.HypothesisResponse;
import com.example.aiadapterloganalyzer.api.dto.ImmediateActionResponse;
import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.api.dto.NextStepResponse;
import com.example.aiadapterloganalyzer.api.dto.SummaryResponse;
import com.example.aiadapterloganalyzer.domain.ActionRisk;
import com.example.aiadapterloganalyzer.domain.BlastRadius;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.example.aiadapterloganalyzer.domain.Probability;
import com.example.aiadapterloganalyzer.domain.Severity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class RuleBasedFallbackDiagnosisClient implements LlmDiagnosisClient {

    private final ObjectMapper objectMapper;

    RuleBasedFallbackDiagnosisClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String generateDiagnosis(DiagnosisPromptRequest request) {
        log.info("Using rule-based fallback diagnosis for incident id={}", request.signals().incidentId());
        return writeJson(buildResponse(request.signals()));
    }

    @Override
    public String repairDiagnosis(RepairPromptRequest request) {
        log.info("Using rule-based fallback repair for incident id={}", request.signals().incidentId());
        return writeJson(buildResponse(request.signals()));
    }

    private IncidentAnalysisResponse buildResponse(LogSignals signals) {
        String category = category(signals);
        FaultLayer faultLayer = faultLayer(signals);
        Severity severity = severity(signals);
        BlastRadius blastRadius = signals.affectedAdapters().size() > 1
                ? BlastRadius.MULTI_ADAPTER
                : BlastRadius.SINGLE_ADAPTER;
        List<String> adapters = signals.affectedAdapters().stream()
                .map(AdapterReference::displayName)
                .toList();

        return IncidentAnalysisResponse.builder()
                .incidentId(signals.incidentId())
                .category(category)
                .summary(SummaryResponse.builder()
                        .description("Detected " + category.toLowerCase() + " from adapter log evidence.")
                        .affectedAdapters(adapters)
                        .affectedOrderTypes(signals.affectedOrderTypes().stream().toList())
                        .faultLayer(faultLayer)
                        .severity(severity)
                        .severityReasoning("Severity is inferred from repeated failures, affected adapters, and operational impact signals.")
                        .blastRadius(blastRadius)
                        .build())
                .hypotheses(List.of(HypothesisResponse.builder()
                        .title(category)
                        .reasoning("Log signals include: " + signals.signalTypes())
                        .probability(Probability.LIKELY)
                        .nextSteps(List.of(nextStep(signals)))
                        .build()))
                .immediateActions(List.of(ImmediateActionResponse.builder()
                        .action(immediateAction(category))
                        .risk(ActionRisk.CAUTION)
                        .reasoning("Action should be confirmed against live observability data before execution.")
                        .build()))
                .build();
    }

    private String category(LogSignals signals) {
        if (signals.signalTypes().contains("connection_pool_exhaustion")) {
            return "Connection pool / resource exhaustion";
        }
        if (signals.signalTypes().contains("signature_or_credential_issue")) {
            return "Internal configuration / credential issue";
        }
        if (signals.signalTypes().contains("queue_backlog") || signals.signalTypes().contains("database_failure")) {
            return "Infrastructure failure (DB/cache/queue)";
        }
        if (signals.signalTypes().contains("terminal_routing_or_cache")) {
            return "Routing / terminal configuration issue";
        }
        if (signals.signalTypes().contains("provider_degradation")) {
            return "External provider degradation";
        }
        return "Unknown adapter incident";
    }

    private FaultLayer faultLayer(LogSignals signals) {
        if (signals.signalTypes().contains("provider_degradation")) {
            return FaultLayer.EXTERNAL;
        }
        if (signals.signalTypes().contains("signature_or_credential_issue")) {
            return FaultLayer.API;
        }
        if (signals.signalTypes().contains("queue_backlog")
                || signals.signalTypes().contains("database_failure")
                || signals.signalTypes().contains("terminal_routing_or_cache")) {
            return FaultLayer.INFRASTRUCTURE;
        }
        return FaultLayer.SDK;
    }

    private Severity severity(LogSignals signals) {
        if (signals.signalTypes().contains("connection_pool_exhaustion")
                || signals.signalTypes().contains("queue_backlog")) {
            return Severity.CRITICAL;
        }
        if (signals.httpStatuses().stream().anyMatch(status -> status >= 500)
                || signals.signalTypes().contains("signature_or_credential_issue")) {
            return Severity.HIGH;
        }
        return Severity.MEDIUM;
    }

    private NextStepResponse nextStep(LogSignals signals) {
        if (signals.signalTypes().contains("signature_or_credential_issue")) {
            return NextStepResponse.builder()
                    .action("Compare configured signing keys with deployed adapter version")
                    .tool("Vault")
                    .detail("Inspect current adapter credentials and signing key material for the affected adapter.")
                    .build();
        }
        if (signals.signalTypes().contains("queue_backlog")) {
            return NextStepResponse.builder()
                    .action("Check queue depth and consumer throughput")
                    .tool("RabbitMQ")
                    .detail("Inspect callback queues, DLQ counts, and consumer lag.")
                    .build();
        }
        if (signals.signalTypes().contains("terminal_routing_or_cache")) {
            return NextStepResponse.builder()
                    .action("Inspect terminal link cache entries")
                    .tool("Redis")
                    .detail("Check terminal-links keys for affected terminals and compare with Terminals Service state.")
                    .build();
        }
        return NextStepResponse.builder()
                .action("Confirm error rate and affected adapter scope")
                .tool("ELK")
                .detail("Query affected adapter logs by timestamp, HTTP status, and request id.")
                .build();
    }

    private String immediateAction(String category) {
        if (category.contains("Connection pool")) {
            return "Reduce pressure by scaling adapter instances or temporarily lowering request concurrency.";
        }
        if (category.contains("credential")) {
            return "Rollback the adapter commons change or restart after verifying Vault secrets.";
        }
        if (category.contains("Routing")) {
            return "Flush affected Redis terminal-link cache keys after confirming current terminal configuration.";
        }
        return "Route eligible traffic to a fallback provider or enable fast-fail behavior.";
    }

    private String writeJson(IncidentAnalysisResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize fallback diagnosis", e);
        }
    }
}
