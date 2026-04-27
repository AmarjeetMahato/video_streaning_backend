package com.youtube.domain.Videos.repository;


import com.youtube.domain.Videos.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;

@Repository
public interface VideoRepository  extends JpaRepository<Video,String> {

    // Pageable का उपयोग करके डेटा लाना
    Page<Video> findAll(Pageable pageable);
}
