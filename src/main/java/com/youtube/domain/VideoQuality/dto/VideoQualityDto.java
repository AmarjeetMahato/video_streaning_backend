package com.youtube.domain.VideoQuality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoQualityDto {

    private String id;

    @NotBlank(message = "Video ID is required")
    private String videoId;

    @NotBlank(message = "Quality cannot be blank")
    @Pattern(regexp = "^(144p|240p|360p|480p|720p|1080p|1440p|2160p)$",
            message = "Invalid quality format (e.g., 1080p, 720p)")
    private String quality;

    @NotBlank(message = "URL is required")
    @URL(message = "Invalid Cloudinary URL format")
    private String url;

    @NotBlank(message = "Format is required")
    @Pattern(regexp = "^(HLS|MP4|DASH)$", message = "Format must be HLS, MP4 or DASH")
    private String format;

    // Output fields (sirf response mein dikhane ke liye)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
