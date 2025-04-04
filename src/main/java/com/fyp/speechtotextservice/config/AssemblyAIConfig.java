package com.fyp.speechtotextservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class AssemblyAIConfig {
    
    @Value("${assemblyai.api.key}")
    private String apiKey;
    
    public String getApiKey() {
        return apiKey;
    }
} 