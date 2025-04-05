package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class StreamingTranscriptionRequest {
    private String audioChunk; // Base64 encoded audio chunk
    private String sessionId; // Session identifier for continuous transcription
    private String languageCode; // Optional language code, defaults to English
} 