package com.fyp.speechtotextservice.controller;

import com.fyp.speechtotextservice.dto.WebSocketTranscriptionRequest;
import com.fyp.speechtotextservice.dto.TranscriptionResponse;
import com.fyp.speechtotextservice.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class WebSocketTranscriptionController {

    private final SpeechToTextService speechToTextService;

    @Autowired
    public WebSocketTranscriptionController(SpeechToTextService speechToTextService) {
        this.speechToTextService = speechToTextService;
    }

    @MessageMapping("/transcribe")
    @SendTo("/topic/transcription")
    public TranscriptionResponse handleTranscription(WebSocketTranscriptionRequest request) {
        try {
            return speechToTextService.transcribeWebSocketAudio(request);
        } catch (Exception e) {
            log.error("Error in WebSocket transcription", e);
            TranscriptionResponse errorResponse = new TranscriptionResponse();
            errorResponse.setStatus("error");
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }
} 