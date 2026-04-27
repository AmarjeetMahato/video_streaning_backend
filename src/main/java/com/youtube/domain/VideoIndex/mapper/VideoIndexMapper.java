package com.youtube.domain.VideoIndex.mapper;


import com.youtube.domain.VideoIndex.dto.VideoIndexDto;
import com.youtube.domain.VideoIndex.entity.VideoIndex;
import com.youtube.domain.Videos.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoIndexMapper {

    // DTO → Entity
    public VideoIndex toEntity(VideoIndexDto dto, Video video) {

        if (dto == null) return null;

        return VideoIndex.builder()
                .id(dto.getId()) // optional (null for create)
                .video(video)    // 👈 important relation mapping
                .masterPlaylistUrl(dto.getMasterPlaylistUrl())
                .spriteSheetUrl(dto.getSpriteSheetUrl())
                .build();
    }

    // Entity → DTO
    public VideoIndexDto toDto(VideoIndex entity) {

        if (entity == null) return null;

        return VideoIndexDto.builder()
                .id(entity.getId())
                .videoId(
                        entity.getVideo() != null
                                ? entity.getVideo().getId()
                                : null
                )
                .masterPlaylistUrl(entity.getMasterPlaylistUrl())
                .spriteSheetUrl(entity.getSpriteSheetUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Update existing entity (IMPORTANT for PUT/PATCH)
    public void updateEntity(VideoIndex existing, VideoIndexDto dto) {

        if (dto == null || existing == null) return;

        existing.setMasterPlaylistUrl(dto.getMasterPlaylistUrl());
        existing.setSpriteSheetUrl(dto.getSpriteSheetUrl());
    }
}