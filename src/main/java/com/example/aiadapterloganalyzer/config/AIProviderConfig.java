package com.example.aiadapterloganalyzer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AIProviderConfig {

    @Bean
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel, SimpleLoggerAdvisor simpleLoggerAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(simpleLoggerAdvisor)
                .build();
    }

    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

}
