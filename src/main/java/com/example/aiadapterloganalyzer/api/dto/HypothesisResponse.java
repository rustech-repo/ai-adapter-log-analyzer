package com.example.aiadapterloganalyzer.api.dto;

import com.example.aiadapterloganalyzer.domain.Probability;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record HypothesisResponse(
        String title,
        String reasoning,
        Probability probability,
        @JsonProperty("next_steps")
        List<NextStepResponse> nextSteps
) {
}
