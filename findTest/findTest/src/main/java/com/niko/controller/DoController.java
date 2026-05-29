package com.niko.controller;


import com.niko.Service.*;
import com.niko.dto.VideoGenerateRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;
@Slf4j
@Controller
@RequestMapping("/do")
@CrossOrigin // 允许前端跨域访问
public class DoController {

    @Autowired
    private DoFindService doFindService;

    @Autowired
    private DoFilterExcelService doFilterExcelService;

    @Autowired
    private DealNumberService dealNumberService;

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private VideoDeceptionService videoService;

    public DoController(VideoDeceptionService videoDeceptionService) {
        this.videoDeceptionService = videoDeceptionService;
    }

    @GetMapping("/generate")
    public String generateVideo() {
        try {
            // 换成你电脑上真实的绝对路径
            String fakeImg = "F:\\carl_work\\carl\\Google\\2026test\\test\\photo_2026-04-28_16-44-43.jpg";
            String realVid = "F:\\carl_work\\carl\\Google\\2026test\\test\\9.mp4";
            String output = "F:\\carl_work\\carl\\Google\\2026test\\test\\out\\out-1.mp4";

            videoService.createDenseKeyframeVideo(fakeImg, realVid, output);
            return "🎉 成功！密集关键帧伪装视频已生成：" + output;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 生成失败：" + e.getMessage();
        }
    }

    @GetMapping("/generate-periodic")
    public String generatePeriodicVideo() {
        try {
            // 换成你电脑上真实的绝对路径
            String realVid = "F:\\carl_work\\carl\\Google\\2026test\\test\\9.mp4";
            String output = "F:\\carl_work\\carl\\Google\\2026test\\test\\out\\out-1.mp4";
            int interval = 2; // 设定每隔 2 秒强制插入一个关键帧

            videoService.createPeriodicKeyframeVideo(realVid, output, interval);
            return "🎉 成功！周期性关键帧视频已生成，间隔：" + interval + "秒。路径：" + output;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 生成失败：" + e.getMessage();
        }
    }

    /**
     * 使用风景图
     *  单个视频处理接口（接收 JSON 请求体）
     * @return
     */
    @PostMapping("/generate-periodicFake")
    public ResponseEntity<Map<String, String>> generateSingleVideo(@RequestBody VideoGenerateRequest req) {
        Map<String, String> response = new HashMap<>();
        try {
            // 设置默认间隔为 1 秒（如果前端没传）
            if (req.getIntervalSeconds() == null || req.getIntervalSeconds() <= 0) {
                req.setIntervalSeconds(1);
            }
            videoService.createPeriodicFakeKeyframeVideoNew(req);
            response.put("msg", "✅ 单个视频生成成功: " + req.getOutputPath());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "视频生成失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 批量处理文件夹接口（接收 JSON 请求体）
     * 只需要传入 fakeVideoPath（风景图/视频） 和 folderPath（要处理的视频文件夹） s
     */
    @PostMapping("/generate-batch")
    public ResponseEntity<Map<String, Object>> generateBatchVideos(@RequestBody Map<String, String> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            String fakePath = params.get("fakeVideoPath");
            String folderPath = params.get("folderPath");
            Integer interval = params.get("intervalSeconds") != null ? Integer.parseInt(params.get("intervalSeconds")) : 1;

            if (fakePath == null || folderPath == null) {
                throw new IllegalArgumentException("缺少必要参数: fakeVideoPath 或 folderPath");
            }

            // 调用 Service 层进行批量处理
            List<String> processedFiles = videoService.processVideoFolder(fakePath, folderPath, interval);

            response.put("msg", "✅ 文件夹批量处理完成");
            response.put("count", processedFiles.size());
            response.put("files", processedFiles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "批量处理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private final VideoDeceptionService videoDeceptionService;

    @PostMapping("/generate-deception")
    public ResponseEntity<Map<String, Object>> generateDeceptionVideo(
            @RequestBody VideoGenerateRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            // 参数基础校验 // 默认1秒插入一次
            if (request.getIntervalSeconds() == null || request.getIntervalSeconds() < 1) {
                request.setIntervalSeconds(1);
            }

            videoDeceptionService.createPeriodicFakeVideoKeyframeVideo(request);

            response.put("code", 200);
            response.put("msg", "✅ 视频生成成功");
            response.put("outputPath", request.getOutputPath());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("参数校验失败: {}", e.getMessage());
            response.put("code", 400);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("视频生成异常", e);
            response.put("code", 500);
            response.put("error", "视频生成失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @ResponseBody
    @GetMapping("/find")
    public String find(@RequestBody List<String> urls) {
        String s = doFindService.find(urls);

        return s;
    }

    @ResponseBody
    @GetMapping("/filter")
    public String filter(@RequestBody MultipartFile file) throws IOException {
        String excelName = file.getOriginalFilename();
        String fileName = excelName.substring(0, excelName.lastIndexOf("."));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String format = sdf.format(new Date());
        String fileWriteName = "D:\\carl\\文件\\" + format + "\\" + fileName;
        File file1 = new File(fileWriteName);
        if (!file1.exists()) {
            file1.mkdir();
        }
        doFilterExcelService.filterExcel(file.getInputStream(), file1.getAbsolutePath() + "\\fliter-" + excelName);

        return fileWriteName;  //返回文件保存的位置
    }

    @ResponseBody
    @GetMapping("/test")
    public String test() throws IOException {



        return "test";
    }

    /**
     * 获取所有number
     *
     * @param text
     * @return
     */
    @ResponseBody
    @GetMapping("/getNumber")
    public String getNumber(@RequestParam("text") String text) {
        return dealNumberService.getNumber(text);
    }

    /**
     * 获取存活的number
     *
     * @param text
     * @return
     */
    @ResponseBody
    @GetMapping("/getActiveNumber")
    public String getActiveNumber(@RequestParam("text") String text) {
        return dealNumberService.getActiveNumber(text);
    }

    @ResponseBody
    @GetMapping("/getActiveNumber1")
    public String getActiveNumber1(@RequestParam("text") String text, @RequestParam("parm") Integer parm) {
        return dealNumberService.getActiveNumber1(text, parm);
    }

    /**
     * 获取充值金额
     */
    @ResponseBody
    @PostMapping("/getRechargeExcel")
    public String getRechargeExcel(@RequestParam("text") String text, @RequestParam("amount") String amount) {
        BigDecimal rechargeAmount = null;
        if (!StringUtils.isAllBlank(amount) && !"0".equals(amount)) {
            rechargeAmount = new BigDecimal(amount);
        }
        return rechargeService.getRechargeExcel(text, rechargeAmount);
    }

    @ResponseBody
    @GetMapping("/getAccountAndAmount")
    public String getAccountAndAmount(@RequestParam("text") String text) {

        String result = dealNumberService.getAccountAndAmount(text);
        return result;
    }
}
