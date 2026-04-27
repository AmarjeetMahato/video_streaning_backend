package com.youtube.domain.VideoQuality.services;

import com.youtube.domain.VideoQuality.dto.VideoQualityDto;
import com.youtube.domain.VideoQuality.entity.VideoQuality;
import com.youtube.domain.VideoQuality.mapper.VideoQualityMapper;
import com.youtube.domain.VideoQuality.repository.VideoQualityRepository;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.domain.Videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoQualityServiceImpl implements  IVideoQualityService {

    private VideoQualityRepository videoQualityRepository;
    private  VideoRepository videoRepository;
    private  IVideoQualityService videoQualityService;
    private  VideoQualityMapper videoQualityMapper;


    @Override
    public VideoQualityDto createVideoQuality(VideoQualityDto videoQualityDto) {
         try {
             // 1. Pehle check karein ki Video exist karta hai ya nahi
             Video video = videoRepository.findById(videoQualityDto.getVideoId())
                     .orElseThrow(() -> new RuntimeException("Video not found"));
             // 2. DTO ko Entity mein convert karein
             VideoQuality videoQuality  = videoQualityMapper.toEntity(videoQualityDto, video);
             // 3. Database mein save karein
             VideoQuality savedQuality =  videoQualityRepository.save(videoQuality);
             // 4. Saved entity ko wapas DTO mein convert karke return karein
             return  videoQualityMapper.toDto(savedQuality);
         } catch (RuntimeException e) {
             throw new RuntimeException(e);
         }
    }

    @Override
    public List<VideoQualityDto> getQualitiesByVideoId(String videoId) {
        if(videoId == null || videoId.isBlank()) throw  new RuntimeException("Video Id is required");
        return videoQualityRepository.findById(videoId)
                .stream()
                .map(videoQualityMapper::toDto)
                .toList();
    }

    @Override
    public void deleteQualitiesByVideoId(String videoId) {
        if(videoId == null || videoId.isBlank()) throw  new RuntimeException("Video Id is required");
        // 2. Delete Operation
        try {
            videoQualityRepository.deleteById(videoId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete video qualities for ID: " + videoId, e);
        }
    }
}
