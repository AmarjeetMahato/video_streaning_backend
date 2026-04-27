package com.youtube.domain.VideoQuality.controllers;


import com.youtube.domain.VideoQuality.dto.VideoQualityDto;
import com.youtube.domain.VideoQuality.services.IVideoQualityService;
import com.youtube.domain.VideoQuality.services.VideoQualityServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/video_quality")
@RequiredArgsConstructor
@RestController
public class VideoQualityControllers {

    private IVideoQualityService videoQualityService;

    @PostMapping("/create")
    public ResponseEntity<?> createVideoQuality(@RequestBody VideoQualityDto videoQualityDto){
              VideoQualityDto video = videoQualityService.createVideoQuality(videoQualityDto);
              return  ResponseEntity.status(HttpStatus.CREATED).body(video);
    }

    @GetMapping("/get_video/{videoId}")
    public ResponseEntity<?> getVideoQuality(@RequestParam String videoId){
        List<VideoQualityDto> video = videoQualityService.getQualitiesByVideoId(videoId);
        return  ResponseEntity.status(HttpStatus.OK).body(video);
    }

    @DeleteMapping("/delete/{videoId}")
    public ResponseEntity<?> deleteVideoQuality(@RequestParam String videoId){
        videoQualityService.deleteQualitiesByVideoId(videoId);
        return  ResponseEntity.status(HttpStatus.OK).body("video deleted successfully");
    }

}
