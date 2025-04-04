package com.fyp.speechtotextservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TranscriptionResponse {
    private String id;
    private String status;
    private String text;
    private String error;
} 