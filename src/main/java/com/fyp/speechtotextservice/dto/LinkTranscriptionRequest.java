package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class LinkTranscriptionRequest {
    private String mediaUrl;
    private String languageCode;
} 