package com.example.aiadapterloganalyzer.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Primary
@ConditionalOnExpression("'${spring.ai.openai.api-key:}'.length() > 0")
class SpringAiDiagnosisClient implements LlmDiagnosisClient {

    private final ChatClient chatClient;
    private final PromptResources promptResources;

    SpringAiDiagnosisClient(
            ChatClient chatClient,
            PromptResources promptResources
    ) {
        this.chatClient = chatClient;
        this.promptResources = promptResources;
    }

    @Override
    public String generateDiagnosis(DiagnosisPromptRequest request) {
        return requestContent(
                "diagnosis",
                request.signals().incidentId(),
                promptResources.diagnosisPrompt(request)
        );
    }

    @Override
    public String repairDiagnosis(RepairPromptRequest request) {
        return requestContent(
                "repair",
                request.signals().incidentId(),
                promptResources.repairPrompt(request)
        );
    }

    private String requestContent(String stage, String incidentId, String userPrompt) {
        log.info("Requesting LLM {} for incident id={}, promptLength={}", stage, incidentId, userPrompt.length());
        String content = chatClient.prompt()
                .system(promptResources.systemPrompt())
                .user(userPrompt)
                .call()
                .content();

        if (!StringUtils.hasText(content)) {
            log.warn("LLM {} returned empty content for incident id={}", stage, incidentId);
            throw new DiagnosisValidationException("LLM " + stage + " returned empty content");
        }

        log.info("LLM {} returned content for incident id={}, contentLength={}",
                stage, incidentId, content.length());
        return content;
    }
}
