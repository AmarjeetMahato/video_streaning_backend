package com.youtube.domain.Videos.services;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.youtube.domain.VideoIndex.dto.VideoIndexDto;
import com.youtube.domain.VideoIndex.services.VideoIndexServiceImpl;
import com.youtube.domain.VideoQuality.dto.VideoQualityDto;
import com.youtube.domain.VideoQuality.services.VideoQualityServiceImpl;
import com.youtube.domain.Videos.repository.VideoRepository;
import com.youtube.enums.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final Cloudinary cloudinary;
    private final VideoRepository videoRepository;
    private  final VideoIndexServiceImpl videoIndexService;
    private  final VideoQualityServiceImpl videoQualityService;

    public void processAndUpload(String videoId, Path inputPath) {
        var resolutions = List.of("480p", "720p", "1080p");
        List<Future<?>> futures = new ArrayList<>();

        // Java 25: Virtual Thread Executor का उपयोग करके वीडियो प्रोसेसिंग
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // यह 'try-with-resources' ब्लॉक है। जब यह ब्लॉक खत्म होगा,
            // तो यह अपने आप इंतज़ार करेगा (Auto-close) ताकि सारे टास्क पूरे हो जाएं।
            for (String res : resolutions) {
                // 'resolutions' लिस्ट में मौजूद हर resolution (जैसे 1080p, 720p) के लिए लूप चलेगा।
                futures.add(executor.submit(() -> {
                    // हर resolution के लिए एक नया 'Virtual Thread' बनेगा और काम शुरू होगा।
                    try {
                        // यह मुख्य मेथड है जो वीडियो को प्रोसेस (Transcode) करेगा।
                        String uploadedUrl =  processResolution(videoId, inputPath, res);

                        VideoQualityDto dto = VideoQualityDto.builder()
                                .videoId(videoId)
                                .quality(res)
                                .url(uploadedUrl)
                                .format("mp4")
                                .build();

                        videoQualityService.createVideoQuality(dto);
                    } catch (Exception e) {
                        // अगर किसी एक टास्क में गड़बड़ होती है, तो यहाँ एरर थ्रो होगा।
                        throw new RuntimeException("Resolution failed: " + res, e);
                    }
                }));
            }
            // यहाँ कोई 'executor.shutdown()' लिखने की ज़रूरत नहीं है।
            // 'try' ब्लॉक के अंत में यह खुद ही सारे थ्रेड्स के खत्म होने का वेट करेगा।
            // 🔥 CRITICAL: Wait for all tasks
            for (Future<?> future : futures) {
                future.get(); // blocks + propagates exception
            }

        } catch (Exception e) {
            // अगर पूरा प्रोसेस फेल हो जाता है या कोई गंभीर एरर आता है, तो स्टेटस अपडेट करें।
            updateVideoStatus(videoId, VideoStatus.FAILED, null);
            return;
        }

        try {
            // Sabhi resolutions ke baad Master Playlist banayein
            String masterUrl = createAndUploadMasterPlaylist(videoId);
            String spriteSheetUrl = generateSpriteSheet(videoId, inputPath);
            VideoIndexDto dto = VideoIndexDto.builder()
                    .videoId(videoId)
                    .masterPlaylistUrl(masterUrl)
                    .spriteSheetUrl(spriteSheetUrl)
                    .build();


            updateVideoStatus(videoId, VideoStatus.READY, masterUrl);
            videoIndexService.createVideoIndex(dto);
        } catch (IOException e) {
            updateVideoStatus(videoId, VideoStatus.FAILED, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            cleanup(videoId, inputPath);
        }
    }

    private String processResolution(String videoId, Path inputPath, String res) throws Exception {
        // 1. आउटपुट डायरेक्टरी का पाथ बनाना (जैसे: temp/video123/1080p/)
        String outputDir = "temp/%s/%s/".formatted(videoId, res);
        // अगर फोल्डर मौजूद नहीं है, तो नया फोल्डर बनाना
        Files.createDirectories(Paths.get(outputDir));

        System.out.println("🚀 Starting transcoding for: " + res);

        // 2. वॉटरमार्क के लिए टेक्स्ट तैयार करना (वीडियो आईडी के पहले 8 अक्षर)
        String watermarkText = "Youtube-Clone-ID-" + videoId.substring(0, 8);

        // 3. FFmpeg कमांड तैयार करना:
        // -i: इनपुट वीडियो फाइल
        // -vf: वीडियो फ़िल्टर (स्केल/साइज बदलना और वॉटरमार्क टेक्स्ट लिखना)
        // fontcolor=white@0.1: वॉटरमार्क को बहुत हल्का (पारदर्शी) रखना ताकि वीडियो साफ़ दिखे
        // -hls: वीडियो को छोटे-छोटे टुकड़ों (.ts files) में बाँटना ताकि बफरिंग कम हो (HLS Streaming)
        String ffmpegCmd = """
ffmpeg -i "%s" -vf "scale=%s,drawtext=text='%s':x=10:y=10:fontsize=20:fontcolor=white@0.1:shadowcolor=black@0.1:shadowx=1:shadowy=1" \
-codec:v libx264 -crf 23 -preset fast \
-codec:a aac -b:a 192k -ar 44100 \
-hls_time 10 -hls_playlist_type vod \
-hls_segment_filename "%ssegment%%03d.ts" "%splaylist.m3u8"
""".formatted(inputPath.toAbsolutePath(), getScale(res), watermarkText, outputDir, outputDir);

        // ऊपर तैयार की गई FFmpeg कमांड को सिस्टम पर चलाना
        executeCommand(ffmpegCmd, "HLS-" + res);

        // 4. YouTube Preview फीचर:
        // सिर्फ 480p वाले टास्क के दौरान वीडियो की छोटी थंबनेल इमेज (Sprite Sheet) बनाना
        if (res.equals("480p")) {
            generateSpriteSheet(videoId, inputPath);
        }

        System.out.println("✅ Transcoding finished for " + res + ". Starting Cloudinary upload...");

        // 5. फाइनल स्टेप: तैयार की गई फाइलों को Cloudinary (Cloud Storage) पर अपलोड करना
        String playlistUrl =  uploadToCloudinary(videoId, res, outputDir);
        return playlistUrl;
    }

    private void executeCommand(String command, String taskName) throws Exception {
        // 1. ऑपरेटिंग सिस्टम की पहचान करना (Windows है या Linux/Mac)
        // अगर Windows है तो "cmd" इस्तेमाल होगा, वरना "bash" (Linux/Mac के लिए)
        String shell = System.getProperty("os.name").toLowerCase().contains("win") ? "cmd" : "bash";
        String shellArg = System.getProperty("os.name").toLowerCase().contains("win") ? "/c" : "-c";

        // 2. प्रोसेस बिल्डर सेटअप करना: यह असल में कमांड चलाने वाला इंजन है
        ProcessBuilder pb = new ProcessBuilder(shell, shellArg, command);

        // एरर और आउटपुट को एक ही स्ट्रीम में मिला देना ताकि उन्हें पढ़ना आसान हो
        pb.redirectErrorStream(true);

        // कमांड को शुरू (Start) करना
        Process process = pb.start();

        // 3. कमांड का आउटपुट पढ़ना:
        // FFmpeg जब काम करता है तो बहुत सारी जानकारी प्रिंट करता है, उसे यहाँ 'reader' से पढ़ा जा रहा है
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // अगर आप प्रोसेस की लाइव प्रोग्रेस देखना चाहते हैं, तो नीचे वाली लाइन को अनकमेंट कर सकते हैं
                // System.out.println("[" + taskName + "]: " + line);
            }
        }

        // 4. कमांड खत्म होने का इंतज़ार करना
        int exitCode = process.waitFor();

        // 5. चेक करना कि काम सही से हुआ या नहीं
        // अगर exitCode 0 नहीं है, इसका मतलब कमांड में कोई गड़बड़ हुई है (Error आया है)
        if (exitCode != 0) {
            throw new RuntimeException(taskName + " failed with exit code: " + exitCode);
        }
    }

    private String generateSpriteSheet(String videoId, Path inputPath) throws Exception {
        // 1. प्रीव्यू इमेजेस के लिए एक अलग फोल्डर पाथ बनाना (जैसे: temp/video123/previews/)
        String outputDir = "temp/%s/previews/".formatted(videoId);
        Files.createDirectories(Paths.get(outputDir));

        // 2. FFmpeg कमांड तैयार करना (Sprite Sheet बनाने के लिए):
        // fps=1/10: हर 10 सेकंड के वीडियो में से 1 फ्रेम (फोटो) निकालना।
        // scale=160:90: हर छोटी फोटो का साइज 160x90 पिक्सल रखना।
        // tile=5x5: इन छोटी फोटो को 5 कॉलम और 5 रो (कुल 25 फोटो) के एक ग्रिड (Grid) में जोड़ना।
        // -q:v 3: इमेज की क्वालिटी सेट करना (1 से 31 के बीच, 3 अच्छी क्वालिटी है)।
        String spriteCmd = """
    ffmpeg -i "%s" -vf "fps=1/10,scale=160:90,tile=5x5" -q:v 3 "%ssprite.jpg"
    """.formatted(inputPath.toAbsolutePath(), outputDir);

        // ऊपर तैयार की गई कमांड को रन करना
        executeCommand(spriteCmd, "Sprite-Gen");

        // 3. बनी हुई 'sprite.jpg' फाइल को Cloudinary पर अपलोड करना
        File spriteFile = new File(outputDir + "sprite.jpg");
        if(spriteFile.exists()) {
            var uploadResult =  cloudinary.uploader().upload(spriteFile, ObjectUtils.asMap(
                    "public_id", videoId + "/previews/sprite", // क्लाउड पर फाइल का नाम और रास्ता
                    "resource_type", "image"                   // बताना कि यह एक फोटो है
            ));

            return  (String) uploadResult.get("secure_url");
        }

        return  null;
    }

    private String createAndUploadMasterPlaylist(String videoId) throws IOException {
        System.out.println("Creating Master Playlist for: " + videoId);

        // 1. मास्टर प्लेलिस्ट का कंटेंट तैयार करना (M3U8 Format)
        // #EXT-X-STREAM-INF: यह प्लेयर को बताता है कि कितनी बैंडविड्थ (इंटरनेट स्पीड)
        // और किस रेजोल्यूशन के लिए कौन सी फाइल (playlist.m3u8) देखनी है।
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

        // 2. फाइल को सेव करने का रास्ता (Path) तय करना
        Path path = Paths.get("temp/%s/master.m3u8".formatted(videoId));

        // यह सुनिश्चित करना कि फोल्डर मौजूद है, वरना एरर आ सकता है
        Files.createDirectories(path.getParent());

        // ऊपर तैयार किए गए टेक्स्ट (masterContent) को 'master.m3u8' फाइल में लिखना
        Files.writeString(path, masterContent);

        // 3. Cloudinary पर अपलोड करना
        // "resource_type", "raw": क्योंकि .m3u8 एक टेक्स्ट फाइल है, इमेज या वीडियो नहीं।
        var result = cloudinary.uploader().upload(path.toFile(), ObjectUtils.asMap(
                "public_id", videoId + "/master",
                "resource_type", "raw"
        ));

        // 4. अपलोड के बाद मिलने वाला सुरक्षित URL (https) निकालना
        String secureUrl = (String) result.get("secure_url");
        System.out.println("Master Playlist uploaded successfully: " + secureUrl);

        // यह URL बाद में डेटाबेस में सेव किया जाएगा ताकि यूजर वीडियो देख सके
        return secureUrl;
    }

    private String uploadToCloudinary(String videoId, String res, String dirPath) throws IOException {
        // 1. उस फोल्डर को खोलना जहाँ प्रोसेस की गई फाइलें रखी हैं (जैसे: temp/video123/1080p/)
        File folder = new File(dirPath);
        File[] files = folder.listFiles(); // फोल्डर के अंदर की सभी फाइलों की लिस्ट बनाना
        String finalPlaylistUrl = "";
        // अगर फोल्डर खाली है या नहीं मिला, तो एरर दिखा कर रुक जाना
        if (files == null || files.length == 0) {
            throw new IOException("No files found in " + dirPath);
        }

        System.out.println("Found " + files.length + " files in " + res + " folder. Uploading...");

        // 2. फोल्डर की हर एक फाइल पर लूप चलाना
        for (File file : files) {
            // क्लाउड पर पाथ: videos/videoId/res/filename
            String fileName = file.getName();
            // क्लाउड पर फाइल का रास्ता और नाम (Public ID) तैयार करना
            // यह फाइल के एक्सटेंशन (जैसे .ts या .m3u8) को हटाकर एक साफ नाम बनाता है
            String publicId = "%s/%s/%s".formatted(videoId, res, file.getName().replaceFirst("[.][^.]+$", ""));

            // 3. Resource Type तय करना (Cloudinary के लिए बहुत ज़रूरी):
            // अगर फाइल '.ts' (वीडियो सेगमेंट) है, तो उसे 'video' की तरह अपलोड करना होगा।
            // अगर फाइल '.m3u8' (प्लेलिस्ट) है, तो उसे 'raw' (टेक्स्ट) की तरह अपलोड करना होगा।
            String resourceType = file.getName().endsWith(".ts") ? "video" : "raw";

            // 4. Cloudinary पर असल अपलोड कमांड
            var uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "public_id", publicId,   // क्लाउड पर किस नाम से सेव होगा
                    "resource_type", resourceType, // फाइल का प्रकार क्या है
                    "use_filename", true,
                    "unique_filename", false
            ));

            // अगर यह फाइल playlist.m3u8 है, तो इसका URL सेव कर लें
            if(fileName.equals("playlist.m3u8")){
                 finalPlaylistUrl  = (String) uploadResult.get("secure_url");
            }
        }

        System.out.println("All files for " + res + " uploaded successfully.");
        return finalPlaylistUrl;
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
            // 1. ओरिजिनल इनपुट वीडियो को डिलीट करना
            // काम खत्म होने के बाद अपलोड की गई मेन फाइल की अब सर्वर पर ज़रूरत नहीं है।
            Files.deleteIfExists(inputPath);

            // 2. पूरे टेम्परेरी फोल्डर को डिलीट करना (Recursive Delete)
            // 'Files.walk' उस वीडियो के फोल्डर के अंदर की सभी फाइलों और सब-फोल्डर्स की लिस्ट बनाता है।
            try (var files = Files.walk(Paths.get("temp/" + videoId))) {
                files.sorted(Comparator.reverseOrder()) // पहले अंदर की फाइलें, फिर फोल्डर डिलीट करने के लिए उल्टा क्रम
                        .map(Path::toFile)                 // Path को File ऑब्जेक्ट में बदलना
                        .forEach(File::delete);            // एक-एक करके सबको डिलीट करना
            }
        } catch (IOException ignored) {
            // अगर फाइल डिलीट नहीं हो पाती (शायद किसी और प्रोसेस ने उसे पकड़ रखा है),
            // तो एरर को इग्नोर कर दिया जाता है ताकि मुख्य प्रोग्राम न रुके।
        }
    }
}