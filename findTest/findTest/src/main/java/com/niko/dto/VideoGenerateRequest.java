package com.niko.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateRequest {
    /** 风景视频路径（用于封面和进度条） */
    private String fakeVideoPath;
    /** 真实视频路径 */
    private String realVideoPath;
    /** 输出视频路径 */
    private String outputPath;
    /** 进度条风景帧插入间隔(秒)，建议 >= 5，默认 10 */
    private Integer intervalSeconds;
}