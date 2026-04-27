package com.youtube.domain.VideoQuality.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/video_quality")
@RequiredArgsConstructor
@RestController
public class VideoQualityControllers {


    @PostMapping("/create")
    public ResponseEntity<?> createVideoQuality(){

    }
}
