package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.example.aiadapterloganalyzer.domain.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosisAgentTest {

    private final ArchitectureContext architectureContext = new ArchitectureContext();
    private final LogSignalExtractor extractor = new LogSignalExtractor(architectureContext);
    private final DiagnosisValidator validator = new DiagnosisValidator(new ObjectMapper());

    @Test
    void repairsInvalidModelOutputOnce() {
        FakeLlmDiagnosisClient llmClient = new FakeLlmDiagnosisClient(
                """
                        {
                          "incident_id": "INC-201",
                          "category": "External provider degradation",
                          "summary": {
                            "description": "OPay is returning 503.",
                            "affected_adapters": ["cc109 (OPay)"],
                            "affected_order_types": ["PAYMENT"],
                            "fault_layer": "External",
                            "severity": "high",
                            "severity_reasoning": "Mass failures.",
                            "blast_radius": "single_adapter"
                          },
                          "hypotheses": [{
                            "title": "Provider maintenance",
                            "reasoning": "503 with MAINTENANCE.",
                            "probability": "likely",
                            "next_steps": [{
                              "action": "Check provider page",
                              "tool": "Browser",
                              "detail": "Not an allowed infrastructure tool."
                            }]
                          }],
                          "immediate_actions": [{
                            "action": "Reroute traffic",
                            "risk": "caution",
                            "reasoning": "Restores payment capability."
                          }]
                        }
                        """,
                validDiagnosisJson()
        );
        DiagnosisAgent agent = new DiagnosisAgent(extractor, architectureContext, llmClient, validator);

        IncidentAnalysisResponse response = agent.analyze("""
                [INC-201] Mass payment failures on adapter cc109 (OPay)
                2024-11-15 14:23:01.445 ERROR c.c.m.client.opay.ClientService
                  - POST https://api.opay.ng/v3/payments/create failed:
                    status=503, body={"message":"Service temporarily unavailable","code":"MAINTENANCE"}
                """);

        assertThat(llmClient.repairCalls).isEqualTo(1);
        assertThat(response.incidentId()).isEqualTo("INC-201");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.EXTERNAL);
        assertThat(response.summary().severity()).isEqualTo(Severity.HIGH);
        assertThat(response.hypotheses().getFirst().nextSteps().getFirst().tool()).isEqualTo("ELK");
    }

    @Test
    void throwsControlledExceptionWhenRepairStillFails() {
        FakeLlmDiagnosisClient llmClient = new FakeLlmDiagnosisClient("{bad json", "{still bad");
        DiagnosisAgent agent = new DiagnosisAgent(extractor, architectureContext, llmClient, validator);

        assertThatThrownBy(() -> agent.analyze("""
                [INC-201] Mass payment failures on adapter cc109 (OPay)
                2024-11-15 14:23:01.445 ERROR c.c.m.client.opay.ClientService
                  - POST https://api.opay.ng/v3/payments/create failed:
                    status=503
                """))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Repaired model output is not valid JSON");
        assertThat(llmClient.repairCalls).isEqualTo(1);
    }

    private static String validDiagnosisJson() {
        return """
                {
                  "incident_id": "INC-201",
                  "category": "External provider degradation",
                  "summary": {
                    "description": "OPay is returning 503 with MAINTENANCE for payment creation.",
                    "affected_adapters": ["cc109 (OPay)"],
                    "affected_order_types": ["PAYMENT"],
                    "fault_layer": "External",
                    "severity": "high",
                    "severity_reasoning": "Repeated provider 503 responses affect payment traffic.",
                    "blast_radius": "single_adapter"
                  },
                  "hypotheses": [{
                    "title": "Provider maintenance",
                    "reasoning": "The provider response includes HTTP 503 and MAINTENANCE.",
                    "probability": "likely",
                    "next_steps": [{
                      "action": "Confirm provider-side error rate",
                      "tool": "ELK",
                      "detail": "Query cc109 errors by HTTP status and timestamp."
                    }]
                  }],
                  "immediate_actions": [{
                    "action": "Route eligible traffic to fallback provider",
                    "risk": "caution",
                    "reasoning": "Requires terminal routing confirmation."
                  }]
                }
                """;
    }

    private static final class FakeLlmDiagnosisClient implements LlmDiagnosisClient {

        private final String diagnosis;
        private final String repair;
        private int repairCalls;

        private FakeLlmDiagnosisClient(String diagnosis, String repair) {
            this.diagnosis = diagnosis;
            this.repair = repair;
        }

        @Override
        public String generateDiagnosis(DiagnosisPromptRequest request) {
            return diagnosis;
        }

        @Override
        public String repairDiagnosis(RepairPromptRequest request) {
            repairCalls++;
            return repair;
        }
    }
}
