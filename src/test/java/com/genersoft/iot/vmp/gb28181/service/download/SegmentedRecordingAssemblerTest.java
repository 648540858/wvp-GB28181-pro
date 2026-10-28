package com.genersoft.iot.vmp.gb28181.service.download;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 使用本地可重复合成录像验证真实帧拼接和拒绝分支，不依赖生产录像、OCR或外部网络。 */
public class SegmentedRecordingAssemblerTest {
    @TempDir
    Path directory;
    private SegmentedDownloadSettings settings;
    private String ffmpeg;
    private String ffprobe;

    /** 工具可由运行时测试环境指定，缺少工具的普通单测环境明确跳过媒体集成用例。 */
    @BeforeEach
    void prepare() {
        ffmpeg = System.getenv().getOrDefault("WVP_TEST_FFMPEG", "/usr/bin/ffmpeg");
        ffprobe = System.getenv().getOrDefault("WVP_TEST_FFPROBE", "/usr/bin/ffprobe");
        assumeTrue(Files.isExecutable(Path.of(ffmpeg)) && Files.isExecutable(Path.of(ffprobe)), "本地未配置FFmpeg/FFprobe");
        settings = new SegmentedDownloadSettings();
        settings.setFfmpeg(ffmpeg);
        settings.setFfprobe(ffprobe);
        settings.setMaxJobBytes(64L * 1024 * 1024);
        settings.setJobTimeoutSeconds(120);
    }

    /** 真实重叠只保留一次，非整秒目标按下一真实帧裁切，AAC和B帧包也必须完整解码。 */
    @Test
    void assemblesUniqueOverlapAndCutsOnlyAtRealFrameBoundary() throws Exception {
        Path original = source("original.mp4", "testsrc2=size=96x64:rate=25", 8, true);
        Path first = clip(original, "first ' quoted.mp4", 0, 3, null, true);
        Path second = clip(original, "second.mp4", 2, 6, null, true);
        Path third = clip(original, "third.mp4", 5, 8, null, true);
        Path work = Files.createDirectory(directory.resolve("work"));
        Path result = new SegmentedRecordingAssembler(settings).assemble(List.of(first, second, third), 6.17, work, () -> false);
        assertEquals(work.resolve("verified.mp4"), result);
        assertTrue(Files.size(result) > 0);
        String frames = tool(List.of(ffprobe, "-v", "error", "-select_streams", "v:0", "-count_frames",
                "-show_entries", "stream=nb_read_frames", "-of", "default=nw=1:nk=1", result.toString()));
        assertEquals(155, Integer.parseInt(frames.trim()));
        double[] audio = decodedAudioRange(result);
        assertTrue(Math.abs(audio[0]) <= audio[2]);
        assertTrue(Math.abs(audio[1] - 6.2) <= audio[2], "真实解码音频须覆盖视频，不能只看容器时长");
        assertTrue(Files.exists(work.resolve("verified-audit.json")));
        assertNoTemporaryWork(work);
    }

