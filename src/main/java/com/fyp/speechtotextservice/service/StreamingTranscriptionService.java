package com.fyp.speechtotextservice.service;

import com.fyp.speechtotextservice.config.AssemblyAIConfig;
import com.fyp.speechtotextservice.dto.StreamingTranscriptionRequest;
import com.fyp.speechtotextservice.dto.StreamingTranscriptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class StreamingTranscriptionService {

    private final AssemblyAIConfig config;
    private final HttpClient client;
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();

    @Autowired
    public StreamingTranscriptionService(AssemblyAIConfig config) {
        this.config = config;
        this.client = HttpClient.newHttpClient();
    }

    @PreDestroy
    public void cleanup() {
        // Close all active WebSocket connections
        activeSessions.values().forEach(session -> {
            if (session.webSocket != null) {
                session.webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Service shutdown");
            }
        });
        activeSessions.clear();
    }

    public String startSession() throws URISyntaxException {
        String sessionId = UUID.randomUUID().toString();
        startWebSocketConnection(sessionId, null);
        return sessionId;
    }

    public void processAudioChunk(StreamingTranscriptionRequest request, Consumer<StreamingTranscriptionResponse> callback) {
        String sessionId = request.getSessionId();
        if (!activeSessions.containsKey(sessionId)) {
            try {
                startWebSocketConnection(sessionId, callback);
            } catch (URISyntaxException e) {
                log.error("Failed to start WebSocket connection", e);
                StreamingTranscriptionResponse response = new StreamingTranscriptionResponse();
                response.setSessionId(sessionId);
                response.setMessageType("Error");
                response.setError("Failed to establish WebSocket connection: " + e.getMessage());
                callback.accept(response);
                return;
            }
        }

        SessionData sessionData = activeSessions.get(sessionId);
        sessionData.callback = callback;

        try {
            // Decode Base64 audio chunk
            byte[] audioBytes = Base64.getDecoder().decode(request.getAudioChunk());
            
            // Send audio bytes to AssemblyAI
            sessionData.webSocket.sendBinary(ByteBuffer.wrap(audioBytes), true);
        } catch (Exception e) {
            log.error("Error processing audio chunk", e);
            StreamingTranscriptionResponse response = new StreamingTranscriptionResponse();
            response.setSessionId(sessionId);
            response.setMessageType("Error");
            response.setError("Error processing audio chunk: " + e.getMessage());
            callback.accept(response);
        }
    }

    public void endSession(String sessionId) {
        SessionData sessionData = activeSessions.get(sessionId);
        if (sessionData != null && sessionData.webSocket != null) {
            sessionData.webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Session ended");
            activeSessions.remove(sessionId);
            log.info("Session ended: {}", sessionId);
        }
    }

    private void startWebSocketConnection(String sessionId, Consumer<StreamingTranscriptionResponse> callback) throws URISyntaxException {
        // AssemblyAI Real-time WebSocket endpoint
        URI uri = new URI(config.getAssemblyAILiveUrl());
        
        WebSocket.Listener listener = new AssemblyAIWebSocketListener(sessionId, callback);
        
        CompletionStage<WebSocket> webSocketCompletionStage = client.newWebSocketBuilder()
                .header("Authorization", config.getApiKey())
                .buildAsync(uri, listener);
        
        webSocketCompletionStage.thenAccept(webSocket -> {
            // Store session data
            SessionData sessionData = new SessionData(webSocket, callback);
            activeSessions.put(sessionId, sessionData);
            log.info("WebSocket connection established for session: {}", sessionId);
            
            // Send configuration message
            String configMessage = String.format("{\"sample_rate\": 16000, \"session_id\": \"%s\"}", sessionId);
            webSocket.sendText(configMessage, true);
        }).exceptionally(e -> {
            log.error("Failed to establish WebSocket connection", e);
            return null;
        });
    }

    private static class SessionData {
        private final WebSocket webSocket;
        private Consumer<StreamingTranscriptionResponse> callback;

        public SessionData(WebSocket webSocket, Consumer<StreamingTranscriptionResponse> callback) {
            this.webSocket = webSocket;
            this.callback = callback;
        }
    }

    private class AssemblyAIWebSocketListener implements WebSocket.Listener {
        private final StringBuilder messageBuffer = new StringBuilder();
        private final String sessionId;
        private final Consumer<StreamingTranscriptionResponse> callback;

        public AssemblyAIWebSocketListener(String sessionId, Consumer<StreamingTranscriptionResponse> callback) {
            this.sessionId = sessionId;
            this.callback = callback;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("WebSocket connection opened for session: {}", sessionId);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);

            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);

                try {
                    // Process the message from AssemblyAI
                    log.debug("Received message: {}", message);
                    
                    // Simple message parsing - could use ObjectMapper for production
                    StreamingTranscriptionResponse response = parseAssemblyAIMessage(message);
                    response.setSessionId(sessionId);
                    
                    // Send response to callback if available
                    if (callback != null) {
                        callback.accept(response);
                    }
                } catch (Exception e) {
                    log.error("Error processing message: {}", e.getMessage());
                }
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("WebSocket connection closed for session {}: {} - {}", sessionId, statusCode, reason);
            activeSessions.remove(sessionId);
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("WebSocket error for session {}: {}", sessionId, error.getMessage());
            activeSessions.remove(sessionId);
            
            if (callback != null) {
                StreamingTranscriptionResponse response = new StreamingTranscriptionResponse();
                response.setSessionId(sessionId);
                response.setMessageType("Error");
                response.setError("WebSocket error: " + error.getMessage());
                callback.accept(response);
            }
            
            WebSocket.Listener.super.onError(webSocket, error);
        }

        private StreamingTranscriptionResponse parseAssemblyAIMessage(String message) {
            StreamingTranscriptionResponse response = new StreamingTranscriptionResponse();
            
            // Simple JSON parsing - should use a JSON library in production
            if (message.contains("\"message_type\":\"PartialTranscript\"")) {
                response.setMessageType("PartialTranscript");
                response.setFinal(false);
                
                // Extract text using simple string operations (use proper JSON parsing in production)
                int textStartIndex = message.indexOf("\"text\":") + 8;
                int textEndIndex = message.indexOf("\"", textStartIndex);
                if (textStartIndex > 8 && textEndIndex > textStartIndex) {
                    response.setText(message.substring(textStartIndex, textEndIndex));
                }
            } else if (message.contains("\"message_type\":\"FinalTranscript\"")) {
                response.setMessageType("FinalTranscript");
                response.setFinal(true);
                
                // Extract text
                int textStartIndex = message.indexOf("\"text\":") + 8;
                int textEndIndex = message.indexOf("\"", textStartIndex);
                if (textStartIndex > 8 && textEndIndex > textStartIndex) {
                    response.setText(message.substring(textStartIndex, textEndIndex));
                }
            } else if (message.contains("\"message_type\":\"Error\"")) {
                response.setMessageType("Error");
                
                // Extract error
                int errorStartIndex = message.indexOf("\"error\":") + 9;
                int errorEndIndex = message.indexOf("\"", errorStartIndex);
                if (errorStartIndex > 9 && errorEndIndex > errorStartIndex) {
                    response.setError(message.substring(errorStartIndex, errorEndIndex));
                }
            }
            
            return response;
        }
    }
} 