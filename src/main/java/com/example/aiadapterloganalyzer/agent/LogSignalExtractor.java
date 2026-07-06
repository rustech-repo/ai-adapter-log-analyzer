package com.example.aiadapterloganalyzer.agent;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LogSignalExtractor {

    private static final Pattern INCIDENT_ID = Pattern.compile("\\[(INC-\\d+)]");
    private static final Pattern HTTP_STATUS = Pattern.compile("\\bstatus=(\\d{3})\\b|\\breturned (\\d{3})\\b|\\bReturning (\\d{3})\\b");
    private static final Pattern REQUEST_ID = Pattern.compile("X-Request-Id:\\s*([a-fA-F0-9-]+)");
    private static final Pattern TIMESTAMP = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})", Pattern.MULTILINE);

    private final ArchitectureContext architectureContext;

    LogSignalExtractor(ArchitectureContext architectureContext) {
        this.architectureContext = architectureContext;
    }

    public LogSignals extract(String logs) {
        Set<AdapterReference> adapters = extractAdapters(logs);
        Set<String> orderTypes = extractOrderTypes(logs);
        Set<Integer> statuses = extractHttpStatuses(logs);
        Set<String> requestIds = extractRequestIds(logs);
        Set<String> signalTypes = extractSignalTypes(logs);
        String[] timestamps = extractTimestampRange(logs);

        return new LogSignals(
                firstMatch(INCIDENT_ID, logs),
                adapters.stream().toList(),
                orderTypes,
                statuses,
                signalTypes,
                requestIds,
                timestamps[0],
                timestamps[1]
        );
    }

    private Set<AdapterReference> extractAdapters(String logs) {
        String normalized = logs.toLowerCase(Locale.ROOT);
        Set<AdapterReference> adapters = new LinkedHashSet<>();
        for (AdapterReference adapter : architectureContext.knownAdapters()) {
            if (normalized.contains(adapter.code().toLowerCase(Locale.ROOT))
                    || containsWord(normalized, adapter.provider())) {
                adapters.add(adapter);
            }
        }
        return adapters;
    }

    private boolean containsWord(String normalizedLogs, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word.toLowerCase(Locale.ROOT)) + "\\b")
                .matcher(normalizedLogs)
                .find();
    }

    private Set<String> extractOrderTypes(String logs) {
        String normalized = logs.toLowerCase(Locale.ROOT);
        Set<String> orderTypes = new LinkedHashSet<>();
        if (normalized.contains("payout")) {
            orderTypes.add("PAYOUT");
        }
        if (normalized.contains("payment") || normalized.contains("/payments") || normalized.contains("/charge")) {
            orderTypes.add("PAYMENT");
        }
        if (normalized.contains("callback")) {
            orderTypes.add("CALLBACK");
        }
        return orderTypes;
    }

    private Set<Integer> extractHttpStatuses(String logs) {
        Matcher matcher = HTTP_STATUS.matcher(logs);
        Set<Integer> statuses = new LinkedHashSet<>();
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    statuses.add(Integer.parseInt(matcher.group(i)));
                }
            }
        }
        return statuses;
    }

    private Set<String> extractRequestIds(String logs) {
        Matcher matcher = REQUEST_ID.matcher(logs);
        Set<String> requestIds = new LinkedHashSet<>();
        while (matcher.find()) {
            requestIds.add(matcher.group(1));
        }
        return requestIds;
    }

    private Set<String> extractSignalTypes(String logs) {
        String normalized = logs.toLowerCase(Locale.ROOT);
        Set<String> signals = new LinkedHashSet<>();
        if (normalized.contains("503") || normalized.contains("maintenance") || normalized.contains("provider returned")) {
            signals.add("provider_degradation");
        }
        if (normalized.contains("signature") || normalized.contains("x-sign") || normalized.contains("401")) {
            signals.add("signature_or_credential_issue");
        }
        if (containsQueueEvidence(normalized)) {
            signals.add("queue_backlog");
        }
        if (normalized.contains("redis") || normalized.contains("terminal link not found") || normalized.contains("terminal-links")) {
            signals.add("terminal_routing_or_cache");
        }
        if (normalized.contains("connection from pool") || normalized.contains("active connections") || normalized.contains("pending requests")) {
            signals.add("connection_pool_exhaustion");
        }
        if (normalized.contains("postgres") || normalized.contains("mongodb") || normalized.contains("database")) {
            signals.add("database_failure");
        }
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            signals.add("timeout");
        }
        return signals;
    }

    private boolean containsQueueEvidence(String normalizedLogs) {
        return normalizedLogs.contains("rabbit")
                || normalizedLogs.contains("dlq")
                || normalizedLogs.contains("queue depth")
                || normalizedLogs.contains("callback queue")
                || normalizedLogs.contains("pending messages");
    }

    private String[] extractTimestampRange(String logs) {
        Matcher matcher = TIMESTAMP.matcher(logs);
        String first = null;
        String last = null;
        while (matcher.find()) {
            if (first == null) {
                first = matcher.group(1);
            }
            last = matcher.group(1);
        }
        return new String[]{first, last};
    }

    private String firstMatch(Pattern pattern, String logs) {
        Matcher matcher = pattern.matcher(logs);
        return matcher.find() ? matcher.group(1) : null;
    }
}
