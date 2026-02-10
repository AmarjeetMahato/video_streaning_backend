package com.youtube.domain.Videos.repository;


import com.youtube.domain.Videos.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository  extends JpaRepository<Video,String> {
}
