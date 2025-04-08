package com.fyp.speechtotextservice.websocket;

import com.fyp.speechtotextservice.service.LiveSpeechToTextService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final LiveSpeechToTextService liveSpeechToTextService;

    private WebSocketSession currentSession; // Store the session to send transcripts back

    public AudioWebSocketHandler(LiveSpeechToTextService liveSpeechToTextService) {
        this.liveSpeechToTextService = liveSpeechToTextService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        currentSession = session;

        liveSpeechToTextService.initWithCallback(transcript -> {
            try {
                if (currentSession != null && currentSession.isOpen()) {
                    System.out.println("Sending transcript: " + transcript);
                    currentSession.sendMessage(new TextMessage(transcript));
                } else {
                    System.out.println("Session closed or null, cannot send: " + transcript);
                }
            } catch (Exception e) {
                System.err.println("Error sending transcript: " + e.getMessage());
            }
        });
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] pcmAudio = message.getPayload().array();
//        System.out.println("Received PCM audio chunk of size: " + pcmAudio.length);
        liveSpeechToTextService.sendAudio(pcmAudio);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket connection closed: " + session.getId());
        currentSession = null;
    }
}
