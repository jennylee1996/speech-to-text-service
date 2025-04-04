package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class WebSocketTranscriptionRequest {
    private String audioData; // Base64 encoded audio data
    private String languageCode;
} 