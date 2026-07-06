# AI Adapter Log Analyzer

Compact Java 21 Spring Boot service for analyzing payment-adapter incident logs. It implements the take-home assignment from `task_APM_log_analyzer.pdf`: classify the incident, summarize affected scope and severity, propose root-cause hypotheses, suggest diagnostic steps, and return a machine-readable JSON response.

The implementation is intentionally small: one REST endpoint, deterministic signal extraction, static architecture context, Spring AI diagnosis, validation, and one repair pass for bad model output.

## Run

Start with the deterministic fallback client:

```bash
./gradlew bootRun
```

Start with the live Spring AI client:

```bash
export OPENAI_API_KEY="..."
export OPENAI_CHAT_MODEL="gpt-4.1-mini"
./gradlew bootRun
```

The API listens on `http://localhost:8080` by default. Override with:

```bash
export SERVER_PORT=8081
```

Health check:

```bash
curl -fsS http://localhost:8080/actuator/health
```

## Configuration

Environment variables:

- `OPENAI_API_KEY`: OpenAI API key. If absent, `SpringAiDiagnosisClient` is not created and the app uses `RuleBasedFallbackDiagnosisClient`.
- `OPENAI_CHAT_MODEL`: chat model, default `gpt-4.1-mini`.
- `SERVER_PORT`: HTTP port, default `8080`.

Do not commit secrets.

## API

Endpoint:

```text
POST /api/incidents/analyze
Content-Type: application/json
```

Request:

```json
{
  "logs": "[INC-201] Mass payment failures on adapter cc109 (OPay)\n2024-11-15 14:23:01.445 ERROR c.c.m.client.opay.ClientService - POST https://api.opay.ng/v3/payments/create failed: status=503, body={\"message\":\"Service temporarily unavailable\",\"code\":\"MAINTENANCE\"}"
}
```

Example curl:

```bash
curl -s http://localhost:8080/api/incidents/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "logs": "[INC-201] Mass payment failures on adapter cc109 (OPay)\n2024-11-15 14:23:01.445 ERROR c.c.m.client.opay.ClientService - POST https://api.opay.ng/v3/payments/create failed: status=503, body={\"message\":\"Service temporarily unavailable\",\"code\":\"MAINTENANCE\"}"
  }'
```

Response shape:

```json
{
  "incident_id": "INC-201",
  "category": "External provider degradation",
  "summary": {
    "description": "string",
    "affected_adapters": ["cc109 (OPay)"],
    "affected_order_types": ["PAYMENT"],
    "fault_layer": "External",
    "severity": "high",
    "severity_reasoning": "string",
    "blast_radius": "single_adapter"
  },
  "hypotheses": [
    {
      "title": "string",
      "reasoning": "string",
      "probability": "likely",
      "next_steps": [
        {
          "action": "string",
          "tool": "ELK",
          "detail": "string"
        }
      ]
    }
  ],
  "immediate_actions": [
    {
      "action": "string",
      "risk": "caution",
      "reasoning": "string"
    }
  ]
}
```

## Agent Pipeline

`IncidentAnalysisController` accepts raw log text and delegates to `DiagnosisAgent`.

`DiagnosisAgent` runs explicit stages:

1. `LogSignalExtractor` parses deterministic signals: incident id, timestamps, adapters, order types, HTTP statuses, request ids, provider/queue/cache/db/timeout/pool indicators.
2. `ArchitectureContext` enriches the prompt with known layers and systems.
3. `SpringAiDiagnosisClient` calls Spring AI with prompts from `src/main/resources/prompts` when an OpenAI key is configured.
4. `RuleBasedFallbackDiagnosisClient` provides deterministic local behavior when no key is configured.
5. `DiagnosisValidator` parses and validates the JSON.
6. If validation fails, the agent asks the LLM for one repaired response and validates again.

Known adapters:

- `cc109 (OPay)`
- `cc087 (PayMe)`
- `cc139 (Halopesa)`
- `cc131 (Cobre)`

Allowed diagnostic tools:

- `ELK`
- `Grafana`
- `Consul`
- `Vault`
- `Redis`
- `RabbitMQ`
- `PostgreSQL`

No real ELK, Grafana, Consul, Vault, Redis, RabbitMQ, or PostgreSQL calls are made. They are diagnostic references only.

## Validation Rules

The service rejects or repairs common model errors required by the PDF:

- invalid JSON
- missing required fields
- invalid enum values for `fault_layer`, `severity`, `blast_radius`, `probability`, and action `risk`
- hallucinated adapters not supported by the log evidence
- diagnostic tools outside the architecture context
- contradictory layer classification, for example provider degradation classified as API
- order types not supported by extracted log evidence
- more than 3 hypotheses or more than 2 immediate actions

## Tests

Run all tests:

```bash
./gradlew test
```

Build the executable jar:

```bash
./gradlew bootJar
```

Test fixtures:

```text
src/test/resources/incidents/inc-201-opay-provider-503.log
src/test/resources/incidents/inc-202-payme-signature.log
src/test/resources/incidents/inc-203-halopesa-rabbitmq-backlog.log
src/test/resources/incidents/inc-204-terminal-link-routing.log
src/test/resources/incidents/inc-205-payme-pool-exhaustion.log
```

Automated coverage includes:

- controller response shape and blank-log validation
- controlled `422` response for unrepaired model validation failure
- deterministic extraction and fallback diagnosis for all 5 PDF incidents
- repair flow after invalid model output
- validator rejection of invalid JSON, missing fields, bad enum values, hallucinated adapters, unsupported tools, contradictory hypotheses, and unsupported order types

## Manual API Verification

I also ran the live API against five different logs from the PDF with `OPENAI_API_KEY` configured, confirming from application logs that `SpringAiDiagnosisClient` handled the requests.

For `INC-201`, the service classified the OPay 503 maintenance incident as provider degradation in the `External` layer with high severity. It returned 3 hypotheses, 2-3 next steps per hypothesis, and 2 immediate actions.

For `INC-202`, the service classified the PayMe signature mismatch as a signature or credential issue in the `API` layer with high severity. It returned 3 hypotheses, 2-3 next steps per hypothesis, and 2 immediate actions.

For `INC-203`, the service classified the Halopesa RabbitMQ and DB stall as an infrastructure-level adapter failure with high severity. It returned 3 hypotheses, 2-3 next steps per hypothesis, and 2 immediate actions.

For `INC-204`, the service classified the terminal routing/cache issue as an infrastructure issue with multi-adapter blast radius and medium severity. It returned 3 hypotheses, 2-3 next steps per hypothesis, and 2 immediate actions.

For `INC-205`, the service classified the PayMe connection pool exhaustion as an SDK-layer resource exhaustion incident with high severity. It returned 3 hypotheses, 3 next steps per hypothesis, and 2 immediate actions.

During live verification, the model initially returned unsupported tools (`none`/`None`) for `INC-201` and `INC-205`; the repair stage re-prompted through `SpringAiDiagnosisClient`, and the repaired JSON passed validation.

## Trade-offs

- The architecture context is static and small, matching the assignment scope.
- There are no integrations with real observability or infrastructure systems.
- No persistence, authentication, async processing, or UI is included.
- The fallback client is intentionally simple and deterministic; richer diagnosis comes from the live Spring AI client.
- Tests mock or avoid live LLM calls for repeatability. Live API verification is documented separately above.
