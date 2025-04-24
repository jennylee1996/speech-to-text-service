package com.fyp.speechtotextservice.service;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.assemblyai.api.resources.transcripts.types.TranscriptLanguageCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fyp.speechtotextservice.config.AssemblyAIConfig;
import com.fyp.speechtotextservice.dto.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
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
        String transcribedText = convertFileToText(videoFile);

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
        if (isYouTubeUrl(mediaUrl)) {
            try {
                String tempDir = "C:/temp";
                String outputFilename = tempDir + "/youtube_" + System.currentTimeMillis() + ".mp3";
                
                log.info("Downloading YouTube audio to: {}", outputFilename);
                downloadAudio(mediaUrl, outputFilename);

                File audioFile = new File(outputFilename);
                if (!audioFile.exists() || audioFile.length() == 0) {
                    throw new IOException("Audio file does not exist or is empty: " + outputFilename);
                }

                // Transcribe using AssemblyAI
//                String transcribedText = AssemblyAiConvertor.convertFileToText(config, audioFile);
                String transcribedText = convertFileToText(audioFile);

                log.info("Transcription completed for URL: {}", mediaUrl);

                if (audioFile.exists()) {
                    boolean deleted = audioFile.delete();
                    if (deleted) {
                        log.info("Deleted temporary audio file: {}", outputFilename);
                    } else {
                        log.warn("Failed to delete temporary audio file: {}", outputFilename);
                    }
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

    private String convertFileToText(File file) throws IOException {

        AssemblyAI assemblyAI = AssemblyAI.builder()
                .apiKey(config.getApiKey())
                .build();

        // Configure transcription with automatic language detection
        var transcriptParams = com.assemblyai.api.resources.transcripts.types.TranscriptOptionalParams.builder()
                .languageDetection(true)
                .build();

        // Upload and transcribe the audio file
        log.info("Uploading and transcribing audio from file: {}", file.getAbsolutePath());
        Transcript transcript = assemblyAI.transcripts().transcribe(file, transcriptParams);

        // Check transcription result
        if (transcript.getStatus() == null || !transcript.getStatus().toString().equals("completed")) {
            throw new IOException("Transcription failed: " + transcript.getError());
        }

        Optional<String> transcribedText = transcript.getText();
        Optional<TranscriptLanguageCode> detectedLanguage = transcript.getLanguageCode();

        if (transcribedText.isPresent()) {
            log.info("Transcription completed: {}", transcribedText.get());
            log.info("Detected language: {}", detectedLanguage);
            return transcribedText.get();
        } else {
            log.warn("Transcription completed but no text was returned.");
            return null;
        }
    }

    private void downloadAudio(String youtubeUrl, String outputPath) throws IOException, InterruptedException {
        log.info("Downloading audio from YouTube URL: {} to {}", youtubeUrl, outputPath);

        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Build the yt-dlp command to download the best audio in its native format
        ProcessBuilder pb = new ProcessBuilder(
                config.getYtDlpPath(),
                "-f", "bestaudio", // Download the best audio stream in its native format
                youtubeUrl,
                "-o", outputPath
        );
        pb.redirectErrorStream(true); // Combine stdout and stderr

        Process process = pb.start();

        // Capture output for debugging and error reporting
        StringBuilder processOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("yt-dlp: {}", line);
                processOutput.append(line).append("\n");
            }
        }

        // Wait with a timeout (e.g., 5 minutes)
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            process.destroy();
            throw new IOException("yt-dlp process timed out after 5 minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed with exit code " + exitCode + ": " + processOutput.toString());
        }

        log.info("Successfully downloaded audio from YouTube URL: {}", youtubeUrl);
    }

    private boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
}
