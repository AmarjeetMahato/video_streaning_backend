package com.youtube.domain.Videos.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.youtube.domain.Videos.dto.VideoDto;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.domain.Videos.mapper.VideoMapper;
import com.youtube.domain.Videos.repository.VideoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {
    private final Cloudinary cloudinary;
    final private  VideoRepository videoRepository;
      final  private VideoMapper videoMapper;
      final  private  VideoProcessingService videoProcessingService;

    @Override
    public Video createVideo(VideoDto videoDto, MultipartFile file) {

             Video video = this.videoMapper.toEntity(videoDto,file);
             Video savedVideo = this.videoRepository.save(video);
        try {
            Path tempPath = Files.createTempFile("raw_", file.getOriginalFilename());
            file.transferTo(tempPath);

            // 3. Start Async Processing using Virtual Threads
            // Thread.ofVirtual() is the Java 21+ way to handle lightweight concurrency
            Thread.ofVirtual().start(() -> {
                videoProcessingService.processAndUpload(savedVideo.getId(), tempPath);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return  savedVideo;
    }

    @Override
    public Video fetchVideo(String videoId) {
           if(videoId == null || videoId.isEmpty()) throw new RuntimeException("videoId is required");
           return  videoRepository.findById(videoId)
                   .orElseThrow(()-> new RuntimeException("Video not found with Id " + videoId));
    }

    @Override
    public List<Video> fetchAllVideos(int limit, int page) {
        // 1. Pageable ऑब्जेक्ट बनाएँ (पेज नंबर, साइज, और सॉर्टिंग)
        // यहाँ हम 'createdAt' के आधार पर लेटेस्ट वीडियो सबसे ऊपर दिखा रहे हैं
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());

        // 2. Repository से पेज फेच करें
        Page<Video> videoPage = videoRepository.findAll(pageable);

        // 3. कंटेंट को लिस्ट के रूप में वापस करें
        return videoPage.getContent();
    }

    @Override
    @Transactional
    public void deleteVideo(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + videoId));
        try {
            // 2. Cloudinary से फाइल्स डिलीट करें (Optional but recommended)
            // आप 'videoId' नाम का पूरा फोल्डर क्लाउड से डिलीट कर सकते हैं
            deleteFolderFromCloudinary(videoId);

            // 3. डेटाबेस से डिलीट करें
            // अगर आपने CascadeType.ALL लगाया है, तो Quality और Index अपने आप डिलीट हो जाएंगे
            videoRepository.delete(video);

            System.out.println("✅ Video and all related data deleted successfully: " + videoId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete video: " + e.getMessage());
        }
    }

    private void deleteFolderFromCloudinary(String videoId) throws Exception {
        // क्लाउड पर फोल्डर का पाथ (जैसे 'videos/video123')
        String folderPath = "videos/" + videoId;

        // 1. फोल्डर के अंदर की सभी फाइल्स डिलीट करें
        cloudinary.api().deleteResourcesByPrefix(folderPath, ObjectUtils.emptyMap());

        // 2. खाली फोल्डर को डिलीट करें
        cloudinary.api().deleteFolder(folderPath, ObjectUtils.emptyMap());
    }
}
