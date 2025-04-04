package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class TranscriptionRequest {
    private String audioUrl;
    private String languageCode;
} 