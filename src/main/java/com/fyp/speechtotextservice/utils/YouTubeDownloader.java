package com.fyp.speechtotextservice.utils;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class YouTubeDownloader {

    public static void downloadAudio(String youtubeUrl, String outputPath) throws IOException, InterruptedException {
        log.info("Downloading audio from YouTube URL: {} to {}", youtubeUrl, outputPath);

        String ytDlpPath = "C:\\Users\\jenny\\AppData\\Local\\Programs\\Python\\Python313\\Scripts\\yt-dlp.exe";

        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Build the yt-dlp command to download the best audio in its native format
        ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath,
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

    public static boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
} 