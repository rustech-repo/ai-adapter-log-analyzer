package com.example.aiadapterloganalyzer.api.dto;

import com.example.aiadapterloganalyzer.domain.BlastRadius;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.example.aiadapterloganalyzer.domain.Severity;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record SummaryResponse(
        String description,
        @JsonProperty("affected_adapters")
        List<String> affectedAdapters,
        @JsonProperty("affected_order_types")
        List<String> affectedOrderTypes,
        @JsonProperty("fault_layer")
        FaultLayer faultLayer,
        Severity severity,
        @JsonProperty("severity_reasoning")
        String severityReasoning,
        @JsonProperty("blast_radius")
        BlastRadius blastRadius
) {
}
