package com.example.aiadapterloganalyzer.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
class PromptResources {

    private final String systemPrompt;
    private final String diagnosisPrompt;
    private final String repairPrompt;

    PromptResources(
            @Value("classpath:prompts/system.md") Resource systemPrompt,
            @Value("classpath:prompts/diagnosis.md") Resource diagnosisPrompt,
            @Value("classpath:prompts/repair.md") Resource repairPrompt
    ) {
        this.systemPrompt = read(systemPrompt);
        this.diagnosisPrompt = read(diagnosisPrompt);
        this.repairPrompt = read(repairPrompt);
    }

    String systemPrompt() {
        return systemPrompt;
    }

    String diagnosisPrompt(DiagnosisPromptRequest request) {
        return diagnosisPrompt.formatted(
                request.architectureContext(),
                request.signals(),
                request.logs()
        );
    }

    String repairPrompt(RepairPromptRequest request) {
        return repairPrompt.formatted(
                request.validationError(),
                request.architectureContext(),
                request.signals(),
                request.invalidOutput()
        );
    }

    private String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load prompt resource " + resource, e);
        }
    }
}
