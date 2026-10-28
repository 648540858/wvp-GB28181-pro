package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** 只合并本地真实解码帧，接缝不确定、资源越界或最终校验失败时绝不发布成品。 */
@Component
public class SegmentedRecordingAssembler {
    private static final double EPSILON = 0.000001;
    private static final double WINDOW_SECONDS = 30;
    private static final int MIN_ANCHOR_FRAMES = 8;
    private static final int MAX_INPUTS = 256;
    private static final int MAX_FRAMES = 500_000;
    private static final int MAX_WINDOW_FRAMES = 3_000;
    private static final int MAX_LOG_BYTES = 32_768;
    private static final long MAX_METADATA_BYTES = 96L * 1024 * 1024;
    private final SegmentedDownloadSettings settings;

    /** 工具路径和空间预算由同一个分段配置owner提供，不接受请求中的可执行命令。 */
    public SegmentedRecordingAssembler(SegmentedDownloadSettings settings) {
        this.settings = settings;
    }

    /** 全量解码、证明重叠、串行转码并校验，只有最后一步才能原子发布verified.mp4。 */
    public Path assemble(List<Path> inputs, double requestedSeconds, Path workDir, BooleanSupplier cancelled) {
        Path staging = null;
        Path lock = null;
        boolean ownsLock = false;
        try {
            if (settings == null || inputs == null || inputs.isEmpty() || inputs.size() > MAX_INPUTS
                    || workDir == null || cancelled == null || !Double.isFinite(requestedSeconds)
                    || requestedSeconds <= 0 || requestedSeconds > settings.getMaxRequestSeconds()
                    || settings.getMaxJobBytes() <= 0 || settings.getJobTimeoutSeconds() <= 0) {
                throw new IOException("分段录像合并参数无效");
            }
            Path root = workDir.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(root)) throw new IOException("合并目录不能是符号链接");
            Files.createDirectories(root);
            root = root.toRealPath();
            Path target = root.resolve("verified.mp4");
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                    || Files.exists(root.resolve("verified-audit.json"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("合并成品或验证凭据已存在");
            }
            lock = root.resolve(".assembling");
            Files.createFile(lock);
            ownsLock = true;
            staging = Files.createDirectory(root.resolve(".assembly-" + UUID.randomUUID()));
            List<Path> sources = new ArrayList<>();
            long sourceBytes = 0;
            for (Path input : inputs) {
                if (input == null || !Files.isRegularFile(input, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("分段输入必须是本地普通文件");
                }
                Path source = input.toRealPath();
                if (!source.startsWith(root)) sourceBytes = Math.addExact(sourceBytes, Files.size(source));
                sources.add(source);
            }
            Context context = new Context(root, staging, sourceBytes,
                    System.nanoTime() + Duration.ofSeconds(settings.getJobTimeoutSeconds()).toNanos(), cancelled);
            check(context);
            List<Clip> clips = new ArrayList<>();
            List<Seam> seams = new ArrayList<>();
            int frameCount = 0;
            for (int index = 0; index < sources.size(); index++) {
                Clip clip = decode(sources.get(index), "source-" + index, context);
                frameCount += clip.frames().size();
                if (frameCount > MAX_FRAMES) throw new IOException("分段解码帧数超出资源上限");
                if (!clips.isEmpty() && !clips.getFirst().format().equals(clip.format())) {
                    throw new IOException("相邻分段的音视频格式不一致");
                }
                if (!clips.isEmpty()) seams.add(align(clips.getLast(), clip, context));
                clips.add(clip);
            }
            List<Cut> cuts = plan(clips, seams, requestedSeconds, context);
            long expectedFrames = 0;
            List<String> manifest = new ArrayList<>();
            List<String> audioManifest = new ArrayList<>();
            List<AudioSelection> audioSelections = new ArrayList<>();
            for (int index = 0; index < cuts.size(); index++) {
                Cut cut = cuts.get(index);
                Path part = staging.resolve(String.format(Locale.ROOT, "part-%03d.mp4", index));
                AudioSelection selection = render(cut, part, context);
                if (selection != null) audioSelections.add(selection);
                expectedFrames += cut.end() - cut.begin();
                manifest.add("file '" + part.getFileName() + "'");
                manifest.add("duration " + number(endTime(cut.clip(), cut.end()) - cut.clip().frames().get(cut.begin()).pts()));
                if (cut.clip().format().audio()) audioManifest.add("file '" + audioPart(part).getFileName() + "'");
            }
            Path list = staging.resolve("parts.txt");
            Files.write(list, manifest, StandardCharsets.UTF_8);
            Path candidate = staging.resolve("candidate.mp4");
            List<String> concat = new ArrayList<>(List.of(settings.getFfmpeg(), "-nostdin", "-v", "warning", "-xerror", "-y", "-threads", "1", "-filter_threads", "1",
                    "-protocol_whitelist", "file,pipe", "-f", "concat", "-safe", "1", "-i", list.toString()));
            if (!audioManifest.isEmpty()) {
                Path audioList = staging.resolve("audio.txt");
                Files.write(audioList, audioManifest, StandardCharsets.UTF_8);
                // PCM中间件不包含每段AAC priming；最终只编码一次音频，不能靠修正DTS掩盖重叠。
                concat.addAll(List.of("-protocol_whitelist", "file,pipe", "-f", "concat", "-safe", "1",
                        "-i", audioList.toString()));
            }
            concat.addAll(List.of("-map", "0:v:0", "-c:v", "copy"));
            if (!audioManifest.isEmpty()) concat.addAll(List.of("-map", "1:a:0", "-c:a", "aac", "-threads:a", "1"));
            concat.addAll(List.of("-movflags", "+faststart", "-video_track_timescale", "1000", candidate.toString()));
            run(concat, "concat", context);
            Clip verified = decode(candidate, "verified", context);
            if (!verified.format().equals(clips.getFirst().format())) {
                throw new IOException("成品音视频结构与源分段不一致或音轨缺失");
            }
            double videoDuration = endTime(verified, verified.frames().size()) - verified.frames().getFirst().pts();
            if (verified.frames().size() != expectedFrames || videoDuration + EPSILON < requestedSeconds
                    || videoDuration > requestedSeconds + 0.1 + EPSILON
                    || Math.abs(verified.frames().getFirst().pts()) > 0.1
                    || Math.abs(verified.containerDuration() - requestedSeconds) > 0.1 + EPSILON) {
                throw new IOException("合并成品的真实帧数或播放时长不满足申请范围");
            }
            verifyPackets(candidate, context);
            AudioRange audio = verified.format().audio() ? verifyAudio(candidate, verified, context) : null;
            Path audit = writeAudit(candidate, requestedSeconds, expectedFrames, verified, seams, audio, audioSelections, context);
            check(context);
            Files.move(audit, root.resolve("verified-audit.json"), StandardCopyOption.ATOMIC_MOVE);
            Files.move(candidate, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException | ArithmeticException error) {
            throw new IllegalStateException("分段录像合并校验失败: " + error.getMessage(), error);
        } finally {
            clean(staging);
            if (ownsLock) {
                try { Files.deleteIfExists(lock); } catch (IOException ignored) { /* 不删除其他任务的文件。 */ }
            }
        }
    }

    /** 保存视频显示顺序PTS和真实帧摘要；不把容器申报时长当作已解码媒体。 */
    private Clip decode(Path input, String label, Context context) throws IOException {
        Path probe = run(List.of(settings.getFfprobe(), "-v", "error", "-protocol_whitelist", "file,pipe",
                "-show_entries", "stream=codec_type,width,height,sample_rate,channels:format=duration",
                "-of", "json", input.toString()), label + "-probe", context);
        JSONObject metadata = JSON.parseObject(Files.readString(probe));
        JSONArray streams = metadata.getJSONArray("streams");
        JSONObject video = null;
        JSONObject audio = null;
        for (int index = 0; streams != null && index < streams.size(); index++) {
            JSONObject stream = streams.getJSONObject(index);
            if ("video".equals(stream.getString("codec_type")) && video == null) video = stream;
            if ("audio".equals(stream.getString("codec_type")) && audio == null) audio = stream;
        }
        if (video == null) throw new IOException("分段缺少视频流");
        Format format = new Format(video.getIntValue("width"), video.getIntValue("height"), audio != null,
                audio == null ? 0 : audio.getIntValue("sample_rate"), audio == null ? 0 : audio.getIntValue("channels"));
        if (format.width() <= 0 || format.height() <= 0 || format.width() > 8192 || format.height() > 8192
                || (long) format.width() * format.height() > 4096L * 2160) {
            throw new IOException("分段视频尺寸无效或超出资源上限");
        }
        List<String> command = new ArrayList<>(List.of(settings.getFfmpeg(), "-nostdin", "-v", "error", "-xerror",
                "-threads", "1", "-filter_threads", "1", "-copyts", "-protocol_whitelist", "file,pipe", "-i", input.toString(),
                "-map", "0:v:0", "-vf", "format=yuv420p", "-c:v", "rawvideo", "-threads:v", "1",
                "-vsync", "0", "-enc_time_base:v", "1:1000", "-frames:v", Integer.toString(MAX_FRAMES + 1), "-f", "framehash", "-hash", "sha256", "pipe:1"));
        if (format.audio()) command.addAll(List.of("-map", "0:a:0", "-f", "null", "-"));
        Path hashes = run(command, label + "-frames", context);
        List<Frame> frames = new ArrayList<>();
        double timeBase = Double.NaN;
        try (BufferedReader reader = Files.newBufferedReader(hashes)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 2048) throw new IOException("帧摘要输出格式无效");
                if (line.startsWith("#tb 0:")) {
                    String[] fraction = line.substring(line.indexOf(':') + 1).trim().split("/");
                    timeBase = Double.parseDouble(fraction[0]) / Double.parseDouble(fraction[1]);
                } else if (!line.isBlank() && !line.startsWith("#")) {
                    String[] fields = line.split(",");
                    if (fields.length != 6 || !fields[0].trim().equals("0") || !Double.isFinite(timeBase)
                            || !fields[5].trim().matches("[0-9a-fA-F]{64}")) throw new IOException("帧摘要无法验证");
                    double pts = Long.parseLong(fields[2].trim()) * timeBase;
                    double duration = Long.parseLong(fields[3].trim()) * timeBase;
                    if (!Double.isFinite(pts) || duration < 0 || !Double.isFinite(duration)
                            || (!frames.isEmpty() && pts <= frames.getLast().pts())) throw new IOException("视频显示时间戳不递增");
                    frames.add(new Frame(pts, duration, fields[5].trim()));
                    if (frames.size() > MAX_FRAMES) throw new IOException("视频帧数超出资源上限");
                }
            }
        } catch (NumberFormatException error) {
            throw new IOException("帧时间戳格式无效", error);
        }
        if (frames.isEmpty()) throw new IOException("视频没有可解码帧");
        readFrameDurations(input, label, frames, context);
        double duration = metadata.getJSONObject("format").getDoubleValue("duration");
        if (!Double.isFinite(duration) || duration <= 0) throw new IOException("视频容器时长无效");
        return new Clip(input, frames, format, duration);
    }

