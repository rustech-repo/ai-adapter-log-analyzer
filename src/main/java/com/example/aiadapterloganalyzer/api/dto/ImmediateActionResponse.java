package com.example.aiadapterloganalyzer.api.dto;

import com.example.aiadapterloganalyzer.domain.ActionRisk;
import lombok.Builder;

@Builder
public record ImmediateActionResponse(
        String action,
        ActionRisk risk,
        String reasoning
) {
}
