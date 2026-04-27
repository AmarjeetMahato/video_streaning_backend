package com.youtube.domain.Videos.services;

import com.youtube.domain.Videos.dto.VideoDto;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.domain.Videos.mapper.VideoMapper;
import com.youtube.domain.Videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements IVideoService {

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
        return null;
    }

    @Override
    public List<Video> fetchAllVideos(int limit, int offset) {
        return List.of();
    }

    @Override
    public void deleteVideo(String videoId) {

    }
}
