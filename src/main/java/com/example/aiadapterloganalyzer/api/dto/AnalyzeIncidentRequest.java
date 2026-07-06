package com.example.aiadapterloganalyzer.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeIncidentRequest(
        @NotBlank(message = "logs must not be blank")
        String logs
) {
}
