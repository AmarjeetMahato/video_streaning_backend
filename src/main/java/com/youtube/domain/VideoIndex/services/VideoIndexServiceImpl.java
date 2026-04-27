package com.youtube.domain.VideoIndex.services;

import com.youtube.domain.VideoIndex.dto.VideoIndexDto;
import com.youtube.domain.VideoIndex.entity.VideoIndex;
import com.youtube.domain.VideoIndex.mapper.VideoIndexMapper;
import com.youtube.domain.VideoIndex.repository.VideoIndexRepository;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.domain.Videos.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoIndexServiceImpl implements  IVideoIndexService {

    private VideoIndexRepository videoIndexRepository;
    private VideoIndexMapper videoIndexMapper;
    private VideoRepository videoRepository;

    @Override
    public VideoIndexDto createVideoIndex(VideoIndexDto videoIndexDto) {
            if(videoIndexDto == null) throw new RuntimeException("Invalid fields value");

            try{
                Video video = videoRepository.findById(videoIndexDto.getId())
                        .orElseThrow(() -> new RuntimeException("Not Found Video"));

                  VideoIndex  videoIndex = videoIndexMapper.toEntity(videoIndexDto,video);
                  VideoIndex index = videoIndexRepository.save(videoIndex);
                  return  videoIndexMapper.toDto(index);
            }catch (Exception e){
                throw new RuntimeException(e.toString());
            }
    }

    @Override
    public VideoIndexDto fetchById(String videoId) {
        return null;
    }
}
