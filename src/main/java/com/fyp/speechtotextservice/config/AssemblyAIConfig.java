package com.fyp.speechtotextservice.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource("classpath:application.properties")
public class AssemblyAIConfig {
    
    @Value("${assemblyai.api.key}")
    private String apiKey;

    @Value("${assemblyai.api.url}")
    private String assemblyAIBaseUrl;

    @Value("${assemblyai.api.liveUrl}")
    private String assemblyAILiveUrl;
} 