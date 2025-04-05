package com.fyp.speechtotextservice.controller;

import com.fyp.speechtotextservice.dto.StreamingTranscriptionRequest;
import com.fyp.speechtotextservice.dto.StreamingTranscriptionResponse;
import com.fyp.speechtotextservice.service.StreamingTranscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@RestController
@RequestMapping("/api/streaming")
public class StreamingTranscriptionController {

    private final StreamingTranscriptionService streamingService;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Autowired
    public StreamingTranscriptionController(StreamingTranscriptionService streamingService) {
        this.streamingService = streamingService;
    }

    /**
     * Start a new streaming session
     * @return session ID for further communication
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startSession() {
        try {
            String sessionId = streamingService.startSession();
            return ResponseEntity.ok(Map.of("sessionId", sessionId));
        } catch (URISyntaxException e) {
            log.error("Failed to start streaming session", e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to start streaming session: " + e.getMessage())
            );
        }
    }

    /**
     * Process audio chunk for transcription
     * @param request The streaming transcription request with audio chunk
     * @return Transcription response
     */
    @PostMapping("/transcribe")
    public ResponseEntity<?> transcribeAudioChunk(@RequestBody StreamingTranscriptionRequest request) {
        if (request.getSessionId() == null || request.getAudioChunk() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Session ID and audio chunk are required")
            );
        }

        // Process audio chunk and return immediately (non-blocking)
        streamingService.processAudioChunk(request, response -> {
            // We don't need to do anything here as we'll use SSE for real-time updates
            log.debug("Processed chunk for session: {}", request.getSessionId());
        });

        return ResponseEntity.accepted().build();
    }

    /**
     * End a streaming session
     * @param sessionId Session ID to end
     * @return Status response
     */
    @PostMapping("/end")
    public ResponseEntity<Map<String, String>> endSession(@RequestParam String sessionId) {
        streamingService.endSession(sessionId);
        
        // Close any associated SSE emitter
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
        
        return ResponseEntity.ok(Map.of("status", "Session ended"));
    }

    /**
     * Server-Sent Events endpoint for receiving real-time transcription updates
     * @param sessionId Session ID to subscribe to
     * @return SSE emitter for real-time updates
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToTranscriptionEvents(@RequestParam String sessionId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // Configure emitter
        emitter.onCompletion(() -> {
            emitters.remove(sessionId);
            log.info("SSE connection completed for session: {}", sessionId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(sessionId);
            log.info("SSE connection timed out for session: {}", sessionId);
        });
        
        emitter.onError(ex -> {
            emitters.remove(sessionId);
            log.error("SSE error for session {}: {}", sessionId, ex.getMessage());
        });
        
        // Store emitter by session ID
        emitters.put(sessionId, emitter);
        
        // Set up callback for streaming service to send updates through SSE
        Consumer<StreamingTranscriptionResponse> callback = response -> {
            try {
                emitter.send(response);
            } catch (IOException e) {
                log.error("Failed to send SSE event", e);
                emitter.completeWithError(e);
            }
        };
        
        // Register callback with streaming service
        try {
            streamingService.processAudioChunk(
                    new StreamingTranscriptionRequest() {{
                        setSessionId(sessionId);
                    }}, 
                    callback
            );
        } catch (Exception e) {
            log.error("Failed to register SSE callback", e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
} 