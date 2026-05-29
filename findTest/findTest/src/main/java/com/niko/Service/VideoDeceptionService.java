package com.niko.Service;

import com.niko.dto.VideoGenerateRequest;

import java.util.List;

public interface VideoDeceptionService {
    public void createDenseKeyframeVideo(String fakeImagePath, String realVideoPath, String outputPath) throws Exception;

    public void createPeriodicKeyframeVideo(String realVideoPath, String outputPath, int intervalSeconds) throws Exception ;

    void createPeriodicFakeKeyframeVideo(String fakeImg, String realVid, String output, int interval) throws Exception;

    void createPeriodicFakeKeyframeVideoNew(VideoGenerateRequest req) throws Exception;

    void createPeriodicFakeVideoKeyframeVideo(VideoGenerateRequest request) throws Exception;

    List<String> processVideoFolder(String fakePath, String folderPath, int intervalSeconds) throws Exception;

    void createBlendVideo(VideoGenerateRequest req) throws Exception;
}
