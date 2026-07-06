package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosisValidatorTest {

    private final ArchitectureContext architectureContext = new ArchitectureContext();
    private final DiagnosisValidator validator = new DiagnosisValidator(new ObjectMapper());
    private final LogSignals signals = new LogSignalExtractor(architectureContext).extract("""
            [INC-201] Mass payment failures on adapter cc109 (OPay)
            2024-11-15 14:23:01.445 ERROR c.c.m.client.opay.ClientService
              - POST https://api.opay.ng/v3/payments/create failed:
                status=503, body={"message":"Service temporarily unavailable","code":"MAINTENANCE"}
            """);

    @Test
    void parsesValidDiagnosisJson() throws Exception {
        IncidentAnalysisResponse response = validator.parseAndValidate(validJson(), signals, architectureContext);

        assertThat(response.incidentId()).isEqualTo("INC-201");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.EXTERNAL);
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> validator.parseAndValidate("{bad json", signals, architectureContext))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> validator.parseAndValidate("""
                {
                  "incident_id": "INC-201",
                  "summary": null,
                  "hypotheses": [],
                  "immediate_actions": []
                }
                """, signals, architectureContext))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("category is required");
    }

    @Test
    void rejectsUnexpectedEnumValues() {
        assertThatThrownBy(() -> validator.parseAndValidate(validJson().replace("\"high\"", "\"urgent\""),
                signals, architectureContext))
                .isInstanceOf(JsonProcessingException.class)
                .hasMessageContaining("Unknown severity");
    }

    @Test
    void rejectsHallucinatedAdapters() {
        assertThatThrownBy(() -> validator.parseAndValidate(validJson().replace("cc109 (OPay)", "cc999 (FakePay)"),
                signals, architectureContext))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("not supported by log evidence");
    }

    @Test
    void rejectsUnsupportedDiagnosticTools() {
        assertThatThrownBy(() -> validator.parseAndValidate(validJson().replace("\"ELK\"", "\"Browser\""),
                signals, architectureContext))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("unsupported diagnostic tool");
    }

    @Test
    void rejectsHypothesesThatContradictLogEvidence() {
        assertThatThrownBy(() -> validator.parseAndValidate(validJson().replace("\"External\"", "\"API\""),
                signals, architectureContext))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("provider degradation evidence");
    }

    @Test
    void rejectsOrderTypesNotPresentInLogEvidence() {
        assertThatThrownBy(() -> validator.parseAndValidate(validJson().replace("\"PAYMENT\"", "\"PAYOUT\""),
                signals, architectureContext))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("order type not supported");
    }

    static String validJson() {
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
}
