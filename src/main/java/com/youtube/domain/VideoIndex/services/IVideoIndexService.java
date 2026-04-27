package com.youtube.domain.VideoIndex.services;

import com.youtube.domain.VideoIndex.dto.VideoIndexDto;

public interface IVideoIndexService {

    VideoIndexDto createVideoIndex(VideoIndexDto videoIndexDto);

    VideoIndexDto fetchById(String videoId);


}
