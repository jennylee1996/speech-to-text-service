package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class TranscriptionResponse {
    private String id;
    private String status;
    private String text;
    private String error;
} 