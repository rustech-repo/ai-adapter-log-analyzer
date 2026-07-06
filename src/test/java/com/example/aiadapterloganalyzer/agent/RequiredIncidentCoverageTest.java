package com.example.aiadapterloganalyzer.agent;

import com.example.aiadapterloganalyzer.api.dto.IncidentAnalysisResponse;
import com.example.aiadapterloganalyzer.domain.BlastRadius;
import com.example.aiadapterloganalyzer.domain.FaultLayer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredIncidentCoverageTest {

    private final ArchitectureContext architectureContext = new ArchitectureContext();
    private final LogSignalExtractor extractor = new LogSignalExtractor(architectureContext);
    private final RuleBasedFallbackDiagnosisClient fallbackClient = new RuleBasedFallbackDiagnosisClient(new ObjectMapper());
    private final DiagnosisValidator validator = new DiagnosisValidator(new ObjectMapper());

    @Test
    void coversProviderDegradationIncident() throws Exception {
        IncidentAnalysisResponse response = analyze("inc-201-opay-provider-503.log");

        assertThat(response.incidentId()).isEqualTo("INC-201");
        assertThat(response.category()).isEqualTo("External provider degradation");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.EXTERNAL);
        assertThat(response.summary().affectedAdapters()).containsExactly("cc109 (OPay)");
    }

    @Test
    void coversCredentialConfigurationIncident() throws Exception {
        IncidentAnalysisResponse response = analyze("inc-202-payme-signature.log");

        assertThat(response.incidentId()).isEqualTo("INC-202");
        assertThat(response.category()).isEqualTo("Internal configuration / credential issue");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.API);
        assertThat(response.summary().affectedAdapters()).containsExactly("cc087 (PayMe)");
    }

    @Test
    void coversInfrastructureQueueIncident() throws Exception {
        IncidentAnalysisResponse response = analyze("inc-203-halopesa-rabbitmq-backlog.log");

        assertThat(response.incidentId()).isEqualTo("INC-203");
        assertThat(response.category()).isEqualTo("Infrastructure failure (DB/cache/queue)");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.INFRASTRUCTURE);
        assertThat(response.hypotheses().getFirst().nextSteps().getFirst().tool()).isEqualTo("RabbitMQ");
    }

    @Test
    void coversRoutingTerminalConfigurationIncident() throws Exception {
        IncidentAnalysisResponse response = analyze("inc-204-terminal-link-routing.log");

        assertThat(response.incidentId()).isEqualTo("INC-204");
        assertThat(response.category()).isEqualTo("Routing / terminal configuration issue");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.INFRASTRUCTURE);
        assertThat(response.summary().blastRadius()).isEqualTo(BlastRadius.MULTI_ADAPTER);
        assertThat(response.summary().affectedAdapters()).containsExactly("cc109 (OPay)", "cc131 (Cobre)");
    }

    @Test
    void coversConnectionPoolExhaustionIncident() throws Exception {
        IncidentAnalysisResponse response = analyze("inc-205-payme-pool-exhaustion.log");

        assertThat(response.incidentId()).isEqualTo("INC-205");
        assertThat(response.category()).isEqualTo("Connection pool / resource exhaustion");
        assertThat(response.summary().faultLayer()).isEqualTo(FaultLayer.SDK);
        assertThat(response.summary().affectedAdapters()).containsExactly("cc087 (PayMe)");
    }

    private IncidentAnalysisResponse analyze(String fixture) throws Exception {
        String logs = IncidentFixture.load(fixture);
        LogSignals signals = extractor.extract(logs);
        String rawDiagnosis = fallbackClient.generateDiagnosis(new DiagnosisPromptRequest(
                logs,
                signals,
                architectureContext.promptContext()
        ));
        return validator.parseAndValidate(rawDiagnosis, signals, architectureContext);
    }
}
