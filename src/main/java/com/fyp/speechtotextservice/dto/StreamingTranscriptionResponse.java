package com.fyp.speechtotextservice.dto;

import lombok.Data;

@Data
public class StreamingTranscriptionResponse {
    private String sessionId;     // Session identifier
    private String text;          // Transcribed text
    private boolean isFinal;      // Whether this is a final transcription
    private String messageType;   // Type of message (e.g., "PartialTranscript", "FinalTranscript", "Error")
    private String error;         // Error message, if any
} 