# FindTest - Video Processing Project

## Project Overview
Spring Boot application for video processing using FFmpeg. The main purpose is to modify videos so that platform fingerprint detection cannot identify them, while keeping them visually identical to humans.

## Key Architecture
- **Controller**: `DoController.java` (`/do/*` endpoints)
- **Service Interface**: `VideoDeceptionService.java`
- **Service Implementation**: `VideoDeceptionServiceImpl.java` (core logic)
- **FFmpeg Executor**: `FFmpegUtil.java` (ProcessBuilder-based, supports String and List<String> commands)
- **DTO**: `VideoGenerateRequest.java` (fakeVideoPath, realVideoPath, outputPath, intervalSeconds)

## Current Active Endpoints
- `POST /do/generate-batch` — Batch process all videos in a folder (currently calls `createTemporalContinuityVideo`)
- `POST /do/generate-periodicFake` — Single video with periodic fake keyframe overlay
- `POST /do/generate-deception` — Single video deception processing

## Current Task: Progress Bar Thumbnail Deception
**Goal**: After uploading, progress bar scrub preview shows FAKE/scenery content, but normal playback shows REAL content.

**Planned Approach**: Dual I-Frame Technique
- At regular intervals, insert 1 frame of pure fake as forced I-frame (keyframe)
- Immediately next frame: pure real I-frame (clean GOP start)
- All remaining frames are real content as P-frames
- Video players use I-frames for seek thumbnails → shows fake
- P-frames reference clean real I-frame → playback shows real

**Plan file**: `C:\Users\Niko\.claude\plans\encapsulated-crunching-ripple.md`

**Next step**: User has a reference video that already achieves this effect. Need to analyze it with ffprobe to understand its frame structure before implementing.

## Key Implementation Files
- `findTest/src/main/java/com/niko/Service/VideoDeceptionService.java` — Interface
- `findTest/src/main/java/com/niko/Service/impl/VideoDeceptionServiceImpl.java` — All processing logic
- `findTest/src/main/java/com/niko/controller/DoController.java` — REST endpoints
- `findTest/src/main/java/com/niko/dto/VideoGenerateRequest.java` — Request DTO
- `findTest/src/main/java/com/niko/util/FFmpegUtil.java` — FFmpeg command execution

## Existing Video Processing Methods (in VideoDeceptionServiceImpl)
- `createDenseKeyframeVideo()` — Full dense keyframe injection with frame alternation
- `createPeriodicKeyframeVideo()` — Lightweight periodic forced keyframes only
- `createPeriodicFakeKeyframeVideo()` — Periodic fake image overlay at intervals
- `createPeriodicFakeKeyframeVideoNew()` — Same but uses List<String> args, with 0.04s overlay
- `createPeriodicFakeVideoKeyframeVideo()` — Extracts frames from fake video, overlays at intervals
- `createTemporalContinuityVideo()` — 0.8% fake blend with temporal propagation (currently active)
- `createBlendVideo()` — 92% real / 8% fake on odd frames
- `createExperimentalBlendVideo()` — 80% real / 20% fake on odd frames
- `processVideoFolder()` — Batch processor, currently calls createTemporalContinuityVideo
