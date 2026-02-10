package com.youtube.domain.Videos.controllers;
import com.youtube.domain.Videos.dto.VideoDto;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.domain.Videos.services.IVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
public class VideoController {

    final  private IVideoService videoService;

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck(){
        return ResponseEntity.status(HttpStatus.OK).body(
                Map.of("message","API Response working perfectly")
        );
    }


    @PostMapping(value = "/create", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> uploadVideo(
            @RequestPart("video") MultipartFile file, // 'file' key in Postman
            @Valid @ModelAttribute("videoData") VideoDto videoDto) { // 'videoData' key in Postman
        // Validate if file is empty before processing
        if(file.isEmpty()){
            return  ResponseEntity.badRequest().body("File is empty");
        }
        Video video = this.videoService.createVideo(videoDto, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }

    @GetMapping("/fetch_video/{id}")
    public ResponseEntity<?> fetchVideo(@Valid @PathVariable String id){
        Video video = this.videoService.fetchVideo(id);
        return ResponseEntity.status(HttpStatus.OK).body(video);
    }

    @GetMapping("/fetch_all_video")
    public ResponseEntity<?> fetchAllVideo(
            @Valid
            @RequestParam(defaultValue = "10") int limit ,
            @RequestParam(defaultValue = "0") int offset
    ){
        List<Video> videoEntityList = this.videoService.fetchAllVideos(limit,offset);
        return ResponseEntity.status(HttpStatus.OK).body(videoEntityList);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteVideo(@Valid @PathVariable String id){
        this.videoService.deleteVideo(id);
        return ResponseEntity.status(HttpStatus.OK).body("Video Delete Successfully !!");
    }
}
