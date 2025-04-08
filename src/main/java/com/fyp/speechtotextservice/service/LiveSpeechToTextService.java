package com.fyp.speechtotextservice.service;

import com.assemblyai.api.RealtimeTranscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.function.Consumer;

@Service
public class LiveSpeechToTextService {

    @Value("${assemblyai.api.key}")
    private String apiKey;

    private RealtimeTranscriber realtimeTranscriber;

    public void initWithCallback(Consumer<String> transcriptCallback) {
        System.out.println("Initializing AssemblyAI with API key: " + apiKey.substring(0, 4) + "...");
        realtimeTranscriber = RealtimeTranscriber.builder()
                .apiKey(apiKey)
                .onSessionStart(session -> System.out.println("Session started: " + session))
                .onPartialTranscript(partial -> {
                    String text = partial.getText();
                    System.out.println("Partial transcript: '" + text + "'");
                    transcriptCallback.accept("PARTIAL: " + text);
                })
                .onFinalTranscript(finalTranscript -> {
                    String text = finalTranscript.getText();
                    System.out.println("Final transcript: '" + text + "'");
                    transcriptCallback.accept("FINAL: " + text);
                })
                .onError(error -> System.err.println("AssemblyAI Error: " + error.getMessage()))
                .build();
        System.out.println("Connecting to AssemblyAI...");
        realtimeTranscriber.connect();
    }

    public void sendAudio(byte[] audioData) {
        if (realtimeTranscriber != null) {
            //System.out.println("Sending audio chunk of size: " + audioData.length);
            realtimeTranscriber.sendAudio(audioData);
        } else {
            System.err.println("RealtimeTranscriber is null");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (realtimeTranscriber != null) {
            System.out.println("Closing RealtimeTranscriber...");
            realtimeTranscriber.close();
        }
    }
}
