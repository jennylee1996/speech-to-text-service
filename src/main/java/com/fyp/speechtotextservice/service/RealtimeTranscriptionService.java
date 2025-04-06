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
    private String latestTranscript = ""; // Store the latest transcribed text

    @PostConstruct
    public void init() {
        transcriber = RealtimeTranscriber.builder()
                .apiKey(config.getApiKey())
                .onSessionStart(session -> {
                    log.info("Session started: {}", session);
                    System.out.println("Session started: " + session);
                })
                .onPartialTranscript(transcript -> {
                    String text = transcript.getText(); // Extract the transcribed text
                    log.info("Partial transcript text: {}", text);
                    System.out.println("Partial transcript text: " + text);
                    latestTranscript = text != null ? text : ""; // Update with the text
                })
                .onFinalTranscript(transcript -> {
                    String text = transcript.getText(); // Extract the transcribed text
                    log.info("Final transcript text: {}", text);
                    System.out.println("Final transcript text: " + text);
                    latestTranscript = text != null ? text : ""; // Update with the text
                })
                .onError(error -> {
                    log.error("Error: {}", error.getMessage());
                    System.out.println("Error: " + error.getMessage());
                })
                .build();
        transcriber.connect();
    }

    public void sendAudio(byte[] audioData) {
        transcriber.sendAudio(audioData);
    }

    public String getLatestTranscript() {
        return latestTranscript;
    }

    @PreDestroy
    public void cleanup() {
        if (transcriber != null) {
            transcriber.close();
        }
    }
}