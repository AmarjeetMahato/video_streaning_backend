package com.youtube.domain.videoProcessingJob.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VideoProcessingJobDto {

    private String id;

    @NotBlank(message = "Video ID is required and cannot be empty")
    private String videoId;

    @NotBlank(message = "Job status is required")
    @Pattern(
            regexp = "QUEUED|PROCESSING|COMPLETED|FAILED",
            message = "Invalid status. Allowed values: QUEUED, PROCESSING, COMPLETED, FAILED"
    )
    private String status;

    @Min(value = 0, message = "Retry count cannot be less than 0")
    @Max(value = 10, message = "Retry count cannot be more than 10")
    private int retryCount;

    @Size(max = 1000, message = "Error message cannot exceed 1000 characters")
    private String errorMessage;

    @Min(value = 0, message = "Progress cannot be less than 0%")
    @Max(value = 100, message = "Progress cannot be more than 100%")
    private int progress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
