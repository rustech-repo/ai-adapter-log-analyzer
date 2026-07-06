package com.example.aiadapterloganalyzer.agent;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class PromptResourcesTest {

    private final PromptResources promptResources = new PromptResources(
            new ClassPathResource("prompts/system.md"),
            new ClassPathResource("prompts/diagnosis.md"),
            new ClassPathResource("prompts/repair.md")
    );

    @Test
    void loadsSystemPromptWithRequiredSchemaAndLimits() {
        String systemPrompt = promptResources.systemPrompt();

        assertThat(systemPrompt)
                .contains("Return only valid JSON")
                .contains("\"incident_id\"")
                .contains("\"hypotheses\"")
                .contains("at most 3 hypotheses")
                .contains("at most 2 immediate actions");
    }

    @Test
    void formatsDiagnosisPromptWithContextSignalsAndLogs() {
        LogSignals signals = new LogSignals(
                "INC-201",
                java.util.List.of(new AdapterReference("cc109", "OPay")),
                java.util.Set.of("PAYMENT"),
                java.util.Set.of(503),
                java.util.Set.of("provider_degradation"),
                java.util.Set.of("req-1"),
                "2024-11-15 14:23:01.445",
                "2024-11-15 14:23:05.115"
        );

        String prompt = promptResources.diagnosisPrompt(new DiagnosisPromptRequest(
                "status=503",
                signals,
                "Allowed diagnostic tools: ELK, Grafana"
        ));

        assertThat(prompt)
                .contains("Allowed diagnostic tools: ELK, Grafana")
                .contains("INC-201")
                .contains("provider_degradation")
                .contains("status=503");
    }

    @Test
    void formatsRepairPromptWithValidationErrorAndInvalidOutput() {
        String prompt = promptResources.repairPrompt(new RepairPromptRequest(
                "{\"tool\":\"Browser\"}",
                "unsupported diagnostic tool: Browser",
                new LogSignals(null, java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                        java.util.Set.of(), java.util.Set.of(), null, null),
                "Allowed diagnostic tools: ELK"
        ));

        assertThat(prompt)
                .contains("unsupported diagnostic tool: Browser")
                .contains("Allowed diagnostic tools: ELK")
                .contains("{\"tool\":\"Browser\"}")
                .contains("Return only corrected valid JSON");
    }
}
