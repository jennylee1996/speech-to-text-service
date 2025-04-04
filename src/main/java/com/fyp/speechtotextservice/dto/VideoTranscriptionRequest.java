package com.fyp.speechtotextservice.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Data
public class VideoTranscriptionRequest {
    private MultipartFile videoFile;
    private String languageCode;
} 