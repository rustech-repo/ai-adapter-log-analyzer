package com.example.aiadapterloganalyzer.agent;

import java.util.List;
import java.util.Set;

public record LogSignals(
        String incidentId,
        List<AdapterReference> affectedAdapters,
        Set<String> affectedOrderTypes,
        Set<Integer> httpStatuses,
        Set<String> signalTypes,
        Set<String> requestIds,
        String firstTimestamp,
        String lastTimestamp
) {
}
