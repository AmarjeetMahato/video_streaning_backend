package com.youtube.domain.Videos.services;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.youtube.domain.Videos.repository.VideoRepository;
import com.youtube.enums.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final Cloudinary cloudinary;
    private final VideoRepository videoRepository;

    public void processAndUpload(String videoId, Path inputPath) {
        var resolutions = List.of("480p", "720p", "1080p");

        // Java 25 Virtual Thread Executor
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for (String res : resolutions) {
                executor.submit(() -> {
                    try {
                        processResolution(videoId, inputPath, res);
                    } catch (Exception e) {
                        throw new RuntimeException("Resolution failed: " + res, e);
                    }
                });
            }
            // Auto-join: Wait for all threads to finish
        } catch (Exception e) {
            updateVideoStatus(videoId, VideoStatus.FAILED, null);
            return;
        }

        try {
            // Sabhi resolutions ke baad Master Playlist banayein
            String masterUrl = createAndUploadMasterPlaylist(videoId);
            updateVideoStatus(videoId, VideoStatus.READY, masterUrl);
        } catch (IOException e) {
            updateVideoStatus(videoId, VideoStatus.FAILED, null);
        } finally {
            cleanup(videoId, inputPath);
        }
    }

    private void processResolution(String videoId, Path inputPath, String res) throws Exception {
        String outputDir = "temp/%s/%s/".formatted(videoId, res);
        Files.createDirectories(Paths.get(outputDir));

        System.out.println("🚀 Starting transcoding for: " + res);

        // 1. Watermark Text: User ID (Subtle @ 0.1 opacity)
        String watermarkText = "Youtube-Clone-ID-" + videoId.substring(0, 8);

        // 2. FFmpeg Command with Watermark Overlay
        String ffmpegCmd = """
    ffmpeg -i "%s" -vf "scale=%s,drawtext=text='%s':x=10:y=10:fontsize=20:fontcolor=white@0.1:shadowcolor=black@0.1:shadowx=1:shadowy=1" \
    -codec:v libx264 -crf 23 -preset fast \
    -codec:a aac -b:a 192k -ar 44100 \
    -hls_time 10 -hls_playlist_type vod \
    -hls_segment_filename "%ssegment%%03d.ts" "%splaylist.m3u8"
    """.formatted(inputPath.toAbsolutePath(), getScale(res), watermarkText, outputDir, outputDir);

        // Command execute karein
        executeCommand(ffmpegCmd, "HLS-" + res);

        // 3. YouTube Preview Feature: Generate Sprite Sheet (Only for 480p to save resources)
        if (res.equals("480p")) {
            generateSpriteSheet(videoId, inputPath);
        }

        System.out.println("✅ Transcoding finished for " + res + ". Starting Cloudinary upload...");
        uploadToCloudinary(videoId, res, outputDir);
    }

    private void executeCommand(String command, String taskName) throws Exception {
        String shell = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "bash";
        String shellArg = System.getProperty("os.name").toLowerCase().contains("win") ? "/c" : "-c";

        ProcessBuilder pb = new ProcessBuilder(shell, shellArg, command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Uncomment niche wali line agar aapko FFmpeg ka live progress dekhna hai
                // System.out.println("[" + taskName + "]: " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException(taskName + " failed with exit code: " + exitCode);
        }
    }

    private void generateSpriteSheet(String videoId, Path inputPath) throws Exception {
        String outputDir = "temp/%s/previews/".formatted(videoId);
        Files.createDirectories(Paths.get(outputDir));

        // Har 5 ya 10 second par ek image lega aur 5x5 grid banayega
        String spriteCmd = """
        ffmpeg -i "%s" -vf "fps=1/10,scale=160:90,tile=5x5" -q:v 3 "%ssprite.jpg"
        """.formatted(inputPath.toAbsolutePath(), outputDir);

        executeCommand(spriteCmd, "Sprite-Gen");

        // Cloudinary upload logic for sprite
        File spriteFile = new File(outputDir + "sprite.jpg");
        if(spriteFile.exists()) {
            cloudinary.uploader().upload(spriteFile, ObjectUtils.asMap(
                    "public_id", videoId + "/previews/sprite",
                    "resource_type", "image"
            ));
        }
    }
    private String createAndUploadMasterPlaylist(String videoId) throws IOException {
        System.out.println("Creating Master Playlist for: " + videoId);

        // Yahan variable define karna zaroori hai
        String masterContent = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=854x480
        480p/playlist.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1400000,RESOLUTION=1280x720
        720p/playlist.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=2800000,RESOLUTION=1920x1080
        1080p/playlist.m3u8
        """;

        // Path create karein (ensure karein ki temp/videoId folder exist karta hai)
        Path path = Paths.get("temp/%s/master.m3u8".formatted(videoId));
        Files.createDirectories(path.getParent()); // Folder create karne ke liye safety check
        Files.writeString(path, masterContent);

        // Cloudinary upload
        var result = cloudinary.uploader().upload(path.toFile(), ObjectUtils.asMap(
                "public_id", videoId + "/master",
                "resource_type", "raw"
        ));

        String secureUrl = (String) result.get("secure_url");
        System.out.println("Master Playlist uploaded successfully: " + secureUrl);

        return secureUrl;
    }

    private void uploadToCloudinary(String videoId, String res, String dirPath) throws IOException {
        File folder = new File(dirPath);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.err.println("No files found in " + dirPath + " to upload!");
            return;
        }

        System.out.println("Found " + files.length + " files in " + res + " folder. Uploading...");

        for (File file : files) {
            String publicId = "%s/%s/%s".formatted(videoId, res, file.getName().replaceFirst("[.][^.]+$", ""));

            // Agar file .ts hai toh resource_type 'video' hona chahiye Cloudinary par HLS ke liye
            String resourceType = file.getName().endsWith(".ts") ? "video" : "raw";

            cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceType
            ));
        }
        System.out.println("All files for " + res + " uploaded successfully.");
    }

    private void updateVideoStatus(String videoId, VideoStatus status, String url) {
        videoRepository.findById(videoId).ifPresent(v -> {
            v.setStatus(status);
            if (url != null) v.setOriginalFilePath(url);
            videoRepository.save(v);
        });
    }

    private String getScale(String res) {
        return switch (res) {
            case "480p" -> "854:480";
            case "720p" -> "1280:720";
            case "1080p" -> "1920:1080";
            default -> "1280:720";
        };
    }

    private void cleanup(String videoId, Path inputPath) {
        try {
            Files.deleteIfExists(inputPath);
            // Recursive delete temp folder
            try (var files = Files.walk(Paths.get("temp/" + videoId))) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        } catch (IOException ignored) {}
    }
}