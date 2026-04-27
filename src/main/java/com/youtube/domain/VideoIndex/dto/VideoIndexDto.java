package com.youtube.domain.VideoIndex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoIndexDto {

    private String id;

    @NotBlank(message = "Video ID is required")
    private String videoId;

    @NotBlank(message = "Master playlist URL is required")
    @URL(message = "Invalid Master Playlist URL")
    private String masterPlaylistUrl; // HLS main .m3u8 link

    @URL(message = "Invalid Sprite Sheet URL")
    private String spriteSheetUrl; // Preview thumbnails grid link

    // Response fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}