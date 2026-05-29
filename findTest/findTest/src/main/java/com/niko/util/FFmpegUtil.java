package com.niko.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Slf4j
@Component
public class FFmpegUtil {
    /**
     * 执行命令并返回标准输出（用来获取 ffprobe 的视频信息）
     */
    public String executeAndGetOutput(String command) throws Exception {
        log.info("执行命令: {}", command);
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        process.waitFor();
        return output.toString();
    }

    /**
     * 仅执行命令（用来执行实际的 FFmpeg 视频处理）
     */
    public void executeCommand(String command) throws Exception {
        log.info("开始执行 FFmpeg 处理命令...");
        ProcessBuilder builder = new ProcessBuilder("cmd", "/c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        // 实时打印 FFmpeg 的处理日志
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg] {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 执行失败，退出码：" + exitCode);
        }
        log.info("FFmpeg 命令执行成功！");
    }

    /**
     * 以数组方式安全执行 FFmpeg 命令（彻底解决转义问题）
     */
    public void executeCommand(List<String> commandArgs) throws Exception {
        log.info("开始执行 FFmpeg 处理命令...");
        ProcessBuilder builder = new ProcessBuilder(commandArgs);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFmpeg] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 执行失败，退出码：" + exitCode);
        }
        log.info("FFmpeg 命令执行成功！");
    }


}

