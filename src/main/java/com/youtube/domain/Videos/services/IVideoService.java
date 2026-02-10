package com.youtube.domain.Videos.services;

import com.youtube.domain.Videos.dto.VideoDto;
import com.youtube.domain.Videos.entity.Video;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IVideoService {

    Video createVideo(VideoDto videoDto, MultipartFile file);

    Video fetchVideo(String videoId);

    List<Video> fetchAllVideos(int limit, int offset);

    void  deleteVideo(String videoId);
}
