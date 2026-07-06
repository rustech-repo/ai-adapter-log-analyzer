package com.example.aiadapterloganalyzer.api;

import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;

public interface IncidentAnalysisService {

    IncidentAnalysisResponse analyze(String logs);
}
