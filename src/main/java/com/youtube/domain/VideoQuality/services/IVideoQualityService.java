package com.youtube.domain.VideoQuality.services;

import com.youtube.domain.VideoQuality.dto.VideoQualityDto;
import com.youtube.domain.VideoQuality.entity.VideoQuality;

import java.util.List;

public interface IVideoQualityService {

    // प्रोसेसिंग के बाद डेटाबेस में क्वालिटी लिंक सेव करने के लिए
    VideoQualityDto createVideoQuality(VideoQualityDto videoQualityDto);

    // किसी एक वीडियो की सभी उपलब्ध क्वालिटीज प्राप्त करने के लिए
    List<VideoQualityDto> getQualitiesByVideoId(String videoId);

    // अगर वीडियो डिलीट होता है तो उसकी क्वालिटीज हटाने के लिए
    void deleteQualitiesByVideoId(String videoId);
}
