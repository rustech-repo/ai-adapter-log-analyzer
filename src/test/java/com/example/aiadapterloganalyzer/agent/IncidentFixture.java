package com.example.aiadapterloganalyzer.agent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class IncidentFixture {

    private IncidentFixture() {
    }

    static String load(String filename) {
        try (var input = IncidentFixture.class.getResourceAsStream("/incidents/" + filename)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing incident fixture: " + filename);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read incident fixture: " + filename, e);
        }
    }
}