    /** 视频足够而音轨明显短尾时拒绝；没有音轨的视频则仍能正常完成。 */
    @Test
    void refusesShortAudioTailButSupportsVideoWithoutAudio() throws Exception {
        Path input = directory.resolve("short-audio.mp4");
        tool(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i", "testsrc2=size=96x64:rate=25",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=16000:duration=1", "-t", "6",
                "-c:v", "libx264", "-qp", "0", "-preset", "ultrafast", "-g", "1", "-threads:v", "1",
                "-c:a", "aac", input.toString()));
        assertTrue(assertRefused(List.of(input), 5).getMessage().contains("音频首尾"));
        Path videoOnly = directory.resolve("video-only.mp4");
        tool(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-i", input.toString(), "-map", "0:v:0",
                "-c:v", "copy", videoOnly.toString()));
        Path work = Files.createDirectory(directory.resolve("no-audio"));
        Path result = new SegmentedRecordingAssembler(settings).assemble(List.of(videoOnly), 5, work, () -> false);
        assertTrue(Files.isRegularFile(result));
        assertNoTemporaryWork(work);
    }

    /** 源音频首两帧覆盖同一时间时只保留先到原样本，不能让WAV顺接多出63毫秒。 */
    @Test
    void normalizesOverlappingAudioSamplesWithoutPadding() throws Exception {
        Path source = timestampedAudioSource("overlap.nut", "asetpts='if(eq(N,0),PTS,PTS-0.063/TB)'");
        Path work = Files.createDirectory(directory.resolve("audio-overlap"));
        Path result = new SegmentedRecordingAssembler(settings).assemble(List.of(source), 5, work, () -> false);
        double[] audio = decodedAudioRange(result);
        assertTrue(Math.abs(audio[1] - 5) <= audio[2]);
        var audit = com.alibaba.fastjson2.JSON.parseObject(Files.readString(work.resolve("verified-audit.json")));
        var selection = audit.getJSONArray("sourceAudio").getJSONObject(0);
        assertEquals(1008, selection.getLongValue("discardedOverlapSamples"));
        assertEquals(80000, selection.getLongValue("selectedSamples"));
        assertNoTemporaryWork(work);
    }

    /** 原音频出现真实时间间隙必须拒绝，不能用重采样自动插入静音伪装完整。 */
    @Test
    void refusesRealAudioTimestampGap() throws Exception {
        Path source = timestampedAudioSource("gap.nut", "asetpts='if(gte(N,16000),PTS+0.2/TB,PTS)'");
        assertTrue(assertRefused(List.of(source), 5).getMessage().contains("真实间隙"));
    }

    /** 用有时间戳PCM容器合成可重复异常，不依赖生产录像或私有夹具。 */
    private Path timestampedAudioSource(String name, String filter) throws Exception {
        Path output = directory.resolve(name);
        tool(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i", "testsrc2=size=96x64:rate=25",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=16000", "-t", "6", "-filter:a", filter,
                "-c:v", "libx264", "-threads:v", "1", "-qp", "0", "-preset", "ultrafast", "-c:a", "pcm_f32le",
                "-f", "nut", output.toString()));
        return output;
    }

    /** 相同静态画面无法唯一定位接缝，不能仅因某个run很长就认定连续。 */
    @Test
    void refusesAmbiguousRepeatedContent() throws Exception {
        Path original = source("static.mp4", "color=black:size=96x64:rate=25", 6, false);
        Path first = clip(original, "first.mp4", 0, 3, null, false);
        Path second = clip(original, "second.mp4", 2, 6, null, false);
        assertTrue(assertRefused(List.of(first, second), 5).getMessage().contains("唯一连续帧锚点"));
    }

    /** 不存在真实公共帧时拒绝接缝，不按请求时长拼接两个无关视频。 */
    @Test
    void refusesMissingOverlap() throws Exception {
        Path first = source("red.mp4", "color=red:size=96x64:rate=25", 3, false);
        Path second = source("blue.mp4", "color=blue:size=96x64:rate=25", 3, false);
        assertTrue(assertRefused(List.of(first, second), 5).getMessage().contains("唯一连续帧锚点"));
    }

    /** 即使存在唯一长锚点，重叠其他位置有一帧不同也不能删掉未验证的内容。 */
    @Test
    void refusesChangedFrameElsewhereInAlignedOverlap() throws Exception {
        Path original = source("original.mp4", "testsrc2=size=96x64:rate=25", 6, false);
        Path first = clip(original, "first.mp4", 0, 3, null, false);
        Path second = clip(original, "second.mp4", 2, 6,
                "drawbox=x=0:y=0:w=16:h=16:t=fill:color=red:enable='eq(n,5)'", false);
        assertTrue(assertRefused(List.of(first, second), 5).getMessage().contains("丢弃区含未验证相同"));
    }

    /** 重叠证明不能补足实际缺少的视频时间，禁止复制最后一帧凑到申请长度。 */
    @Test
    void refusesInsufficientRealDuration() throws Exception {
        Path original = source("original.mp4", "testsrc2=size=96x64:rate=25", 4, false);
        Path first = clip(original, "first.mp4", 0, 3, null, false);
        Path second = clip(original, "second.mp4", 2, 4, null, false);
        assertTrue(assertRefused(List.of(first, second), 5).getMessage().contains("不足申请时长"));
    }

    /** 损坏输入和空间预算越界均不能留下看似成功的成品。 */
    @Test
    void refusesCorruptionAndResourceOverflow() throws Exception {
        Path input = directory.resolve("broken.mp4");
        Files.writeString(input, "not an mp4", StandardCharsets.UTF_8);
        assertRefused(List.of(input), 1);
        settings.setMaxJobBytes(1);
        Path work = Files.createDirectory(directory.resolve("budget"));
        assertThrows(IllegalStateException.class,
                () -> new SegmentedRecordingAssembler(settings).assemble(List.of(input), 1, work, () -> false));
        assertFalse(Files.exists(work.resolve("verified.mp4")));
    }

    /** 预先取消不得启动媒体处理或发布文件，符号链接工作目录也不能写入其他位置。 */
    @Test
    void cancellationAndUnsafeDirectoryNeverPublish() throws Exception {
        Path input = directory.resolve("input.mp4");
        Files.writeString(input, "placeholder");
        Path work = Files.createDirectory(directory.resolve("cancelled"));
        assertThrows(CancellationException.class,
                () -> new SegmentedRecordingAssembler(settings).assemble(List.of(input), 1, work, () -> true));
        assertNoTemporaryWork(work);
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path link = directory.resolve("linked-work");
        Files.createSymbolicLink(link, outside);
        assertThrows(IllegalStateException.class,
                () -> new SegmentedRecordingAssembler(settings).assemble(List.of(input), 1, link, () -> false));
        assertFalse(Files.exists(outside.resolve("verified.mp4")));
    }

    /** 工具挂起时取消能销毁自有进程树，不能让后续任务继承后台媒体进程。 */
    @Test
    void cancellationDestroysRunningTool() throws Exception {
        Path input = directory.resolve("input.mp4");
        Files.writeString(input, "placeholder");
        settings.setFfprobe(hangingTool().toString());
        Path work = Files.createDirectory(directory.resolve("running"));
        AtomicBoolean cancelled = new AtomicBoolean();
        CompletableFuture<Throwable> result = CompletableFuture.supplyAsync(() -> {
            try {
                new SegmentedRecordingAssembler(settings).assemble(List.of(input), 1, work, cancelled::get);
                return null;
            } catch (Throwable error) {
                return error;
            }
        });
        long pid = awaitToolPid(work);
        cancelled.set(true);
        assertInstanceOf(CancellationException.class, result.get(5, TimeUnit.SECONDS));
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        assertNoTemporaryWork(work);
    }

    /** 总截止时间到达后杀掉挂起工具并保留失败状态，不能等待系统进程自行退出。 */
    @Test
    void timeoutDestroysRunningTool() throws Exception {
        Path input = directory.resolve("input.mp4");
        Files.writeString(input, "placeholder");
        settings.setFfprobe(hangingTool().toString());
        settings.setJobTimeoutSeconds(1);
        Path work = Files.createDirectory(directory.resolve("timeout"));
        long start = System.nanoTime();
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new SegmentedRecordingAssembler(settings).assemble(List.of(input), 1, work, () -> false));
        assertTrue(error.getMessage().contains("超时"));
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(5));
        assertNoTemporaryWork(work);
    }

    /** 生成带确定性运动帧的无损源，所有源素材均由测试即时合成。 */
    private Path source(String name, String video, double seconds, boolean audio) throws Exception {
        Path output = directory.resolve(name);
        List<String> command = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-f", "lavfi", "-i", video));
        if (audio) command.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:sample_rate=16000"));
        command.addAll(List.of("-t", Double.toString(seconds), "-c:v", "libx264", "-preset", "ultrafast", "-qp", "0", "-g", "1", "-threads", "1"));
        if (audio) command.addAll(List.of("-c:a", "aac"));
        command.add(output.toString());
        tool(command);
        return output;
    }

    /** 无损裁出同一源的重叠片段，可额外注入一帧改变来检验完整重叠证明。 */
    private Path clip(Path source, String name, double start, double end, String extra, boolean audio) throws Exception {
        Path output = directory.resolve(name);
        String video = "trim=start=" + start + ":end=" + end + ",setpts=PTS-STARTPTS" + (extra == null ? "" : "," + extra);
        List<String> command = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-v", "error", "-y", "-threads", "1", "-i", source.toString(),
                "-vf", video, "-c:v", "libx264", "-preset", "ultrafast", "-qp", "0", "-g", "1", "-threads:v", "1", "-vsync", "0"));
        if (audio) command.addAll(List.of("-af", "atrim=start=" + start + ":end=" + end + ",asetpts=PTS-STARTPTS", "-c:a", "aac"));
        command.add(output.toString());
        tool(command);
        return output;
    }

    /** 失败必须通过公共入口观察，输入仍保留，成品和临时处理目录都不能伪装为成功。 */
    private IllegalStateException assertRefused(List<Path> inputs, double seconds) throws Exception {
        Path work = Files.createTempDirectory(directory, "refused-");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new SegmentedRecordingAssembler(settings).assemble(inputs, seconds, work, () -> false));
        assertFalse(Files.exists(work.resolve("verified.mp4")));
        for (Path input : inputs) assertTrue(Files.exists(input));
        assertNoTemporaryWork(work);
        return error;
    }

    /** 固定工具输出文件与等待上限，测试本身也不能无限捕获进程日志。 */
    private String tool(List<String> command) throws Exception {
        Path stdout = Files.createTempFile(directory, "tool-", ".out");
        Path stderr = Files.createTempFile(directory, "tool-", ".err");
        Process process = new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "合成媒体工具超时");
            assertTrue(Files.size(stderr) < 32_768);
            assertEquals(0, process.exitValue(), Files.readString(stderr));
            assertTrue(Files.size(stdout) < 32_768);
            return Files.readString(stdout);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    /** 独立读取成品的实际AAC解码样本范围，避免断言再次依赖容器声明时长。 */
    private double[] decodedAudioRange(Path input) throws Exception {
        int rate = Integer.parseInt(tool(List.of(ffprobe, "-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=sample_rate", "-of", "default=nw=1:nk=1", input.toString())).trim());
        String frames = tool(List.of(ffprobe, "-v", "error", "-select_streams", "a:0", "-show_frames",
                "-show_entries", "frame=best_effort_timestamp_time,nb_samples:side_data=", "-of", "compact=p=0:nk=0", input.toString()));
        double first = Double.NaN;
        double end = Double.NaN;
        for (String line : frames.split("\\R")) {
            if (line.isBlank()) continue;
            double pts = Double.NaN;
            int samples = 0;
            for (String field : line.split("\\|")) {
                if (field.startsWith("best_effort_timestamp_time=")) pts = Double.parseDouble(field.substring(field.indexOf('=') + 1));
                if (field.startsWith("nb_samples=")) samples = Integer.parseInt(field.substring(field.indexOf('=') + 1));
            }
            assertTrue(Double.isFinite(pts) && samples > 0);
            if (!Double.isFinite(first)) first = pts;
            end = pts + (double) samples / rate;
        }
        assertTrue(Double.isFinite(first) && Double.isFinite(end));
        return new double[]{first, end, Math.min(0.15, 1024.0 / rate + 0.002)};
    }

    /** 可控挂起工具只供测试进程终止；生产实现仍使用参数列表启动真实工具。 */
    private Path hangingTool() throws IOException {
        Path tool = directory.resolve("hanging-tool.sh");
        Files.writeString(tool, "#!/bin/sh\necho $$\nsleep 30\n");
        assertTrue(tool.toFile().setExecutable(true));
        return tool;
    }

    /** 有界等待工具输出自己的PID，避免依赖固定sleep猜测是否已进入运行阶段。 */
    private long awaitToolPid(Path work) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try (var files = Files.walk(work)) {
                for (Path path : files.filter(p -> p.toString().endsWith("source-0-probe.out")).toList()) {
                    String value = Files.readString(path).trim();
                    if (value.matches("[0-9]+")) return Long.parseLong(value);
                }
            }
            Thread.sleep(20);
        }
        fail("挂起工具未在限定时间内启动");
        return -1;
    }

    /** 每次执行结束都释放自有临时目录和互斥文件。 */
    private void assertNoTemporaryWork(Path work) throws IOException {
        try (var files = Files.list(work)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".assembly")
                    || path.getFileName().toString().equals(".assembling")));
        }
    }

    /** 可在现网同基础Java21+FFmpeg4镜像运行同一组合成回归，产物只留在指定忽略目录。 */
    public static void main(String[] args) throws Exception {
        if (args.length >= 4 && args[0].equals("--assemble")) {
            SegmentedDownloadSettings config = new SegmentedDownloadSettings();
            config.setFfmpeg(System.getenv().getOrDefault("WVP_TEST_FFMPEG", "/usr/bin/ffmpeg"));
            config.setFfprobe(System.getenv().getOrDefault("WVP_TEST_FFPROBE", "/usr/bin/ffprobe"));
            List<Path> inputs = new ArrayList<>();
            for (int index = 3; index < args.length; index++) inputs.add(Path.of(args[index]));
            Path result = new SegmentedRecordingAssembler(config).assemble(inputs, Double.parseDouble(args[1]),
                    Path.of(args[2]), () -> false);
            System.out.println("已验证合并成品: " + result);
            return;
        }
        Path root = args.length == 0 ? Files.createTempDirectory("assembler-tests-") : Path.of(args[0]);
        Files.createDirectories(root);
        List<String> cases = List.of("assemblesUniqueOverlapAndCutsOnlyAtRealFrameBoundary", "refusesShortAudioTailButSupportsVideoWithoutAudio", "normalizesOverlappingAudioSamplesWithoutPadding",
                "refusesRealAudioTimestampGap", "refusesAmbiguousRepeatedContent",
                "refusesMissingOverlap", "refusesChangedFrameElsewhereInAlignedOverlap", "refusesInsufficientRealDuration",
                "refusesCorruptionAndResourceOverflow", "cancellationAndUnsafeDirectoryNeverPublish",
                "cancellationDestroysRunningTool", "timeoutDestroysRunningTool");
        for (String name : cases) {
            SegmentedRecordingAssemblerTest test = new SegmentedRecordingAssemblerTest();
            test.directory = Files.createTempDirectory(root, name + "-");
            test.prepare();
            try {
                SegmentedRecordingAssemblerTest.class.getDeclaredMethod(name).invoke(test);
            } catch (InvocationTargetException error) {
                throw new AssertionError(name + "失败", error.getCause());
            }
            System.out.println("PASS " + name);
        }
        System.out.println("通过合成媒体回归: " + cases.size());
    }
}
