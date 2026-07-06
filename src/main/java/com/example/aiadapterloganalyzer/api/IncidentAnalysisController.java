package com.example.aiadapterloganalyzer.api;

import com.example.aiadapterloganalyzer.api.dto.AnalyzeIncidentRequest;
import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
class IncidentAnalysisController {

    private final IncidentAnalysisService incidentAnalysisService;

    IncidentAnalysisController(IncidentAnalysisService incidentAnalysisService) {
        this.incidentAnalysisService = incidentAnalysisService;
    }

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.OK)
    IncidentAnalysisResponse analyze(@Valid @RequestBody AnalyzeIncidentRequest request) {
        return incidentAnalysisService.analyze(request.logs());
    }
}
