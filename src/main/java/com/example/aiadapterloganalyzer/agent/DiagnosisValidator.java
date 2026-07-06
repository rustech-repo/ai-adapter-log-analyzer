package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.dto.HypothesisResponse;
import com.example.aiadapterloganalyzer.api.dto.ImmediateActionResponse;
import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.api.dto.NextStepResponse;
import com.example.aiadapterloganalyzer.api.dto.SummaryResponse;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class DiagnosisValidator {

    private final ObjectMapper objectMapper;

    DiagnosisValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IncidentAnalysisResponse parseAndValidate(
            String rawJson,
            LogSignals signals,
            ArchitectureContext architectureContext
    ) throws JsonProcessingException {
        IncidentAnalysisResponse response = objectMapper.readValue(rawJson, IncidentAnalysisResponse.class);
        validate(response, signals, architectureContext);
        log.debug("Validated diagnosis output for incident id={}", response.incidentId());
        return response;
    }

    private void validate(IncidentAnalysisResponse response, LogSignals signals, ArchitectureContext architectureContext) {
        requireText(response.category(), "category");
        require(response.summary() != null, "summary is required");
        require(response.hypotheses() != null && !response.hypotheses().isEmpty(), "hypotheses are required");
        require(response.hypotheses().size() <= 3, "at most 3 hypotheses are allowed");
        require(response.immediateActions() != null, "immediate_actions is required");
        require(response.immediateActions().size() <= 2, "at most 2 immediate actions are allowed");

        validateSummary(response.summary());
        validateAdapters(response.summary(), signals);
        validateSummaryAgainstSignals(response.summary(), signals);
        validateHypotheses(response.hypotheses(), architectureContext);
        validateImmediateActions(response.immediateActions());
    }

    private void validateSummary(SummaryResponse summary) {
        requireText(summary.description(), "summary.description");
        require(summary.affectedAdapters() != null, "summary.affected_adapters is required");
        require(summary.affectedOrderTypes() != null, "summary.affected_order_types is required");
        require(summary.faultLayer() != null, "summary.fault_layer is required");
        require(summary.severity() != null, "summary.severity is required");
        requireText(summary.severityReasoning(), "summary.severity_reasoning");
        require(summary.blastRadius() != null, "summary.blast_radius is required");
    }

    private void validateAdapters(SummaryResponse summary, LogSignals signals) {
        if (signals.affectedAdapters().isEmpty()) {
            return;
        }

        Set<String> allowedNames = new LinkedHashSet<>();
        for (AdapterReference adapter : signals.affectedAdapters()) {
            allowedNames.add(adapter.displayName().toLowerCase(Locale.ROOT));
            allowedNames.add(adapter.code().toLowerCase(Locale.ROOT));
            allowedNames.add(adapter.provider().toLowerCase(Locale.ROOT));
        }

        for (String adapterName : summary.affectedAdapters()) {
            String normalized = adapterName.toLowerCase(Locale.ROOT);
            boolean known = allowedNames.stream().anyMatch(normalized::contains)
                    || allowedNames.contains(normalized);
            require(known, "summary.affected_adapters contains an adapter not supported by log evidence: " + adapterName);
        }
    }

    private void validateSummaryAgainstSignals(SummaryResponse summary, LogSignals signals) {
        if (signals.signalTypes().contains("provider_degradation")) {
            require(summary.faultLayer() != FaultLayer.API,
                    "provider degradation evidence cannot be classified as API layer");
        }
        if (signals.signalTypes().contains("signature_or_credential_issue")) {
            require(summary.faultLayer() != FaultLayer.EXTERNAL,
                    "signature or credential evidence cannot be classified as External layer");
        }
        if (signals.signalTypes().contains("queue_backlog")
                || signals.signalTypes().contains("database_failure")
                || signals.signalTypes().contains("terminal_routing_or_cache")) {
            require(summary.faultLayer() == FaultLayer.INFRASTRUCTURE || summary.faultLayer() == FaultLayer.CORE,
                    "infrastructure evidence must be classified as Infrastructure or Core layer");
        }

        if (!signals.affectedOrderTypes().isEmpty()) {
            for (String orderType : summary.affectedOrderTypes()) {
                require(signals.affectedOrderTypes().contains(orderType),
                        "summary.affected_order_types contains an order type not supported by log evidence: " + orderType);
            }
        }
    }

    private void validateHypotheses(Iterable<HypothesisResponse> hypotheses, ArchitectureContext architectureContext) {
        for (HypothesisResponse hypothesis : hypotheses) {
            requireText(hypothesis.title(), "hypotheses.title");
            requireText(hypothesis.reasoning(), "hypotheses.reasoning");
            require(hypothesis.probability() != null, "hypotheses.probability is required");
            require(hypothesis.nextSteps() != null, "hypotheses.next_steps is required");
            require(hypothesis.nextSteps().size() <= 3, "each hypothesis may contain at most 3 next steps");
            for (NextStepResponse nextStep : hypothesis.nextSteps()) {
                requireText(nextStep.action(), "next_steps.action");
                requireText(nextStep.tool(), "next_steps.tool");
                requireText(nextStep.detail(), "next_steps.detail");
                require(architectureContext.allowedTools().contains(nextStep.tool()),
                        "unsupported diagnostic tool: " + nextStep.tool());
            }
        }
    }

    private void validateImmediateActions(Iterable<ImmediateActionResponse> immediateActions) {
        for (ImmediateActionResponse action : immediateActions) {
            requireText(action.action(), "immediate_actions.action");
            require(action.risk() != null, "immediate_actions.risk is required");
            requireText(action.reasoning(), "immediate_actions.reasoning");
        }
    }

    private void requireText(String value, String field) {
        require(StringUtils.hasText(value), field + " is required");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new DiagnosisValidationException(message);
        }
    }
}
