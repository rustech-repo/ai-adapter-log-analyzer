package com.example.aiadapterloganalyzer.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSignalExtractorTest {

    private final LogSignalExtractor extractor = new LogSignalExtractor(new ArchitectureContext());

    @Test
    void extractsProviderDegradationSignals() {
        LogSignals signals = extractor.extract(IncidentFixture.load("inc-201-opay-provider-503.log"));

        assertThat(signals.incidentId()).isEqualTo("INC-201");
        assertThat(signals.affectedAdapters()).extracting(AdapterReference::displayName)
                .containsExactly("cc109 (OPay)");
        assertThat(signals.affectedOrderTypes()).contains("PAYMENT");
        assertThat(signals.httpStatuses()).contains(503);
        assertThat(signals.signalTypes()).contains("provider_degradation");
        assertThat(signals.requestIds()).contains("9f3a2b7c-1d4e-4f8a-b2c1-3e5f7a9b1d3e");
        assertThat(signals.firstTimestamp()).isEqualTo("2024-11-15 14:23:01.445");
        assertThat(signals.lastTimestamp()).isEqualTo("2024-11-15 14:23:05.115");
    }

    @Test
    void extractsPoolExhaustionSignals() {
        LogSignals signals = extractor.extract(IncidentFixture.load("inc-205-payme-pool-exhaustion.log"));

        assertThat(signals.affectedAdapters()).extracting(AdapterReference::displayName)
                .containsExactly("cc087 (PayMe)");
        assertThat(signals.affectedOrderTypes()).contains("PAYMENT");
        assertThat(signals.signalTypes()).contains("connection_pool_exhaustion", "timeout");
    }

    @Test
    void extractsSignatureAndCredentialSignals() {
        LogSignals signals = extractor.extract(IncidentFixture.load("inc-202-payme-signature.log"));

        assertThat(signals.incidentId()).isEqualTo("INC-202");
        assertThat(signals.affectedAdapters()).extracting(AdapterReference::displayName)
                .containsExactly("cc087 (PayMe)");
        assertThat(signals.httpStatuses()).contains(401);
        assertThat(signals.signalTypes()).contains("signature_or_credential_issue");
    }

    @Test
    void extractsQueueAndDatabaseSignals() {
        LogSignals signals = extractor.extract(IncidentFixture.load("inc-203-halopesa-rabbitmq-backlog.log"));

        assertThat(signals.incidentId()).isEqualTo("INC-203");
        assertThat(signals.affectedAdapters()).extracting(AdapterReference::displayName)
                .containsExactly("cc139 (Halopesa)");
        assertThat(signals.affectedOrderTypes()).contains("PAYOUT", "CALLBACK");
        assertThat(signals.signalTypes()).contains("queue_backlog", "database_failure", "timeout");
    }

    @Test
    void extractsMultiAdapterTerminalRoutingSignals() {
        LogSignals signals = extractor.extract(IncidentFixture.load("inc-204-terminal-link-routing.log"));

        assertThat(signals.incidentId()).isEqualTo("INC-204");
        assertThat(signals.affectedAdapters()).extracting(AdapterReference::displayName)
                .containsExactly("cc109 (OPay)", "cc131 (Cobre)");
        assertThat(signals.signalTypes()).contains("terminal_routing_or_cache");
    }
}
