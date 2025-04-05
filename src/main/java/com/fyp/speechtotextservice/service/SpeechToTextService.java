package com.fyp.speechtotextservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.speechtotextservice.config.AssemblyAIConfig;
import com.fyp.speechtotextservice.dto.*;
import com.fyp.speechtotextservice.utils.AssemblyAiConvertor;
import com.fyp.speechtotextservice.utils.YouTubeDownloader;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SpeechToTextService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    @Autowired
    private final AssemblyAIConfig config;

    @Autowired
    public SpeechToTextService(AssemblyAIConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // Video transcription
    public TranscriptionResponse transcribeVideo(VideoTranscriptionRequest request) throws IOException {

        // Convert MultipartFile to File
        File videoFile = convertMultipartFileToFile(request.getVideoFile());

        // Convert File to Text
        String transcribedText = AssemblyAiConvertor.convertFileToText(config, videoFile);

        // Create and return the TranscriptionResponse
        TranscriptionResponse response = new TranscriptionResponse();
        response.setStatus("completed");
        response.setText(transcribedText);
        return response;
    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        file.deleteOnExit();
        return file;
    }

    // Link transcription
    public TranscriptionResponse transcribeFromLink(LinkTranscriptionRequest request) throws IOException {

        String mediaUrl = request.getMediaUrl();
        log.info("Transcribing from URL: {}", mediaUrl);
        
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            throw new IOException("Media URL cannot be empty");
        }

        // Check if it's a YouTube URL
        if (YouTubeDownloader.isYouTubeUrl(mediaUrl)) {
            try {
                String tempDir = "C:/temp";
                String outputFilename = tempDir + "/youtube_" + System.currentTimeMillis() + ".mp3";
                
                log.info("Downloading YouTube audio to: {}", outputFilename);
                YouTubeDownloader.downloadAudio(mediaUrl, outputFilename);

                File audioFile = new File(outputFilename);
                if (!audioFile.exists() || audioFile.length() == 0) {
                    throw new IOException("Audio file does not exist or is empty: " + outputFilename);
                }

                // Transcribe using AssemblyAI
                String transcribedText = AssemblyAiConvertor.convertFileToText(config, audioFile);
                log.info("Transcription completed for URL: {}", mediaUrl);

                if (audioFile.exists()) {
//                    boolean deleted = audioFile.delete();
//                    if (deleted) {
//                        log.info("Deleted temporary audio file: {}", outputFilename);
//                    } else {
//                        log.warn("Failed to delete temporary audio file: {}", outputFilename);
//                    }
                }
                // Create and return the TranscriptionResponse
                TranscriptionResponse response = new TranscriptionResponse();
                response.setStatus("completed");
                response.setText(transcribedText);
                return response;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("YouTube download was interrupted: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Media URL is not a YouTube URL: " + mediaUrl);
        }
    }
}