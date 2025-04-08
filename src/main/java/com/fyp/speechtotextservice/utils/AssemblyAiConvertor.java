package com.fyp.speechtotextservice.utils;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@NoArgsConstructor
public class AssemblyAiConvertor {

    public static String convertFileToText(File file) throws IOException {

        AssemblyAI assemblyAI = AssemblyAI.builder()
                .apiKey("")
                .build();
        // Upload and transcribe the audio file
        log.info("Uploading and transcribing audio");
        Transcript transcript = assemblyAI.transcripts().transcribe(file);

        // Check transcription result
        if (transcript.getStatus() == null || !transcript.getStatus().toString().equals("completed")) {
            throw new IOException("Transcription failed: " + transcript.getError());
        }
        Optional<String> transcribedText = transcript.getText();

        if (transcribedText.isPresent()) {
            log.info("Transcription completed: {}", transcribedText);
            return transcribedText.get();
        }
        return null;
    }

}
