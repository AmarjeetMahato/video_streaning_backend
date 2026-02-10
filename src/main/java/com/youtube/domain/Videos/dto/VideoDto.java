package com.youtube.domain.Videos.dto;


import com.youtube.enums.VideoStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class VideoDto {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    // --- Nullable/Optional Fields (Server-populated) ---
    // In par @NotBlank nahi lagega kyunki ye initial request mein null hongi

    @PositiveOrZero(message = "Duration must be positive")
    private Double duration;

    @PositiveOrZero(message = "Size must be positive")
    private Long totalSize;

    private String mimeType;

    private String thumbnailUrl;

    private String originalFilePath;

    private VideoStatus status;
}
