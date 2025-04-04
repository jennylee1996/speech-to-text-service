package com.fyp.speechtotextservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.speechtotextservice.config.AssemblyAIConfig;
import com.fyp.speechtotextservice.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SpeechToTextService {

    private static final String ASSEMBLY_AI_BASE_URL = "https://api.assemblyai.com/v2";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
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

    // WebSocket transcription
    public TranscriptionResponse transcribeWebSocketAudio(WebSocketTranscriptionRequest request) throws IOException {
        // Convert base64 audio to bytes
        byte[] audioBytes = Base64.getDecoder().decode(request.getAudioData());
        
        // Create multipart request
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", "audio.wav",
                        RequestBody.create(audioBytes, MediaType.parse("audio/wav")))
                .addFormDataPart("language_code", request.getLanguageCode())
                .build();

        Request httpRequest = new Request.Builder()
                .url(ASSEMBLY_AI_BASE_URL + "/transcript")
                .addHeader("Authorization", config.getApiKey())
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to submit transcription: " + response.body().string());
            }
            
            TranscriptionResponse transcriptionResponse = objectMapper.readValue(
                    response.body().string(),
                    TranscriptionResponse.class
            );
            return pollTranscriptionResult(transcriptionResponse.getId());
        }
    }

    // Video transcription
    public TranscriptionResponse transcribeVideo(VideoTranscriptionRequest request) throws IOException {
        MultipartFile videoFile = request.getVideoFile();
        
        // Create multipart request
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", videoFile.getOriginalFilename(),
                        RequestBody.create(videoFile.getBytes(), MediaType.parse(videoFile.getContentType())))
                .addFormDataPart("language_code", request.getLanguageCode())
                .build();

        Request httpRequest = new Request.Builder()
                .url(ASSEMBLY_AI_BASE_URL + "/transcript")
                .addHeader("Authorization", config.getApiKey())
                .post(requestBody)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to submit transcription: " + response.body().string());
            }
            
            TranscriptionResponse transcriptionResponse = objectMapper.readValue(
                    response.body().string(),
                    TranscriptionResponse.class
            );
            return pollTranscriptionResult(transcriptionResponse.getId());
        }
    }

    // Link transcription
    public TranscriptionResponse transcribeFromLink(LinkTranscriptionRequest request) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(request);
        Request httpRequest = new Request.Builder()
                .url(ASSEMBLY_AI_BASE_URL + "/transcript")
                .addHeader("Authorization", config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to submit transcription: " + response.body().string());
            }
            
            TranscriptionResponse transcriptionResponse = objectMapper.readValue(
                    response.body().string(),
                    TranscriptionResponse.class
            );
            return pollTranscriptionResult(transcriptionResponse.getId());
        }
    }

    private TranscriptionResponse pollTranscriptionResult(String transcriptionId) throws IOException {
        while (true) {
            Request request = new Request.Builder()
                    .url(ASSEMBLY_AI_BASE_URL + "/transcript/" + transcriptionId)
                    .addHeader("Authorization", config.getApiKey())
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get transcription result: " + response.body().string());
                }

                TranscriptionResponse result = objectMapper.readValue(
                        response.body().string(),
                        TranscriptionResponse.class
                );

                if ("completed".equals(result.getStatus())) {
                    return result;
                } else if ("error".equals(result.getStatus())) {
                    throw new IOException("Transcription failed: " + result.getError());
                }

                // Wait for 3 seconds before polling again
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Transcription polling interrupted", e);
            }
        }
    }
} 