package com.fyp.speechtotextservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class AssemblyAIWebSocketClient implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AssemblyAIWebSocketClient.class);
    private WebSocketSession session;
    private final BlockingQueue<String> transcriptionQueue = new LinkedBlockingQueue<>();

    public void connect(String token, int sampleRate) throws Exception {
        String wsUrl = String.format("wss://api.assemblyai.com/v2/realtime/ws?sample_rate=%d&token=%s", sampleRate, token);
        StandardWebSocketClient client = new StandardWebSocketClient();
        this.session = client.execute(this, String.valueOf(new URI(wsUrl))).get();
        log.info("Connected to AssemblyAI WebSocket");
    }

    public void sendAudio(byte[] audioData) {
        if (session != null && session.isOpen()) {
            String audioMessage = "{\"audio_data\": \"" + java.util.Base64.getEncoder().encodeToString(audioData) + "\"}";
            try {
                session.sendMessage(new TextMessage(audioMessage));
            } catch (Exception e) {
                log.error("Error sending audio data: {}", e.getMessage());
            }
        }
    }

    public void close() throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage("{\"terminate_session\": true}"));
            session.close();
            log.info("Closed AssemblyAI WebSocket connection");
        }
    }

    public BlockingQueue<String> getTranscriptionQueue() {
        return transcriptionQueue;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        log.info("WebSocket connection established with AssemblyAI");
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        log.info("Received message: {}", payload);

        if (payload.contains("text")) {
            String text = payload.split("\"text\":\"")[1].split("\"")[0];
            if (!text.isEmpty()) {
                transcriptionQueue.offer(text);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", exception.getMessage());
        transcriptionQueue.offer("Error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WebSocket connection closed: {}", closeStatus);
        this.session = null;
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}