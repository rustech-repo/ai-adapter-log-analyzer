package com.example.aiadapterloganalyzer.api;

import com.example.aiadapterloganalyzer.agent.DiagnosisValidationException;
import com.example.aiadapterloganalyzer.api.dto.HypothesisResponse;
import com.example.aiadapterloganalyzer.api.dto.ImmediateActionResponse;
import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.api.dto.NextStepResponse;
import com.example.aiadapterloganalyzer.api.dto.SummaryResponse;
import com.example.aiadapterloganalyzer.domain.ActionRisk;
import com.example.aiadapterloganalyzer.domain.BlastRadius;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.example.aiadapterloganalyzer.domain.Probability;
import com.example.aiadapterloganalyzer.domain.Severity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentAnalysisControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        IncidentAnalysisController controller = new IncidentAnalysisController(logs -> response());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void analyzeReturnsAssignmentResponseShape() throws Exception {
        mockMvc.perform(post("/api/incidents/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "logs": "[INC-201] Mass payment failures on adapter cc109 (OPay)"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident_id").value("INC-201"))
                .andExpect(jsonPath("$.category").value("External provider degradation"))
                .andExpect(jsonPath("$.summary.description").value("OPay is returning 503 for payment creation requests."))
                .andExpect(jsonPath("$.summary.affected_adapters[0]").value("cc109 (OPay)"))
                .andExpect(jsonPath("$.summary.affected_order_types[0]").value("PAYMENT"))
                .andExpect(jsonPath("$.summary.fault_layer").value("External"))
                .andExpect(jsonPath("$.summary.severity").value("high"))
                .andExpect(jsonPath("$.summary.severity_reasoning").value("Mass payment failures are directly impacting revenue."))
                .andExpect(jsonPath("$.summary.blast_radius").value("single_adapter"))
                .andExpect(jsonPath("$.hypotheses", hasSize(1)))
                .andExpect(jsonPath("$.hypotheses[0].probability").value("likely"))
                .andExpect(jsonPath("$.hypotheses[0].next_steps[0].tool").value("ELK"))
                .andExpect(jsonPath("$.immediate_actions", hasSize(1)))
                .andExpect(jsonPath("$.immediate_actions[0].risk").value("caution"));
    }

    @Test
    void analyzeRejectsBlankLogs() throws Exception {
        mockMvc.perform(post("/api/incidents/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "logs": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyzeReturnsControlledErrorWhenDiagnosisValidationFails() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        IncidentAnalysisController controller = new IncidentAnalysisController(logs -> {
            throw new DiagnosisValidationException("Repaired model output is not valid JSON");
        });
        MockMvc errorMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();

        errorMockMvc.perform(post("/api/incidents/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "logs": "[INC-201] status=503"
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("diagnosis_validation_failed"))
                .andExpect(jsonPath("$.message").value("Repaired model output is not valid JSON"));
    }

    private static IncidentAnalysisResponse response() {
        return new IncidentAnalysisResponse(
                "INC-201",
                "External provider degradation",
                new SummaryResponse(
                        "OPay is returning 503 for payment creation requests.",
                        List.of("cc109 (OPay)"),
                        List.of("PAYMENT"),
                        FaultLayer.EXTERNAL,
                        Severity.HIGH,
                        "Mass payment failures are directly impacting revenue.",
                        BlastRadius.SINGLE_ADAPTER
                ),
                List.of(new HypothesisResponse(
                        "OPay maintenance",
                        "Provider responses include HTTP 503.",
                        Probability.LIKELY,
                        List.of(new NextStepResponse(
                                "Confirm provider-side error pattern",
                                "ELK",
                                "Search adapter cc109 errors by request id and HTTP status."
                        ))
                )),
                List.of(new ImmediateActionResponse(
                        "Switch eligible traffic to fallback provider",
                        ActionRisk.CAUTION,
                        "This can restore payments but needs merchant routing confirmation."
                ))
        );
    }
}
