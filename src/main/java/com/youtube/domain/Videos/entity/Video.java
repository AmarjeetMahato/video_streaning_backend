package com.youtube.domain.Videos.entity;


import com.youtube.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos", indexes = {
        // 1. Title Search Index (Aapne jo banaya tha)
        @Index(name = "title_idx", columnList = "title"),
        // 2. Status Filter Index (Bahut zaroori hai "processing" ya "ready" videos fetch karne ke liye)
        @Index(name = "status_idx", columnList = "status"),
        // 3. Sorting Index (Latest videos dikhane ke liye - CreatedAt par index)
        @Index(name = "created_at_idx", columnList = "created_at"),
        // 4. Composite Index (Agar aap status aur title dono se search karte hain)
        @Index(name = "status_title_idx", columnList = "status, title")
})
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Video {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private String id; // UUID ko String ya java.util.UUID dono mein rakh sakte hain

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Video Metadata
    private Double duration; // doublePrecision ke liye Double best hai

    @Column(name = "total_size")
    private Long totalSize; // BigInt mode: bigint -> Long

    @Column(name = "mime_type", length = 50)
    private String mimeType = "video/mp4";

    // Media Paths
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "original_file_path", columnDefinition = "TEXT")
    private String originalFilePath;

    // Status Enum
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VideoStatus status = VideoStatus.PENDING;

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
