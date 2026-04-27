package com.youtube.domain.VideoIndex.entity;

import com.youtube.domain.Videos.entity.Video;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class VideoIndex {

    @Id
    @Column( unique = true, nullable = false, updatable = false)
    private  String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Column(columnDefinition = "TEXT")
    private String spriteSheetUrl; // Preview images grid link

    @Column(columnDefinition = "TEXT")
    private String masterPlaylistUrl; // Main index file link

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
