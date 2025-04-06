package com.fyp.speechtotextservice.websocket;

import com.assemblyai.api.RealtimeTranscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    @Value("${assemblyai.api-key}")
    private String apiKey;

    private RealtimeTranscriber transcriber;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        transcriber = RealtimeTranscriber.builder()
                .apiKey(apiKey)
                .onPartialTranscript(transcript -> {
                    try {
                        session.sendMessage(new BinaryMessage(transcript.getText().getBytes()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .onFinalTranscript(transcript -> {
                    try {
                        session.sendMessage(new BinaryMessage(transcript.getText().getBytes()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .onError(error -> System.err.println("Error: " + error.getMessage()))
                .build();

        transcriber.connect();
        System.out.println("WebSocket connection established");
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] audioData = message.getPayload().array();
        transcriber.sendAudio(audioData);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (transcriber != null) {
            transcriber.close();
        }
        System.out.println("WebSocket connection closed");
    }
}