    /** FFmpeg4的rawvideo摘要可能没有duration，逐帧对照真实解码PTS和包持续时间，禁止用容器时长补尾帧。 */
    private void readFrameDurations(Path input, String label, List<Frame> frames, Context context) throws IOException {
        Path output = run(List.of(settings.getFfprobe(), "-v", "error", "-threads", "1",
                "-protocol_whitelist", "file,pipe", "-select_streams", "v:0", "-show_frames",
                "-show_entries", "frame=best_effort_timestamp_time,pkt_duration_time,duration_time:side_data=",
                "-of", "compact=p=0:nk=0", input.toString()), label + "-pts", context);
        int index = 0;
        try (BufferedReader reader = Files.newBufferedReader(output)) {
            String line;
            double last = Double.NEGATIVE_INFINITY;
            while ((line = reader.readLine()) != null) {
                Map<String, String> fields = compactFields(line);
                if (!fields.containsKey("best_effort_timestamp_time")) continue;
                if (index >= frames.size()) throw new IOException("解码帧数与摘要不一致");
                Frame frame = frames.get(index);
                double pts = Double.parseDouble(fields.get("best_effort_timestamp_time"));
                String value = fields.getOrDefault("duration_time", fields.get("pkt_duration_time"));
                double duration = Double.parseDouble(value);
                if (!Double.isFinite(pts) || !Double.isFinite(duration) || duration <= 0 || pts <= last
                        || Math.abs(pts - frame.pts()) > 0.0011) {
                    throw new IOException("解码帧边界缺失或与摘要不一致");
                }
                frames.set(index++, new Frame(pts, duration, frame.hash()));
                last = pts;
            }
        } catch (NumberFormatException | NullPointerException error) {
            throw new IOException("解码帧持续时间不可验证", error);
        }
        if (index != frames.size()) throw new IOException("显示帧与摘要数量不一致");
    }

