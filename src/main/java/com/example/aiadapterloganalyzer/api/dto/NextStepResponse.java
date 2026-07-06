package com.example.aiadapterloganalyzer.api.dto;

import lombok.Builder;

@Builder
public record NextStepResponse(
        String action,
        String tool,
        String detail
) {
}
