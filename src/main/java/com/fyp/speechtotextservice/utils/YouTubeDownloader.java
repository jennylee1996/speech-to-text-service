package com.fyp.speechtotextservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
public class YouTubeDownloader {
    
    /**
     * Downloads audio from a YouTube URL in MP3 format
     * 
     * @param youtubeUrl The YouTube video URL
     * @param outputPath The path where the audio file should be saved
     * @throws IOException If there's an error during download
     * @throws InterruptedException If the process is interrupted
     */
    public static void downloadAudio(String youtubeUrl, String outputPath) throws IOException, InterruptedException {
        log.info("Downloading audio from YouTube URL: {} to {}", youtubeUrl, outputPath);


        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        String ytDlpPath = "C:\\Users\\jenny\\AppData\\Local\\Programs\\Python\\Python313\\Scripts\\yt-dlp.exe";
        String ffmpegPath = "C:\\ffmpeg\\ffmpeg-7.0.2-full_build\\bin\\ffmpeg.exe";

        ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath,
                "-x",
                "--audio-format", "mp3",
                "--ffmpeg-location", ffmpegPath,
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

    /**
     * Checks if a URL is a YouTube URL
     * 
     * @param url The URL to check
     * @return true if it's a YouTube URL, false otherwise
     */
    public static boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
} 