    /** 只按明确字段名解析ffprobe紧凑输出，兼容字段顺序与附加side-data变化。 */
    private Map<String, String> compactFields(String line) throws IOException {
        if (line.length() > 4096) throw new IOException("媒体时间戳输出行超出资源上限");
        Map<String, String> fields = new HashMap<>();
        for (String field : line.split("\\|")) {
            String[] pair = field.split("=", 2);
            if (pair.length == 2) fields.put(pair[0], pair[1]);
        }
        return fields;
    }

    /** 只删除已逐帧证明的重叠，目标不足时拒绝，不插帧、不复制最后一帧补时长。 */
    private List<Cut> plan(List<Clip> clips, List<Seam> seams, double requested, Context context) throws IOException {
        int[] begins = new int[clips.size()];
        int[] ends = clips.stream().mapToInt(clip -> clip.frames().size()).toArray();
        for (int index = 1; index < clips.size(); index++) {
            check(context);
            Clip left = clips.get(index - 1);
            int offset = seams.get(index - 1).offset();
            if (offset < begins[index - 1]) throw new IOException("接缝重叠超出前段保留内容");
            int common = left.frames().size() - offset;
            int middle = common / 2;
            ends[index - 1] = offset + middle;
            begins[index] = middle;
        }
        List<Cut> result = new ArrayList<>();
        double elapsed = 0;
        for (int index = 0; index < clips.size() && elapsed + EPSILON < requested; index++) {
            Clip clip = clips.get(index);
            int begin = begins[index];
            int end = ends[index];
            if (end <= begin) throw new IOException("接缝没有剩余真实视频帧");
            double start = clip.frames().get(begin).pts();
            double remaining = requested - elapsed;
            if (endTime(clip, end) - start > remaining + EPSILON) {
                end = begin + 1;
                while (end < ends[index] && endTime(clip, end) - start + EPSILON < remaining) end++;
            }
            double duration = endTime(clip, end) - start;
            if (elapsed + duration > requested + 0.1 + EPSILON) throw new IOException("申请尾部没有足够精细的真实帧边界");
            result.add(new Cut(clip, begin, end));
            elapsed += duration;
        }
        if (elapsed + EPSILON < requested) throw new IOException("完整唯一视频帧不足申请时长");
        return result;
    }

