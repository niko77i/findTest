package com.niko.Service.impl;

import com.niko.Service.VideoDeceptionService;
import com.niko.dto.VideoGenerateRequest;
import com.niko.util.FFmpegUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class VideoDeceptionServiceImpl implements VideoDeceptionService {

    @Autowired
    private FFmpegUtil ffmpegUtil;

    /**
     * 全片密集关键帧注入法（画面交替替换）
     *
     * @param fakeImagePath 伪装图片绝对路径 (如 D:/videos/fake.jpg)
     * @param realVideoPath 真实视频绝对路径 (如 D:/videos/real.mp4)
     * @param outputPath    输出视频绝对路径 (如 D:/videos/output.mp4)
     */
    @Override
    public void createDenseKeyframeVideo(String fakeImagePath, String realVideoPath, String outputPath) throws Exception {
        // 1. 先通过 ffprobe 获取真实视频的 宽、高、帧率
        String probeCommand = String.format("ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate -of default=noprint_wrappers=1 \"%s\"", realVideoPath);
        String probeResult = ffmpegUtil.executeAndGetOutput(probeCommand);

        // 使用正则提取数据
        int width = extractValue(probeResult, "width=(\\d+)");
        int height = extractValue(probeResult, "height=(\\d+)");
        String fpsStr = extractString(probeResult, "r_frame_rate=(\\d+/\\d+)");

        log.info("获取到真实视频参数 -> 宽:{}, 高:{}, 帧率:{}", width, height, fpsStr);

        // 2. 动态拼接 FFmpeg 命令
        // 将伪装图片缩放到真实视频的宽高，并匹配真实视频的帧率，最后交替录制并强制全I帧
        String command = String.format(
                "ffmpeg -y -loop 1 -i \"%s\" -i \"%s\" -filter_complex \"[0:v]scale=%d:%d,setsar=1,fps=%s[img];[1:v]fps=%s[vid];[img][vid]interleave[v]\" -map \"[v]\" -map 1:a? -c:v libx264 -c:a copy -pix_fmt yuv420p -g 1 -keyint_min 1 -x264-params keyint=1:min-keyint=1:scenecut=0 \"%s\"",
                fakeImagePath, realVideoPath, width, height, fpsStr, fpsStr, outputPath
        );

        ffmpegUtil.executeCommand(command);
    }

    /**
     * 进阶版：周期性强制插入关键帧（轻量级，体积正常）
     * @param realVideoPath 真实视频绝对路径
     * @param outputPath 输出视频绝对路径
     * @param intervalSeconds 强制插入关键帧的间隔（单位：秒，建议设为 1 或 2）
     */
    public void createPeriodicKeyframeVideo(String realVideoPath, String outputPath, int intervalSeconds) throws Exception {
        // 1. 先通过 ffprobe 获取真实视频的帧率（为了重新编码时保持原帧率）
        String probeCommand = String.format("ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate -of default=noprint_wrappers=1 \"%s\"", realVideoPath);
        String probeResult = ffmpegUtil.executeAndGetOutput(probeCommand);

        // 提取帧率（例如 "30/1"）
        String fpsStr = extractString(probeResult, "r_frame_rate=(\\d+/\\d+)");
        log.info("获取到真实视频帧率: {}", fpsStr);

        // 2. 动态拼接 FFmpeg 命令
        // 核心：-force_key_frames "expr:gte(t,n_forced*N)" 表示每隔 N 秒强制插入一个关键帧
        String command = String.format(
                "ffmpeg -y -i \"%s\" -c:v libx264 -r %s -force_key_frames \"expr:gte(t,n_forced*%d)\" -c:a copy -pix_fmt yuv420p \"%s\"",
                realVideoPath, fpsStr, intervalSeconds, outputPath
        );

        ffmpegUtil.executeCommand(command);
    }

    /**
     * 终极版：周期性插入伪装图，并强制设为关键帧
     * @param fakeImagePath 伪装图片绝对路径
     * @param realVideoPath 真实视频绝对路径
     * @param outputPath 输出视频绝对路径
     * @param intervalSeconds 插入伪装关键帧的间隔（单位：秒）
     */
    public void createPeriodicFakeKeyframeVideo(String fakeImagePath, String realVideoPath, String outputPath, int intervalSeconds) throws Exception {
        // 1. 获取原视频的分辨率
        String probeCommand = String.format("ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of default=noprint_wrappers=1 \"%s\"", realVideoPath);
        String probeResult = ffmpegUtil.executeAndGetOutput(probeCommand);

        // 提取宽高（这里提取出来的通常是 String）
        String width = String.valueOf(extractValue(probeResult, "width=(\\d+)"));
        String height = String.valueOf(extractValue(probeResult, "height=(\\d+)"));

        // 2. 核心逻辑：把伪装图做成一个每隔 N 秒闪现一次的视频流，然后覆盖在原视频上
        // 注意：这里把宽度和高度的占位符改成了 %s，避免类型转换报错
        String command = String.format(
                "ffmpeg -y -loop 1 -i \"%s\" -i \"%s\" -filter_complex \"[0:v]scale=%s:%s,colorchannelmixer=aa=0.001[fg];[1:v][fg]overlay=enable='between(mod(t\\,%d)\\,0\\,0.05)':shortest=1[v]\" -map \"[v]\" -map 1:a? -c:v libx264 -preset ultrafast -force_key_frames \"expr:gte(t,n_forced*%d)\" -c:a copy -pix_fmt yuv420p \"%s\"",
                fakeImagePath, realVideoPath, width, height, intervalSeconds, intervalSeconds, outputPath
        );

        ffmpegUtil.executeCommand(command);
    }

    // 辅助方法：提取数字
    private int extractValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        throw new RuntimeException("无法从 ffprobe 结果中提取参数: " + regex);
    }

    // 辅助方法：提取字符串
    private String extractString(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) return matcher.group(1);
        throw new RuntimeException("无法从 ffprobe 结果中提取参数: " + regex);
    }

    /**
     * 终极版：周期性插入伪装图，并强制设为关键帧（适配实体类）
     */
    public void createPeriodicFakeKeyframeVideoNew(VideoGenerateRequest req) throws Exception {
        String fakeImagePath = req.getFakeVideoPath();
        String realVideoPath = req.getRealVideoPath();
        String outputPath = req.getOutputPath();
        int intervalSeconds = req.getIntervalSeconds();

        // 1. 获取原视频的分辨率和时长
        String probeCmd = String.format(
                "ffprobe -v error -select_streams v:0 -show_entries stream=width,height -show_entries format=duration -of default=noprint_wrappers=1 \"%s\"",
                realVideoPath
        );
        String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);

        int width = extractIntValue(probeResult, "width=(\\d+)");
        int height = extractIntValue(probeResult, "height=(\\d+)");
        double duration = extractDoubleValue(probeResult, "duration=([\\d.]+)");

        // 2. 构建安全的参数列表
        List<String> args = new ArrayList<>();
        args.add("ffmpeg");
        args.add("-y");

        // 输入0: 伪装图（循环读取）
        args.add("-loop"); args.add("1");
        args.add("-t"); args.add(String.valueOf(duration));
        args.add("-i"); args.add(fakeImagePath);

        // 输入1: 真实视频
        args.add("-i"); args.add(realVideoPath);

        // 3. 核心滤镜：每隔N秒的前0.04秒内替换画面
        String filterComplex = String.format(
                "[0:v]scale=%d:%d,setsar=1,fps=25[fake];" +
                        "[1:v]fps=25[real];" +
                        "[real][fake]overlay=enable='lt(mod(t,%d),0.04)':shortest=1[outv]",
                width, height, intervalSeconds
        );

        args.add("-filter_complex"); args.add(filterComplex);
        args.add("-map"); args.add("[outv]");
        args.add("-map"); args.add("1:a?");

        // 4. 编码参数优化
        args.add("-c:v"); args.add("libx264");
        args.add("-preset"); args.add("medium");
        args.add("-crf"); args.add("18");
        args.add("-pix_fmt"); args.add("yuv420p");
        args.add("-force_key_frames");
        args.add(String.format("expr:lt(mod(t,%d),0.04)", intervalSeconds));
        args.add("-sc_threshold"); args.add("0");
        args.add("-c:a"); args.add("copy");
        args.add(outputPath);

        // 5. 执行命令
        System.out.println("开始生成进度条欺骗视频，间隔:" + intervalSeconds + "s, 分辨率:" + width + "x" + height);
        ffmpegUtil.executeCommand(args);
        System.out.println("视频生成成功: " + outputPath);
    }
   /* @Override
    public void createPeriodicFakeVideoKeyframeVideo(VideoGenerateRequest req) throws Exception {
        String fakePath = req.getFakeVideoPath();
        String realPath = req.getRealVideoPath();
        String outputPath = req.getOutputPath();
        int intervalSeconds = (req.getIntervalSeconds() != null && req.getIntervalSeconds() > 0)
                ? req.getIntervalSeconds() : 8;

        // ========== 1. 使用纯英文临时目录，彻底规避 Windows + JDK8 中文路径 -22 问题 ==========
        Path tempDir = Files.createTempDirectory("ffmpeg_deception_");
        String tmpFake = tempDir.resolve("fake.mp4").toAbsolutePath().toString();
        String tmpReal = tempDir.resolve("real.mp4").toAbsolutePath().toString();

        Files.copy(Paths.get(fakePath), Paths.get(tmpFake), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(realPath), Paths.get(tmpReal), StandardCopyOption.REPLACE_EXISTING);

        try {
            // ========== 2. 探测真实视频元数据 ==========
            String probeCmd = String.format(
                    "ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate,duration -of default=noprint_wrappers=1 \"%s\"",
                    tmpReal
            );
            String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);

            int width = extractIntValue(probeResult, "width=(\\d+)");
            int height = extractIntValue(probeResult, "height=(\\d+)");
            String fpsStr = extractStringValue(probeResult, "r_frame_rate=(\\d+/\\d+)");

            // 帧率解析失败兜底
            if (fpsStr == null || fpsStr.isEmpty() || "N/A".equals(fpsStr)) {
                log.warn("⚠️ 未能正确解析真实视频帧率，默认使用 30/1");
                fpsStr = "30/1";
            }

            // ========== 3. 探测风景视频时长，动态生成 trim 参数（防御短视频 -22）==========
            String fakeProbeCmd = String.format(
                    "ffprobe -v error -select_streams v:0 -show_entries stream=duration -of default=noprint_wrappers=1 \"%s\"",
                    tmpFake
            );
            String fakeProbeResult = ffmpegUtil.executeAndGetOutput(fakeProbeCmd);
            double fakeDuration = Double.parseDouble(extractStringValue(fakeProbeResult, "duration=([\\d.]+)").replace("N/A", "0"));

            // 封面截取：不足1秒则按实际时长截取
            String coverTrim = fakeDuration >= 1.0 ? "trim=0:1" : "trim=0:" + fakeDuration;
            // 进度条假帧：不足0.04秒则按实际时长截取
            String fakeTrim = fakeDuration >= 0.04 ? "trim=0:0.04" : "trim=0:" + fakeDuration;

            log.info("📊 真实视频: {}x{} @ {} | 风景视频时长: {}s | coverTrim: {} | fakeTrim: {}",
                    width, height, fpsStr, fakeDuration, coverTrim, fakeTrim);

            // ========== 4. 构建滤镜图（✅ 已用 format=yuv420p 替换错误的 pix_fmt=yuv420p）==========
            String filter = String.format(
                    "[0:v]fps=%s,scale=%d:%d,setsar=1,format=yuv420p,%s,setpts=PTS-STARTPTS[cover];" +
                            "[1:v]fps=%s,scale=%d:%d,setsar=1,format=yuv420p[vreal];" +
                            "[0:v]fps=%s,scale=%d:%d,setsar=1,format=yuv420p,%s,setpts=PTS-STARTPTS[fake];" +
                            "[vreal][fake]concat=n=2:v=1:a=0[body];" +
                            "[cover][body]concat=n=2:v=1:a=0[outv]",
                    fpsStr, width, height, coverTrim,
                    fpsStr, width, height,
                    fpsStr, width, height, fakeTrim
            );

            // ========== 5. 执行 FFmpeg（必须使用 List 传参，禁止 Runtime.exec(String)）==========
            List<String> command = Arrays.asList(
                    "ffmpeg", "-y",
                    "-stream_loop", "-1", "-i", tmpFake,
                    "-i", tmpReal,
                    "-filter_complex", filter,
                    "-map", "[outv]",
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-crf", "28",
                    "-pix_fmt", "yuv420p",   // ✅ 全局编码参数保留，确保输出兼容性
                    "-movflags", "+faststart",
                    outputPath
            );

            ffmpegUtil.executeCommand(command);
            log.info("✅ 欺骗视频生成成功: {}", outputPath);
//            String coverOutputPath = Paths.get(req.getOutputPath()).getParent()
//                    .resolve("cover.jpg")
//                    .toAbsolutePath().toString();
//            extractYoutubeThumbnail(tmpFake, coverOutputPath);

        } finally {
            // ========== 6. 清理临时文件 ==========
            try {
                Files.deleteIfExists(tempDir.resolve("fake.mp4"));
                Files.deleteIfExists(tempDir.resolve("real.mp4"));
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                log.warn("⚠️ 临时文件清理失败: {}", e.getMessage());
            }
        }
    }*/



    /**
     * 风景视频作为进度条
     * @param req
     * @throws Exception
     */
   @Override
   public void createPeriodicFakeVideoKeyframeVideo(VideoGenerateRequest req) throws Exception {
       String realVideoPath = req.getRealVideoPath();
       String outputPath = req.getOutputPath();
       int intervalSeconds = req.getIntervalSeconds();

       // 1. 准备临时抽帧目录
       Path tempDir = Files.createTempDirectory("scenery_frames_");
       // 抽帧图片的命名格式：frame_0001.png, frame_0002.png...
       String framePattern = tempDir.resolve("frame_%04d.png").toAbsolutePath().toString();

       try {
           // 2. 【抽帧阶段】将风景视频抽帧为图片序列
           log.info("正在将风景视频拆解为图片序列...");
           List<String> extractCmd = Arrays.asList(
                   "ffmpeg", "-y",
                   "-i", req.getFakeVideoPath(),
                   "-vsync", "vfr", // 保持原视频帧率，不丢帧
                   framePattern
           );
           ffmpegUtil.executeCommand(extractCmd);

           // 3. 获取真实视频的分辨率和时长
           String probeCmd = String.format(
                   "ffprobe -v error -select_streams v:0 -show_entries stream=width,height -show_entries format=duration -of default=noprint_wrappers=1 \"%s\"",
                   realVideoPath
           );
           String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);
           int width = extractIntValue(probeResult, "width=(\\d+)");
           int height = extractIntValue(probeResult, "height=(\\d+)");
           double duration = extractDoubleValue(probeResult, "duration=([\\d.]+)");

           // 4. 【生成阶段】完全套用你成功的 0.04秒 逻辑
           List<String> args = new ArrayList<>();
           args.add("ffmpeg");
           args.add("-y");

           // 输入0: 循环读取抽帧后的风景图片序列 (framerate 设为 25，保证插入时的画面流畅)
           args.add("-framerate"); args.add("25");
           args.add("-loop"); args.add("1"); // 无限循环读取图片
           args.add("-i"); args.add(framePattern);

           // 输入1: 真实视频
           args.add("-i"); args.add(realVideoPath);

           // 核心滤镜：完全复刻之前的逻辑！
           // 每隔 intervalSeconds 秒，前 0.04 秒显示风景图片(fake)，其余时间显示真实视频(real)
           String filterComplex = String.format(
                   "[0:v]scale=%d:%d,setsar=1,fps=25[fake];" +
                           "[1:v]fps=25[real];" +
                           "[real][fake]overlay=enable='lt(mod(t,%d),0.04)':shortest=1[outv]",
                   width, height, intervalSeconds
           );

           args.add("-filter_complex"); args.add(filterComplex);
           args.add("-map"); args.add("[outv]");
           args.add("-map"); args.add("1:a?"); // 保留真实视频的音频

           // 编码参数：完全沿用你成功的配置
           args.add("-c:v"); args.add("libx264");
           args.add("-preset"); args.add("medium");
           args.add("-crf"); args.add("18");
           args.add("-pix_fmt"); args.add("yuv420p");

           // 强制在插入风景图片的那 0.04 秒生成 I 帧（关键帧）
           args.add("-force_key_frames");
           args.add(String.format("expr:lt(mod(t,%d),0.04)", intervalSeconds));
           args.add("-sc_threshold"); args.add("0"); // 禁用场景切换检测

           args.add("-c:a"); args.add("copy"); // 音频直接复制
           args.add(outputPath);

           log.info("开始合成最终视频...");
           ffmpegUtil.executeCommand(args);
           log.info("✅ 视频生成成功: {}", outputPath);

       } finally {
           // 5. 【清理阶段】删除临时抽帧文件夹，防止磁盘爆满
           deleteDirectory(tempDir.toFile());
       }

   }

    /**
     * Temporal Continuity 实验版
     *
     * 核心思想：
     *
     * 1. fake 不直接显示
     * 2. fake 低权重进入编码链
     * 3. fake 通过时间传播扩散
     * 4. 避免双重画面
     * 5. 保持 motion continuity
     * 6. 避免 R/F 硬切
     *
     * 效果：
     *
     * - 本地轻微 temporal glitch
     * - 不会明显双层画面
     * - 有轻微时间扰动
     * - 编码连续性更强
     */
    public void createTemporalContinuityVideo(
            VideoGenerateRequest req
    ) throws Exception {

        String realVideo =
                req.getRealVideoPath();

        String fakeVideo =
                req.getFakeVideoPath();

        String output =
                req.getOutputPath();

        // =====================================================
        // 1. probe 视频信息
        // =====================================================

        String probeCmd =
                String.format(
                        "ffprobe -v error " +
                                "-select_streams v:0 " +
                                "-show_entries stream=width,height,r_frame_rate " +
                                "-of default=noprint_wrappers=1 \"%s\"",
                        realVideo
                );

        String probeResult =
                ffmpegUtil.executeAndGetOutput(
                        probeCmd
                );

        int width =
                extractIntValue(
                        probeResult,
                        "width=(\\d+)"
                );

        int height =
                extractIntValue(
                        probeResult,
                        "height=(\\d+)"
                );

        String fpsString =
                extractStringValue(
                        probeResult,
                        "r_frame_rate=([0-9]+/[0-9]+)"
                );

        double fps =
                parseFps(fpsString);

        log.info(
                "width={} height={} fps={}",
                width,
                height,
                fps
        );

        // =====================================================
        // 2. GOP
        // =====================================================

        // 5秒一个GOP
        int gop =
                (int) (fps * 5);

        // =====================================================
        // 3. ffmpeg args
        // =====================================================

        List<String> args =
                new ArrayList<>();

        args.add("ffmpeg");

        args.add("-y");

        // =====================================================
        // 输入
        // =====================================================

        args.add("-i");
        args.add(realVideo);

        args.add("-i");
        args.add(fakeVideo);

        // =====================================================
        // filter_complex
        // =====================================================

        /*
         * 非常关键：
         *
         * 不再使用：
         *      overlay
         *      高频frame切换
         *      强blend
         *
         * 而是：
         *      极弱fake注入
         *      temporal propagation
         *      时间域连续扰动
         */

        String filterComplex =
                String.format(

                        // =========================================
                        // real
                        // =========================================

                        "[0:v]" +
                                "fps=%f," +
                                "scale=%d:%d," +
                                "setsar=1" +
                                "[r];" +

                                // =========================================
                                // fake
                                // =========================================

                                "[1:v]" +
                                "fps=%f," +
                                "scale=%d:%d," +
                                "setsar=1" +
                                "[f];" +

                                // =========================================
                                // 极弱fake注入
                                // =========================================

                                /*
                                 * 99.2% real
                                 * 0.8% fake
                                 *
                                 * 人眼几乎不可见
                                 */

                                "[r][f]" +
                                "blend=all_expr='0.992*A+0.008*B'" +
                                "[inject];" +

                                // =========================================
                                // 时间传播
                                // =========================================

                                /*
                                 * 当前帧 + 上一帧
                                 * temporal continuity
                                 */

                                "[inject]" +
                                "tblend=all_mode=average" +
                                "[tblend1];" +

                                // =========================================
                                // temporal spread
                                // =========================================

                                /*
                                 * fake influence 扩散
                                 */

                                "[tblend1]" +
                                "tmix=frames=4:weights='1 2 2 1'" +
                                "[tmix1];" +

                                // =========================================
                                // 轻微时间模糊
                                // =========================================

                                /*
                                 * 避免局部fake突然可见
                                 */

                                "[tmix1]" +
                                "gblur=sigma=0.3" +
                                "[outv]",

                        fps,
                        width,
                        height,

                        fps,
                        width,
                        height
                );

        args.add("-filter_complex");

        args.add(filterComplex);

        // =====================================================
        // 输出映射
        // =====================================================

        args.add("-map");
        args.add("[outv]");

        args.add("-map");
        args.add("0:a?");

        // =====================================================
        // 编码
        // =====================================================

        args.add("-c:v");
        args.add("libx264");

        // 编码速度
        args.add("-preset");
        args.add("slow");

        // 高质量
        args.add("-crf");
        args.add("18");

        // =========================================
        // 禁用 B Frame
        // =========================================

        /*
         * 避免fake被B Frame稀释
         */

        args.add("-bf");
        args.add("0");

        // =========================================
        // GOP
        // =========================================

        args.add("-g");
        args.add(String.valueOf(gop));

        args.add("-keyint_min");
        args.add(String.valueOf(gop));

        // =========================================
        // 固定关键帧结构
        // =========================================

        args.add("-sc_threshold");
        args.add("0");

        // =========================================
        // 像素格式
        // =========================================

        args.add("-pix_fmt");
        args.add("yuv420p");

        // =========================================
        // 音频复制
        // =========================================

        args.add("-c:a");
        args.add("copy");

        // 输出
        args.add(output);

        // =====================================================
        // 执行
        // =====================================================

        log.info(
                "开始 temporal continuity experiment..."
        );

        ffmpegUtil.executeCommand(args);

        log.info(
                "实验视频生成完成: {}",
                output
        );
    }

    /**
     * 实验性高频混合视频
     *
     * 效果：
     * 1. 本地播放会有轻微爆闪感
     * 2. 主体仍然是真实视频
     * 3. 风景视频持续参与编码
     * 4. 保持原视频 fps
     * 5. 固定 GOP
     * 6. 禁用 B Frame
     *
     * 注意：
     * 这是视频编码实验代码，
     * 用于研究：
     * - blend
     * - frame alternation
     * - GOP
     * - glitch 风格
     */
    public void createExperimentalBlendVideo(VideoGenerateRequest req) throws Exception {

        String realVideo = req.getRealVideoPath();

        String fakeVideo = req.getFakeVideoPath();

        String output = req.getOutputPath();

        // =========================================================
        // 1. 获取真实视频参数
        // =========================================================

        String probeCmd = String.format(
                "ffprobe -v error " +
                        "-select_streams v:0 " +
                        "-show_entries stream=width,height,r_frame_rate " +
                        "-of default=noprint_wrappers=1 \"%s\"",
                realVideo
        );

        String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);

        int width = extractIntValue(
                probeResult,
                "width=(\\d+)"
        );

        int height = extractIntValue(
                probeResult,
                "height=(\\d+)"
        );

        String fpsString = extractStringValue(
                probeResult,
                "r_frame_rate=([0-9]+/[0-9]+)"
        );

        double fps = parseFps(fpsString);

        log.info(
                "视频参数 width={} height={} fps={}",
                width,
                height,
                fps
        );

        // =========================================================
        // 2. GOP
        // =========================================================

        // 每5秒一个GOP
        int gop = (int) (fps * 5);

        // =========================================================
        // 3. FFmpeg
        // =========================================================

        List<String> args = new ArrayList<>();

        args.add("ffmpeg");

        args.add("-y");

        // =========================================================
        // 输入
        // =========================================================

        args.add("-i");
        args.add(realVideo);

        args.add("-i");
        args.add(fakeVideo);

        // =========================================================
        // filter_complex
        // =========================================================

        /*
         * 说明：
         *
         * 偶数帧:
         *      100% real
         *
         * 奇数帧:
         *      80% real
         *      20% fake
         *
         * 会形成一种持续性的轻微爆闪感
         */

        String filterComplex =
                String.format(
                        "[0:v]" +
                                "fps=%f," +
                                "scale=%d:%d," +
                                "setsar=1" +
                                "[r];" +

                                "[1:v]" +
                                "fps=%f," +
                                "scale=%d:%d," +
                                "setsar=1" +
                                "[f];" +

                                "[r][f]" +
                                "blend=all_expr='if(eq(mod(N,2),0),A,0.80*A+0.20*B)'" +
                                "[outv]",

                        fps,
                        width,
                        height,

                        fps,
                        width,
                        height
                );

        args.add("-filter_complex");

        args.add(filterComplex);

        // =========================================================
        // 输出映射
        // =========================================================

        args.add("-map");
        args.add("[outv]");

        // 保留真实视频音频
        args.add("-map");
        args.add("0:a?");

        // =========================================================
        // 编码参数
        // =========================================================

        args.add("-c:v");
        args.add("libx264");

        // 编码速度
        args.add("-preset");
        args.add("slow");

        // 画质
        args.add("-crf");
        args.add("18");

        // 禁用 B Frame
        args.add("-bf");
        args.add("0");

        // GOP
        args.add("-g");
        args.add(String.valueOf(gop));

        args.add("-keyint_min");
        args.add(String.valueOf(gop));

        // 禁止场景切换自动关键帧
        args.add("-sc_threshold");
        args.add("0");

        // 像素格式
        args.add("-pix_fmt");
        args.add("yuv420p");

        // 音频直接复制
        args.add("-c:a");
        args.add("copy");

        // 输出
        args.add(output);

        // =========================================================
        // 执行
        // =========================================================

        log.info("开始生成实验性混合视频...");

        ffmpegUtil.executeCommand(args);

        log.info("生成完成: {}", output);
    }



    /**
     * 实验性连续帧混合
     *
     * 特点：
     * 1. 高频帧混合
     * 2. 保持原视频 fps
     * 3. 禁用 B Frame
     * 4. 固定 GOP
     * 5. 降低码率暴涨
     * 6. 轻微闪烁效果
     */
    @Override
    public void createBlendVideo(VideoGenerateRequest req) throws Exception {

        String fakeVideo = req.getFakeVideoPath();
        String realVideo = req.getRealVideoPath();
        String output = req.getOutputPath();

        // ============================================
        // 1. 获取真实视频信息
        // ============================================

        String probeCmd = String.format(
                "ffprobe -v error " +
                        "-select_streams v:0 " +
                        "-show_entries stream=width,height,r_frame_rate " +
                        "-of default=noprint_wrappers=1 \"%s\"",
                realVideo
        );

        String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);

        int width = extractIntValue(probeResult, "width=(\\d+)");
        int height = extractIntValue(probeResult, "height=(\\d+)");

        String fpsString = extractStringValue(
                probeResult,
                "r_frame_rate=([0-9]+/[0-9]+)"
        );

        double fps = parseFps(fpsString);

        log.info("视频信息: width={}, height={}, fps={}",
                width, height, fps);

        // ============================================
        // 2. GOP
        // ============================================

        int gop = (int) (fps * 5);

        // ============================================
        // 3. FFmpeg
        // ============================================

        List<String> args = new ArrayList<>();

        args.add("ffmpeg");
        args.add("-y");

        // 输入
        args.add("-i");
        args.add(realVideo);

        args.add("-i");
        args.add(fakeVideo);

        // ============================================
        // 4. Filter
        // ============================================

        /*
         * 核心思想：
         *
         * 偶数帧:
         *      100% real
         *
         * 奇数帧:
         *      92% real
         *      8% fake
         *
         * 形成轻微连续编码扰动
         */

        String filterComplex = String.format(
                "[0:v]fps=%f,scale=%d:%d[r];" +
                        "[1:v]fps=%f,scale=%d:%d[f];" +

                        "[r][f]" +
                        "blend=all_expr='if(eq(mod(N,2),0),A,0.92*A+0.08*B)'"
                        + "[outv]",
                fps,
                width,
                height,

                fps,
                width,
                height
        );

        args.add("-filter_complex");
        args.add(filterComplex);

        args.add("-map");
        args.add("[outv]");

        args.add("-map");
        args.add("0:a?");

        // ============================================
        // 5. 编码参数
        // ============================================

        args.add("-c:v");
        args.add("libx264");

        args.add("-preset");
        args.add("slow");

        args.add("-crf");
        args.add("18");

        // 禁止 B Frame
        args.add("-bf");
        args.add("0");

        // GOP
        args.add("-g");
        args.add(String.valueOf(gop));

        args.add("-keyint_min");
        args.add(String.valueOf(gop));

        // 禁止场景切换关键帧
        args.add("-sc_threshold");
        args.add("0");

        // yuv420p
        args.add("-pix_fmt");
        args.add("yuv420p");

        // 音频直接复制
        args.add("-c:a");
        args.add("copy");

        args.add(output);

        // ============================================
        // 6. 执行
        // ============================================

        log.info("开始生成实验性混合视频...");

        ffmpegUtil.executeCommand(args);

        log.info("生成完成: {}", output);
    }
    /**
     * 30000/1001 -> 29.97
     */
    private double parseFps(String fpsString) {

        if (fpsString.contains("/")) {

            String[] arr = fpsString.split("/");

            double a = Double.parseDouble(arr[0]);
            double b = Double.parseDouble(arr[1]);

            return a / b;
        }

        return Double.parseDouble(fpsString);
    }

    // 递归删除临时文件夹的工具方法
    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

    // 支持的视频格式后缀
    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".avi", ".mkv", ".mov", ".flv", ".wmv"};

   //

    /**
     * 批量处理文件夹中的所有视频
     */
    @Override
    public List<String> processVideoFolder(String fakePath, String folderPath, int intervalSeconds) throws Exception {

        List<String> processedFiles = new ArrayList<>();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("指定的文件夹路径不存在或不是有效目录: " + folderPath);
        }

        // 1. 在当前文件夹中创建名为 "fake" 的子文件夹
        File fakeDir = new File(folder, "fake");
        if (!fakeDir.exists()) {
            fakeDir.mkdirs();
        }
        String fakeDirPath = fakeDir.getAbsolutePath();

        // 2. 遍历文件夹（包含子文件夹）获取所有视频文件
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                // 如果是子目录，可以选择递归处理（这里暂按只处理当前目录演示，如需递归可参考之前的 walkFileTree）
                if (file.isFile() && isVideoFile(file.getName())) {
                    // 3. 构建输出路径：fake文件夹/原文件名_fake.mp4
                    String outputFileName = file.getName().replaceFirst("[.][^.]+$", "") + "_fake.mp4";
                    String outputPath = new File(fakeDirPath, outputFileName).getAbsolutePath();

                    // 4. 组装请求实体并调用处理方法
                    VideoGenerateRequest req = new VideoGenerateRequest();
                    req.setFakeVideoPath(fakePath);
                    req.setRealVideoPath(file.getAbsolutePath());
                    req.setOutputPath(outputPath);
                    req.setIntervalSeconds(intervalSeconds);

                    // 执行生成
//                    createPeriodicFakeVideoKeyframeVideo(req);
                    createTemporalContinuityVideo(req);
                    processedFiles.add(outputPath);
                }
            }

        }
        return processedFiles;
    }
    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (lowerName.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * 从风景视频中提取第一帧作为 YouTube 自定义缩略图
     * @param fakeVideoPath 风景视频绝对路径
     * @param outputCoverPath 输出封面图的绝对路径 (如: F:/.../cover.jpg)
     */
    public void extractYoutubeThumbnail(String fakeVideoPath, String outputCoverPath) throws Exception {
        // ✅ 必须使用 List 传参，防止 Windows 路径含空格导致命令截断
        List<String> command = Arrays.asList(
                "ffmpeg", "-y",
                "-i", fakeVideoPath,
                "-vf", "scale=1280:720,format=yuv420p",
                "-vframes", "1",
                outputCoverPath
        );

        log.info("🖼️ 开始提取YouTube封面: {}", outputCoverPath);

        // 复用你项目中已有的执行方法（需确保该方法支持 List<String>）
        ffmpegUtil.executeCommand(command);

        // 校验文件是否真正生成成功
        File coverFile = new File(outputCoverPath);
        if (!coverFile.exists() || coverFile.length() == 0) {
            throw new IOException("封面图提取失败或文件大小为0: " + outputCoverPath);
        }

        log.info("✅ YouTube封面提取成功: {} ({}KB)",
                outputCoverPath, coverFile.length() / 1024);
    }
    /**
     * 终极版：周期性插入风景视频片段，并强制设为关键帧
     * @param fakeVideoPath   风景视频绝对路径
     * @param realVideoPath   真实视频绝对路径
     * @param outputPath      输出视频绝对路径
     * @param intervalSeconds 插入风景视频的间隔（单位：秒）
     */
    public void createPeriodicFakeVideoKeyframeVideo(String fakeVideoPath, String realVideoPath,
                                                String outputPath, int intervalSeconds) throws Exception {

        // 【核心修复】创建纯英文临时工作区，彻底规避 JDK8 + Windows 中文路径 -22 报错
        Path tempDir = Files.createTempDirectory("ffmpeg_work_");
        String tempFake = tempDir.resolve("fake.mp4").toString();
        String tempReal = tempDir.resolve("real.mp4").toString();
        String tempOut  = tempDir.resolve("out.mp4").toString();

        try {
            // 1. 复制文件到临时英文路径 (Files.copy 内部走 NIO，不受 ProcessBuilder 编码限制)
            log.info("正在准备临时工作文件...");
            Files.copy(Paths.get(fakeVideoPath), Paths.get(tempFake), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(realVideoPath), Paths.get(tempReal), StandardCopyOption.REPLACE_EXISTING);

            // 2. 探测真实视频元数据（使用临时英文路径）
            String probeCmd = String.format(
                    "ffprobe -v error -select_streams v:0 -show_entries stream=width,height,r_frame_rate -of default=noprint_wrappers=1 \"%s\"",
                    tempReal
            );
            String probeResult = ffmpegUtil.executeAndGetOutput(probeCmd);
            int width = extractIntValue(probeResult, "width=(\\d+)");
            int height = extractIntValue(probeResult, "height=(\\d+)");
            String fpsStr = extractStringValue(probeResult, "r_frame_rate=(\\d+/\\d+)");

            // 计算实际帧率数值，用于基于帧号的关键帧控制
            String[] fpsParts = fpsStr.split("/");
            double fpsVal = Double.parseDouble(fpsParts[0]) / Double.parseDouble(fpsParts[1]);
            int frameInterval = (int) Math.round(fpsVal * (intervalSeconds + 1)); // +1 是因为开头有1秒封面

            // 3. 构建滤镜图（移除所有可能引起争议的 setdar/format=auto）
            String filterComplex = String.format(
                    "[0:v]fps=%s,scale=%d:%d,setsar=1,pix_fmt=yuv420p,trim=0:1,setpts=PTS-STARTPTS[cover];" +
                            "[1:v]fps=%s,scale=%d:%d,setsar=1,pix_fmt=yuv420p[vreal];" +
                            "[0:v]fps=%s,scale=%d:%d,setsar=1,pix_fmt=yuv420p,trim=0:0.04,setpts=PTS-STARTPTS[fake];" +
                            "[vreal][fake]concat=n=2:v=1:a=0[body];" +
                            "[cover][body]concat=n=2:v=1:a=0[outv]",
                    fpsStr, width, height,
                    fpsStr, width, height,
                    fpsStr, width, height
            );

            // 4. 【核心修复】改用基于帧号的 force_key_frames，彻底避开时间表达式解析坑
            // n=0 确保首帧(封面)是关键帧；mod(n,frameInterval)=0 确保周期性风景片段是关键帧
            String keyFrameExpr = "expr:eq(n,0)+eq(mod(n," + frameInterval + "),0)";

            List<String> args = Arrays.asList(
                    "ffmpeg", "-y",
                    "-stream_loop", "-1", "-i", tempFake,
                    "-i", tempReal,
                    "-filter_complex", filterComplex,
                    "-map", "[outv]",
                    "-map", "1:a?",
                    "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
                    "-force_key_frames", keyFrameExpr,
                    "-sc_threshold", "0",
                    "-c:a", "aac", "-b:a", "128k",
                    tempOut
            );

            log.info("执行FFmpeg命令(临时路径): {}", String.join(" ", args));
            ffmpegUtil.executeCommand(args);

            // 5. 将结果移回目标路径
            Path targetPath = Paths.get(outputPath);
            Files.createDirectories(targetPath.getParent()); // 确保输出目录存在
            Files.move(Paths.get(tempOut), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 视频生成成功并已移至: {}", outputPath);

        } finally {
            // 6. 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(tempFake));
                Files.deleteIfExists(Paths.get(tempReal));
                Files.deleteIfExists(Paths.get(tempOut));
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                log.warn("清理临时文件失败: {}", e.getMessage());
            }
        }

    }

    // extractIntValue / extractDoubleValue 保持不变
    private String extractStringValue(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) return matcher.group(1);
        throw new RuntimeException("无法从 ffprobe 结果中提取字符串参数: " + regex);
    }

    private int extractIntValue(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        throw new RuntimeException("无法从 ffprobe 结果中提取整数参数: " + regex);
    }

    private double extractDoubleValue(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        if (matcher.find()) return Double.parseDouble(matcher.group(1));
        throw new RuntimeException("无法从 ffprobe 结果中提取浮点参数: " + regex);
    }

    private void deleteQuietly(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walk(path).sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }
    }

}
