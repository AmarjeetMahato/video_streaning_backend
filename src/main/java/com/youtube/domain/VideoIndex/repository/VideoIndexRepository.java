package com.youtube.domain.VideoIndex.repository;

import com.youtube.domain.VideoIndex.entity.VideoIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoIndexRepository extends JpaRepository<VideoIndex,String> {
}
