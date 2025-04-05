package com.fyp.speechtotextservice.controller;

import com.fyp.speechtotextservice.dto.LinkTranscriptionRequest;
import com.fyp.speechtotextservice.dto.TranscriptionResponse;
import com.fyp.speechtotextservice.dto.VideoTranscriptionRequest;
import com.fyp.speechtotextservice.service.LiveSpeechToTextService;
import com.fyp.speechtotextservice.service.SpeechToTextService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;
    private final LiveSpeechToTextService liveSpeechToTextService;

    @PostMapping("/transcribe/video")
    public ResponseEntity<TranscriptionResponse> transcribeVideo(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "languageCode", defaultValue = "en") String languageCode) {
        try {
            VideoTranscriptionRequest request = new VideoTranscriptionRequest();
            request.setVideoFile(videoFile);
            request.setLanguageCode(languageCode);
            
            TranscriptionResponse response = speechToTextService.transcribeVideo(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error transcribing video", e);
            TranscriptionResponse errorResponse = new TranscriptionResponse();
            errorResponse.setStatus("error");
            errorResponse.setError(e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/transcribe/link")
    public ResponseEntity<TranscriptionResponse> transcribeFromLink(@RequestBody LinkTranscriptionRequest request) {
        try {
            TranscriptionResponse response = speechToTextService.transcribeFromLink(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error transcribing from link", e);
            TranscriptionResponse errorResponse = new TranscriptionResponse();
            errorResponse.setStatus("error");
            errorResponse.setError(e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/liveTranscribe/start")
    public ResponseEntity<String> startTranscription() {
        try {
            liveSpeechToTextService.startTranscriptionSession();
            return ResponseEntity.ok("Transcription session started");
        } catch (Exception e) {
            log.error("Error starting transcription session: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error starting transcription: " + e.getMessage());
        }
    }

    @PostMapping("/liveTranscribe/audio")
    public ResponseEntity<String> sendAudio(@RequestBody byte[] audioData) {
        try {
            liveSpeechToTextService.sendAudio(audioData);
            return ResponseEntity.ok("Audio data sent");
        } catch (Exception e) {
            log.error("Error sending audio data: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error sending audio: " + e.getMessage());
        }
    }

    @GetMapping(value = "/liveTranscribe/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTranscriptions() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        BlockingQueue<String> transcriptionQueue = liveSpeechToTextService.getTranscriptionQueue();

        new Thread(() -> {
            try {
                while (true) {
                    String transcript = transcriptionQueue.take();
                    emitter.send(SseEmitter.event().data(transcript));
                }
            } catch (IOException e) {
                log.error("Error sending SSE event: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                log.error("Transcription streaming interrupted: {}", e.getMessage());
                emitter.complete();
            }
        }).start();

        return emitter;
    }



} 