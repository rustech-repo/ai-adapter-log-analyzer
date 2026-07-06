Repair the invalid incident diagnosis output.

Return only corrected valid JSON in the required schema. Do not include markdown or explanation.

Validation error:
%s

Architecture context:
%s

Extracted deterministic signals:
%s

Invalid output:
%s

Repair rules:
- Preserve supported conclusions when they match the log evidence.
- Remove or replace hallucinated adapters, services, providers, and tools.
- Use only allowed fault layers: SDK, Core, API, Infrastructure, External.
- Use only allowed severity values: low, medium, high, critical.
- Use only allowed blast radius values: single_merchant, single_adapter, multi_adapter, platform_wide.
- Use only allowed probability values: likely, possible, unlikely.
- Use only allowed action risk values: safe, caution, risky.
- Ensure every hypothesis has valid next_steps.
- Ensure next_steps reference only allowed diagnostic tools from the architecture context.
- Keep at most 3 hypotheses and at most 2 immediate actions.
