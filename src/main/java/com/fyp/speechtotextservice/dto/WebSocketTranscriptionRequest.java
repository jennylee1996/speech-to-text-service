package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class WebSocketTranscriptionRequest {
    private String audioData;
    private String languageCode;
} 