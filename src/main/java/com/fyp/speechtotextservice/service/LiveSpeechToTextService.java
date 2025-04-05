package com.fyp.speechtotextservice.service;

import com.fyp.speechtotextservice.config.AssemblyAIWebSocketClient;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;

@Service
@AllArgsConstructor
public class LiveSpeechToTextService {

    private final TokenService tokenService;
    private final AssemblyAIWebSocketClient webSocketClient;
    private final int SAMPLE_RATE = 16000;


    public void startTranscriptionSession() throws Exception {
        String token = tokenService.generateTemporaryToken().block();
        if (token == null) {
            throw new RuntimeException("Failed to generate temporary token");
        }
        webSocketClient.connect(token, SAMPLE_RATE);
    }

    public void sendAudio(byte[] audioData) {
        webSocketClient.sendAudio(audioData);
    }

    public BlockingQueue<String> getTranscriptionQueue() {
        return webSocketClient.getTranscriptionQueue();
    }

    @PreDestroy
    public void close() throws Exception {
        webSocketClient.close();
    }
}