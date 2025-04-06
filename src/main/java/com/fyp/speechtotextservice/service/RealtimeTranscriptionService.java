package com.fyp.speechtotextservice.service;

import com.assemblyai.api.RealtimeTranscriber;
import com.fyp.speechtotextservice.config.AssemblyAIConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RealtimeTranscriptionService {

    @Autowired
    private AssemblyAIConfig config;

    private RealtimeTranscriber transcriber;

    @PostConstruct
    public void init() {
        // Initialize the RealtimeTranscriber
        transcriber = RealtimeTranscriber.builder()
                .apiKey(config.getApiKey())
                .onSessionStart(session -> log.info("Session started: {}", session))
                .onPartialTranscript(transcript -> log.info("Partial: {}", transcript))
                .onFinalTranscript(transcript -> log.info("Final: {}", transcript))
                .onError(error -> log.error("Error: {}", error.getMessage()))
                .build();

        // Connect to AssemblyAI's real-time transcription service
        transcriber.connect();
    }

    public void sendAudio(byte[] audioData) {
        // Send audio data to the transcriber
        transcriber.sendAudio(audioData);
    }

    @PreDestroy
    public void cleanup() {
        // Close the connection when the service is destroyed
        if (transcriber != null) {
            transcriber.close();
        }
    }
}