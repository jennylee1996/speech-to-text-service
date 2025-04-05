package com.fyp.speechtotextservice.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoTranscriptionRequest {
    private MultipartFile videoFile;
    private String languageCode;
} 