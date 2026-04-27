package com.youtube.domain.VideoQuality.mapper;

import com.youtube.domain.VideoQuality.dto.VideoQualityDto;
import com.youtube.domain.VideoQuality.entity.VideoQuality;
import com.youtube.domain.Videos.entity.Video;

public class VideoQualityMapper {

    // DTO → Entity
    public VideoQuality toEntity(VideoQualityDto dto, Video video) {

        if (dto == null) return null;

        return VideoQuality.builder()
                .id(dto.getId()) // optional (null for create)
                .video(video)    // 👈 important (relation mapping)
                .quality(dto.getQuality())
                .url(dto.getUrl())
                .format(dto.getFormat())
                .build();
    }

    // Entity → DTO
    public VideoQualityDto toDto(VideoQuality entity) {

        if (entity == null) return null;

        VideoQualityDto dto = new VideoQualityDto();

        dto.setId(entity.getId());
        dto.setVideoId(
                entity.getVideo() != null ? entity.getVideo().getId() : null
        );
        dto.setQuality(entity.getQuality());
        dto.setUrl(entity.getUrl());
        dto.setFormat(entity.getFormat());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

}
