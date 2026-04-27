package com.youtube.domain.VideoQuality.entity;


import com.youtube.domain.Videos.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_quality",indexes = {
        @Index(name = "quality_idx", columnList = "quality"),
        @Index(name = "url_idx", columnList = "url"),
        @Index(name = "format_idx",columnList = "format")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VideoQuality {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    private String quality; // 1080p, 720p

    @Column(columnDefinition = "TEXT")
    private String url; // Cloudinary M3U8 link

    private String format; // HLS

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // Helper to generate ID if not using DB default
    @PrePersist
    public void ensureId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