    /** 最长连续公共帧须唯一，且按该offset对齐的整个丢弃重叠都必须逐帧一致。 */
    private Seam align(Clip left, Clip right, Context context) throws IOException {
        List<Frame> a = left.frames();
        List<Frame> b = right.frames();
        int from = 0;
        while (from < a.size() && a.get(from).pts() < a.getLast().pts() - WINDOW_SECONDS) from++;
        int count = 0;
        while (count < b.size() && b.get(count).pts() <= b.getFirst().pts() + WINDOW_SECONDS) count++;
        if (a.size() - from > MAX_WINDOW_FRAMES || count > MAX_WINDOW_FRAMES) throw new IOException("接缝搜索帧数超出资源边界");
        int[] previous = new int[count + 1];
        Map<Integer, Integer> runs = new HashMap<>();
        for (int i = from; i < a.size(); i++) {
            check(context);
            int[] current = new int[count + 1];
            for (int j = 0; j < count; j++) {
                if (a.get(i).hash().equals(b.get(j).hash())) {
                    current[j + 1] = previous[j] + 1;
                    runs.merge(i - j, current[j + 1], Math::max);
                }
            }
            previous = current;
        }
        int best = runs.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> offsets = runs.entrySet().stream().filter(entry -> entry.getValue() == best)
                .map(Map.Entry::getKey).toList();
        if (best < MIN_ANCHOR_FRAMES || offsets.size() != 1) throw new IOException("接缝不存在唯一连续帧锚点");
        int offset = offsets.getFirst();
        if (offset < from || offset < 0 || offset >= a.size() || b.size() <= a.size() - offset) {
            throw new IOException("接缝未证明新分段延续前段");
        }
        int overlap = a.size() - offset;
        if (overlap > count) throw new IOException("接缝完整重叠超出30秒搜索边界");
        for (int index = 0; index < overlap; index++) {
            if (!a.get(offset + index).hash().equals(b.get(index).hash())) throw new IOException("接缝丢弃区含未验证相同的帧");
        }
        return new Seam(offset, best, overlap);
    }

