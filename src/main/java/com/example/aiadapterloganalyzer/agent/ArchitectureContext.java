package com.example.aiadapterloganalyzer.agent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ArchitectureContext {

    private static final List<AdapterReference> KNOWN_ADAPTERS = List.of(
            new AdapterReference("cc109", "OPay"),
            new AdapterReference("cc087", "PayMe"),
            new AdapterReference("cc139", "Halopesa"),
            new AdapterReference("cc131", "Cobre")
    );

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "ELK",
            "Grafana",
            "Consul",
            "Vault",
            "Redis",
            "RabbitMQ",
            "PostgreSQL"
    );

    public List<AdapterReference> knownAdapters() {
        return KNOWN_ADAPTERS;
    }

    public Set<String> allowedTools() {
        return ALLOWED_TOOLS;
    }

    public Optional<AdapterReference> findByCodeOrProvider(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return KNOWN_ADAPTERS.stream()
                .filter(adapter -> adapter.code().equalsIgnoreCase(value)
                        || adapter.provider().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public String promptContext() {
        return """
                Architecture layers:
                - API: REST controllers, RabbitMQ consumers, requests from MI.
                - Core: status mapping, decline mapping, timeout handling, retry decisions.
                - SDK: HTTP/SOAP clients, provider DTOs, RestClientProvider metrics.
                - Infrastructure: Consul, Vault, Redis, RabbitMQ, PostgreSQL, ELK, Grafana.
                - External: provider systems and APIs.

                Allowed diagnostic tools: ELK, Grafana, Consul, Vault, Redis, RabbitMQ, PostgreSQL.
                Known adapters: cc109 (OPay), cc087 (PayMe), cc139 (Halopesa), cc131 (Cobre).
                """;
    }
}
