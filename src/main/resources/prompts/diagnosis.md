Analyze the incident logs and produce a structured diagnosis JSON.

Use this pipeline:
1. Read the raw log evidence.
2. Cross-check it against the deterministic signals.
3. Enrich the diagnosis with the architecture context.
4. Classify the most likely failure category.
5. Produce only the required JSON object.

Architecture context:
%s

Extracted deterministic signals:
%s

Raw logs:
%s

Classification guidance:
- Provider 5xx, provider maintenance, repeated provider timeouts: External provider degradation, usually External or SDK.
- X-Sign mismatch, 401 after upgrade, Vault secret rotation mismatch: Internal configuration / credential issue, usually API or Core.
- RabbitMQ backlog, DLQ growth, database timeout, Redis cache issue: Infrastructure failure (DB/cache/queue), Infrastructure.
- Active connections maxed, pending requests, pool acquisition timeout: Connection pool / resource exhaustion, SDK or Infrastructure.
- Terminal link not found, stale terminal-links cache, Terminals Service 404: Routing / terminal configuration issue, Infrastructure or Core.

Diagnostic step rules:
- Each hypothesis must have 2-3 next steps when possible.
- Tools must be one of the allowed architecture tools only.
- Prefer concrete checks such as ELK queries, Grafana dashboards, Consul health checks, Vault secret inspection, Redis key lookup, RabbitMQ queue inspection, or PostgreSQL connection checks.

Immediate action rules:
- Include at most 2 actions.
- Each action must have risk safe, caution, or risky.
- Prefer mitigations an on-call engineer can take immediately.