    /** 音轨以实际解码样本核首尾和连续性，允许一帧AAC误差，但不能用容器时长掩盖短尾。 */
    private AudioRange verifyAudio(Path input, Clip video, Context context) throws IOException {
        int sampleRate = video.format().sampleRate();
        if (sampleRate <= 0) throw new IOException("成品音频采样率无效");
        Path output = run(List.of(settings.getFfprobe(), "-v", "error", "-threads", "1",
                "-select_streams", "a:0", "-show_frames", "-show_entries",
                "frame=best_effort_timestamp_time,nb_samples:side_data=", "-of", "compact=p=0:nk=0",
                input.toString()), "audio-frames", context);
        double first = Double.NaN;
        double lastPts = Double.NEGATIVE_INFINITY;
        double end = Double.NaN;
        long samples = 0;
        int frames = 0;
        double tolerance = Math.min(0.15, 1024.0 / sampleRate + 0.002);
        try (BufferedReader reader = Files.newBufferedReader(output)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, String> fields = compactFields(line);
                if (!fields.containsKey("nb_samples")) continue;
                double pts = Double.parseDouble(fields.get("best_effort_timestamp_time"));
                int count = Integer.parseInt(fields.get("nb_samples"));
                if (!Double.isFinite(pts) || count <= 0 || pts <= lastPts
                        || (frames > 0 && (pts < end - 1.0 / sampleRate - 0.000002 || pts > end + tolerance))) {
                    throw new IOException("成品解码音频存在缺口或时间戳异常");
                }
                if (frames == 0) first = pts;
                lastPts = pts;
                end = pts + (double) count / sampleRate;
                samples = Math.addExact(samples, count);
                if (++frames > 2_000_000) throw new IOException("解码音频帧数超出资源上限");
            }
        } catch (NumberFormatException | NullPointerException error) {
            throw new IOException("音频真实样本边界不可验证", error);
        }
        double videoStart = video.frames().getFirst().pts();
        double videoEnd = endTime(video, video.frames().size());
        if (frames == 0 || Math.abs(first - videoStart) > tolerance || Math.abs(end - videoEnd) > tolerance) {
            throw new IOException("成品实际音频首尾未覆盖视频范围");
        }
        return new AudioRange(first, end, frames, samples, sampleRate, tolerance);
    }

    /** 留下不含图像或路由的验证摘要；成品MP4仍是最后一个原子发布步骤。 */
    private Path writeAudit(Path candidate, double requested, long expectedFrames, Clip verified,
                            List<Seam> seams, AudioRange audio, List<AudioSelection> selections, Context context) throws IOException {
        JSONObject result = new JSONObject();
        result.put("requestedSeconds", requested);
        result.put("expectedFrames", expectedFrames);
        result.put("actualFrames", verified.frames().size());
        result.put("videoFirstPts", verified.frames().getFirst().pts());
        result.put("videoEndPts", endTime(verified, verified.frames().size()));
        result.put("containerDuration", verified.containerDuration());
        result.put("completeDecodeVerified", true);
        result.put("packetDtsVerified", true);
        result.put("sha256", sha256(candidate, context));
        JSONArray evidence = new JSONArray();
        for (int index = 0; index < seams.size(); index++) {
            Seam seam = seams.get(index);
            JSONObject entry = new JSONObject();
            entry.put("leftInput", index);
            entry.put("rightInput", index + 1);
            entry.put("offsetFrames", seam.offset());
            entry.put("uniqueAnchorFrames", seam.anchorFrames());
            entry.put("entireOverlapFrames", seam.overlapFrames());
            entry.put("allDiscardedOverlapIdentical", true);
            evidence.add(entry);
        }
        result.put("seams", evidence);
        JSONArray sourceAudio = new JSONArray();
        for (int index = 0; index < selections.size(); index++) {
            AudioSelection selection = selections.get(index);
            JSONObject entry = new JSONObject();
            entry.put("input", index);
            entry.put("decodedSamples", selection.total());
            entry.put("selectedSamples", selection.selected());
            entry.put("discardedOverlapSamples", selection.discarded());
            entry.put("sampleRate", selection.sampleRate());
            entry.put("timestampQuantizationSeconds", 0.0011);
            sourceAudio.add(entry);
        }
        result.put("sourceAudio", sourceAudio);
        if (audio != null) {
            JSONObject range = new JSONObject();
            range.put("firstPts", audio.first());
            range.put("endPts", audio.end());
            range.put("decodedFrames", audio.frames());
            range.put("decodedSamples", audio.samples());
            range.put("sampleRate", audio.sampleRate());
            range.put("toleranceSeconds", audio.tolerance());
            result.put("audio", range);
        }
        result.put("qualification", "仅证明真实帧拼接与解码音视频覆盖，不证明源录像UTC边界或源内OSD时钟连续。");
        Path output = context.staging().resolve("verified-audit.json");
        Files.writeString(output, JSON.toJSONString(result), StandardCharsets.UTF_8);
        return output;
    }

    /** 流式摘要绑定最终文件，定期检查取消，不能一次性把录像加载进堆。 */
    private String sha256(Path path, Context context) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65_536];
            int count;
            int chunks = 0;
            while ((count = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, count);
                if (++chunks % 256 == 0) check(context);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("运行时缺少SHA-256", error);
        }
    }

    /** 视频只保留真实帧；音频另按实际样本PTS裁剪，避免WAV抹去源重叠时间戳。 */
    private AudioSelection render(Cut cut, Path output, Context context) throws IOException {
        List<String> command = new ArrayList<>(List.of(settings.getFfmpeg(), "-nostdin", "-v", "error", "-xerror", "-y",
                "-threads", "1", "-filter_threads", "1", "-copyts", "-protocol_whitelist", "file,pipe", "-i", cut.clip().input().toString(),
                "-map", "0:v:0", "-vf", "trim=start_frame=" + cut.begin() + ":end_frame=" + cut.end() + ",setpts=PTS-STARTPTS",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "18", "-pix_fmt", "yuv420p", "-threads:v", "1",
                "-vsync", "0", "-enc_time_base:v", "1:1000", "-video_track_timescale", "1000",
                "-movflags", "+faststart", output.toString()));
        run(command, output.getFileName().toString(), context);
        return cut.clip().format().audio() ? renderAudio(cut, output, context) : null;
    }

    /** 先解码原样本再按PTS选取字节区间，固定保留先到内容，不补音、不拉伸音频。 */
    private AudioSelection renderAudio(Cut cut, Path video, Context context) throws IOException {
        Format format = cut.clip().format();
        AudioSamplePlan plan = audioSamplePlan(cut, video.getFileName().toString(), context);
        Path raw = video.resolveSibling(video.getFileName() + ".source.f32");
        Path selected = video.resolveSibling(video.getFileName() + ".selected.f32");
        run(List.of(settings.getFfmpeg(), "-nostdin", "-v", "error", "-xerror", "-y", "-threads", "1", "-copyts",
                "-protocol_whitelist", "file,pipe", "-i", cut.clip().input().toString(), "-map", "0:a:0",
                "-c:a", "pcm_f32le", "-ar", Integer.toString(format.sampleRate()), "-ac", Integer.toString(format.channels()),
                "-f", "f32le", raw.toString()), video.getFileName() + "-raw-audio", context);
        long sampleBytes = Math.multiplyExact(4L, format.channels());
        if (Files.size(raw) != Math.multiplyExact(plan.total(), sampleBytes)) {
            throw new IOException("原样PCM字节数与实际解码样本不一致");
        }
        copyAudioSamples(raw, selected, plan.ranges(), sampleBytes, context);
        if (Files.size(selected) != Math.multiplyExact(plan.selected(), sampleBytes)) {
            throw new IOException("裁剪PCM字节数与保留样本不一致");
        }
        run(List.of(settings.getFfmpeg(), "-nostdin", "-v", "error", "-xerror", "-y", "-f", "f32le",
                "-ar", Integer.toString(format.sampleRate()), "-ac", Integer.toString(format.channels()), "-i", selected.toString(),
                "-c:a", "pcm_f32le", "-rf64", "auto", audioPart(video).toString()), video.getFileName() + "-wrap-audio", context);
        Files.delete(raw);
        Files.delete(selected);
        return new AudioSelection(plan.total(), plan.selected(), plan.discarded(), format.sampleRate());
    }

    /** 毫秒源PTS的量化误差仅保留原样本；超过量化范围的真实间隙必须拒绝。 */
    private AudioSamplePlan audioSamplePlan(Cut cut, String label, Context context) throws IOException {
        Path metadata = run(List.of(settings.getFfprobe(), "-v", "error", "-threads", "1", "-select_streams", "a:0",
                "-show_frames", "-show_entries", "frame=best_effort_timestamp_time,nb_samples:side_data=",
                "-of", "compact=p=0:nk=0", cut.clip().input().toString()), label + "-source-audio-frames", context);
        int rate = cut.clip().format().sampleRate();
        if (rate <= 0 || cut.clip().format().channels() <= 0) throw new IOException("源音频格式无效");
        double start = cut.clip().frames().get(cut.begin()).pts();
        double end = endTime(cut.clip(), cut.end());
        double covered = start;
        double previous = Double.NEGATIVE_INFINITY;
        long total = 0;
        long selected = 0;
        long discarded = 0;
        int frames = 0;
        List<SampleRange> ranges = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(metadata)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, String> fields = compactFields(line);
                if (!fields.containsKey("nb_samples")) continue;
                double pts = Double.parseDouble(fields.get("best_effort_timestamp_time"));
                int count = Integer.parseInt(fields.get("nb_samples"));
                if (!Double.isFinite(pts) || pts <= previous || count <= 0 || ++frames > 2_000_000) {
                    throw new IOException("源音频解码样本时间戳无效");
                }
                previous = pts;
                double finish = pts + (double) count / rate;
                if (finish > start && pts < end) {
                    if (pts > covered + 0.0011) throw new IOException("源音频存在真实间隙，禁止补样本");
                    double beginTime = Math.max(start, pts);
                    // WVP/ZLM源时间戳以毫秒量化；微小跳动不改样本，大重叠只剔除已覆盖时间。
                    if (pts < covered - 0.0011) beginTime = Math.max(beginTime, covered);
                    int begin = sampleBoundary(beginTime, pts, rate, count);
                    int until = sampleBoundary(Math.min(end, finish), pts, rate, count);
                    int nominalBegin = sampleBoundary(Math.max(start, pts), pts, rate, count);
                    discarded = Math.addExact(discarded, Math.max(0, Math.min(begin, until) - nominalBegin));
                    if (until > begin) {
                        addSampleRange(ranges, Math.addExact(total, begin), Math.addExact(total, until));
                        selected = Math.addExact(selected, until - begin);
                        covered = Math.max(covered, Math.min(end, finish));
                    }
                }
                total = Math.addExact(total, count);
            }
        } catch (NumberFormatException | NullPointerException error) {
            throw new IOException("源音频样本边界不可验证", error);
        }
        if (selected == 0 || covered < end - 0.0011) throw new IOException("源音频首尾未覆盖视频裁剪范围");
        return new AudioSamplePlan(total, selected, discarded, ranges);
    }

    /** 半开时间边界向上取最近真实样本，只处理元数据浮点舍入而不插入采样点。 */
    private int sampleBoundary(double target, double pts, int rate, int count) {
        return (int) Math.max(0, Math.min(count, Math.ceil((target - pts) * rate - EPSILON)));
    }

    /** 合并相邻物理样本区间，限制高度碎片化输入的内存和磁盘随机访问。 */
    private void addSampleRange(List<SampleRange> ranges, long begin, long end) throws IOException {
        if (!ranges.isEmpty() && ranges.getLast().end() == begin) {
            SampleRange previous = ranges.removeLast();
            ranges.add(new SampleRange(previous.begin(), end));
        } else {
            if (ranges.size() >= 100_000) throw new IOException("音频样本区间超出资源上限");
            ranges.add(new SampleRange(begin, end));
        }
    }

    /** 逐块复制确实存在的原样本，所有临时字节仍受单任务预算与取消信号约束。 */
    private void copyAudioSamples(Path input, Path output, List<SampleRange> ranges, long sampleBytes,
                                 Context context) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        try (FileChannel source = FileChannel.open(input, StandardOpenOption.READ);
             FileChannel target = FileChannel.open(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            for (SampleRange range : ranges) {
                source.position(Math.multiplyExact(range.begin(), sampleBytes));
                long remaining = Math.multiplyExact(range.end() - range.begin(), sampleBytes);
                while (remaining > 0) {
                    check(context);
                    buffer.clear();
                    buffer.limit((int) Math.min(buffer.capacity(), remaining));
                    int read = source.read(buffer);
                    if (read <= 0) throw new IOException("原音频样本文件提前结束");
                    remaining -= read;
                    buffer.flip();
                    while (buffer.hasRemaining()) target.write(buffer);
                }
            }
        }
    }

    /** 音频文件名完全由自有part序号派生，concat清单不拼入任何输入文件名。 */
    private Path audioPart(Path video) {
        return video.resolveSibling(video.getFileName().toString().replace(".mp4", ".wav"));
    }

    /** 显示PTS在全解码阶段检查，压缩包只要求每个流DTS严格递增，不能误拒正常B帧PTS重排。 */
    private void verifyPackets(Path input, Context context) throws IOException {
        Path packets = run(List.of(settings.getFfprobe(), "-v", "error", "-show_packets", "-show_entries",
                "packet=stream_index,pts_time,dts_time:side_data=", "-of", "compact=p=0:nk=0", input.toString()), "packets", context);
        Map<Integer, Double> last = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(packets)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                Map<String, String> values = compactFields(line);
                if (!values.containsKey("stream_index")) continue;
                try {
                    int stream = Integer.parseInt(values.get("stream_index"));
                    double dts = Double.parseDouble(values.get("dts_time"));
                    double pts = Double.parseDouble(values.get("pts_time"));
                    if (!Double.isFinite(dts) || !Double.isFinite(pts)
                            || (last.containsKey(stream) && dts <= last.get(stream))) throw new IOException("成品压缩包DTS不递增");
                    last.put(stream, dts);
                } catch (NumberFormatException | NullPointerException error) {
                    throw new IOException("成品压缩包时间戳缺失", error);
                }
            }
        }
        if (last.isEmpty()) throw new IOException("成品没有可验证压缩包");
    }

    /** 帧边界使用后一真实PTS，只有片段末帧才采用解码器报告的持续时间。 */
    private double endTime(Clip clip, int end) {
        return end < clip.frames().size() ? clip.frames().get(end).pts()
                : clip.frames().getLast().pts() + clip.frames().getLast().duration();
    }

    /** 工具输出只落到自有staging，stderr以固定尾部缓冲读取，超时或取消会终止进程树。 */
    private Path run(List<String> command, String label, Context context) throws IOException {
        check(context);
        Path stdout = context.staging().resolve(label + ".out");
        Process process = new ProcessBuilder(command).redirectOutput(stdout.toFile()).start();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        Thread drain = new Thread(() -> drain(process.getErrorStream(), errors), "record-download-tool-log");
        drain.setDaemon(true);
        drain.start();
        try {
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) check(context);
            check(context);
            drain.join(1000);
            if (process.exitValue() != 0) {
                synchronized (errors) {
                    throw new IOException("录像处理工具失败: " + errors.toString(StandardCharsets.UTF_8));
                }
            }
            return stdout;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CancellationException("录像合并被中断");
        } finally {
            if (process.isAlive()) {
                process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                try { process.waitFor(1, TimeUnit.SECONDS); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            try { process.getErrorStream().close(); } catch (IOException ignored) { /* 日志不能阻止终止。 */ }
        }
    }

    /** 持续排空stderr但只保存最后32KB，防止坏媒体或工具异常撑爆堆内存。 */
    private void drain(InputStream input, ByteArrayOutputStream output) {
        try (input) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                synchronized (output) {
                    byte[] old = output.toByteArray();
                    output.reset();
                    int keep = Math.min(old.length, MAX_LOG_BYTES - length);
                    output.write(old, old.length - keep, keep);
                    output.write(buffer, 0, length);
                }
            }
        } catch (IOException ignored) { /* 工具被销毁时日志管道关闭属于正常收尾。 */ }
    }

    /** 在所有耗时阶段检查取消、总截止时间及输入加临时文件的空间预算。 */
    private void check(Context context) throws IOException {
        if (Thread.currentThread().isInterrupted() || context.cancelled().getAsBoolean()) throw new CancellationException("录像合并已取消");
        if (System.nanoTime() >= context.deadline()) throw new IOException("录像合并处理超时");
        long bytes = context.sourceBytes();
        try (var files = Files.walk(context.root())) {
            for (Path file : files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)).toList()) {
                long size = Files.size(file);
                if (file.getFileName().toString().endsWith(".out") && size > MAX_METADATA_BYTES) {
                    throw new IOException("录像工具元数据输出超出资源上限");
                }
                bytes = Math.addExact(bytes, size);
            }
        }
        if (bytes > settings.getMaxJobBytes()) throw new IOException("录像合并超出任务空间预算");
    }

    /** 数字只按固定小数点格式写入工具参数，禁止环境语言影响时间值。 */
    private String number(double value) {
        return String.format(Locale.ROOT, "%.9f", value);
    }

    /** 仅删除本次随机staging中的临时文件，失败时保留输入和已有成品。 */
    private void clean(Path directory) {
        if (directory == null) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { /* 任务目录后续由原保留期清理owner处理。 */ }
    }

    /** 只在内存保存显示时间与摘要，不持有视频像素。 */
    private record Frame(double pts, double duration, String hash) {}
    /** 源文件与解码结构共同参与接缝校验。 */
    private record Clip(Path input, List<Frame> frames, Format format, double containerDuration) {}
    /** 各段使用同一音视频结构，禁止在拼接时隐式丢失音轨。 */
    private record Format(int width, int height, boolean audio, int sampleRate, int channels) {}
    /** 裁剪边界是解码帧索引，结束索引不包含该帧。 */
    private record Cut(Clip clip, int begin, int end) {}
    /** 接缝计数只在整个对齐重叠逐帧验证后产生。 */
    private record Seam(int offset, int anchorFrames, int overlapFrames) {}
    /** 音轨首尾由实际解码样本数推导，不读取容器申报跨度替代。 */
    private record AudioRange(double first, double end, int frames, long samples, int sampleRate, double tolerance) {}
    /** 物理样本索引采用半开区间，不包含任何生成的补齐音频。 */
    private record SampleRange(long begin, long end) {}
    /** 源样本计数用于校验解码字节量，丢弃计数只包含已覆盖的时间重叠。 */
    private record AudioSamplePlan(long total, long selected, long discarded, List<SampleRange> ranges) {}
    /** 最终审计记录每个保留片段的真实音频处理量。 */
    private record AudioSelection(long total, long selected, long discarded, int sampleRate) {}
    /** 所有子进程共享单一取消信号、截止时间与磁盘预算。 */
    private record Context(Path root, Path staging, long sourceBytes, long deadline, BooleanSupplier cancelled) {}
}
