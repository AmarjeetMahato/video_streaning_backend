package com.youtube.domain.Videos.mapper;


import com.youtube.domain.Videos.dto.VideoDto;
import com.youtube.domain.Videos.entity.Video;
import com.youtube.enums.VideoStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class VideoMapper {

    public Video toEntity(VideoDto dto, MultipartFile file){

        return  Video.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .totalSize(file.getSize())
                .mimeType(file.getContentType())
                // Shuruat mein hum temp path dete hain, baad mein storage path se update karenge
                .thumbnailUrl("temp/" + file.getOriginalFilename())
                .status(VideoStatus.PENDING)
                // Inhe hum null ya 0.0 rakhte hain, service layer baad mein update karegi
                .duration(0.0)
                .originalFilePath(null)
                .build();
    }
}
