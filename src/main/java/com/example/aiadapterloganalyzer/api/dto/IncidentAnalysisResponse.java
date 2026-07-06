package com.example.aiadapterloganalyzer.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record IncidentAnalysisResponse(
        @JsonProperty("incident_id")
        String incidentId,
        String category,
        SummaryResponse summary,
        List<HypothesisResponse> hypotheses,
        @JsonProperty("immediate_actions")
        List<ImmediateActionResponse> immediateActions
) {
}
