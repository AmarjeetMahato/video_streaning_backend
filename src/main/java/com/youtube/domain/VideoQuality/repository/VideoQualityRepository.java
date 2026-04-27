package com.youtube.domain.VideoQuality.repository;

import com.youtube.domain.VideoQuality.entity.VideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoQualityRepository extends JpaRepository<VideoQuality,String> {

}
