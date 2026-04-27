package com.youtube.domain.videoProcessingJob.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "video_processing_jobs",
        indexes = {
                @Index(name = "idx_video_id", columnList = "videoId"),
                @Index(name = "idx_status", columnList = "status")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VideoProcessingJob {

    @Id
    @Column(nullable = false, updatable = false, unique = true)
    private  String id;

    // 🔗 Video reference
    @NotBlank(message = "VideoId cannot be blank")
    @Column(nullable = false)
    private String videoId;

    // 📌 Job status
    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status;
    // QUEUED, PROCESSING, COMPLETED, FAILED

    // 🔁 Retry tracking
    @Min(value = 0, message = "Retry count cannot be negative")
    @Column(nullable = false)
    private int retryCount;

    // ⚠️ Error tracking
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // 📊 optional progress (0–100)
    @Min(value = 0)
    @Column(nullable = false)
    private int progress;

    // 🕒 timestamps
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 🆔 Auto ID generation
    @PrePersist
    public void ensureId() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }

        if (status == null) {
            status = "QUEUED";
        }

        retryCount = 0;
        progress = 0;
    }
}
