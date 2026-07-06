package com.example.aiadapterloganalyzer.api;

public record ApiErrorResponse(
        String error,
        String message
) {
}
