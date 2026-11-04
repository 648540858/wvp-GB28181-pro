package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 用真实本机HTTP与临时磁盘证明源地址约束、令牌边界和清理隔离。 */
@SuppressWarnings("unchecked")
class SegmentedArtifactFilesTest {
    @TempDir Path root;
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SegmentedRecordingAssembler assembler = mock(SegmentedRecordingAssembler.class);
    private final Map<String, String> metadata = new HashMap<>();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger responseCode = new AtomicInteger(200);
    private final AtomicReference<String> query = new AtomicReference<>();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    private SegmentedDownloadSettings settings;
    private SegmentedArtifactFiles files;
    private MediaServer media;
    private HttpServer http;

    /** 令牌写入使用可观察内存Redis，源文件通过真实固定端点提供。 */
    @BeforeEach
    void setUp() throws Exception {
        settings = new SegmentedDownloadSettings(); settings.setDirectory(root.resolve("jobs").toString());
        settings.setPublicBaseUrl("https://zlm.example.test/record-download");
        UserSetting user = new UserSetting(); user.setServerId("artifact-test");
        files = new SegmentedArtifactFiles(settings, assembler, redis, user, clock);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(call -> metadata.get(call.getArgument(0)));
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(call ->
                metadata.putIfAbsent(call.getArgument(0), call.getArgument(1)) == null);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenAnswer(call -> {
            String key = ((List<String>) call.getArgument(1)).get(0);
            Object expected = call.getArgument(2);
            return metadata.remove(key, expected) ? 1L : 0L;
        });
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/index/api/downloadFile", exchange -> {
            requests.incrementAndGet(); query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = new byte[]{0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
            exchange.getResponseHeaders().set("Content-Type", "video/mp4");
            if (responseCode.get() == 302) exchange.getResponseHeaders().set("Location", "http://127.0.0.1:1/private");
            exchange.sendResponseHeaders(responseCode.get(), body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        http.start();
        media = new MediaServer(); media.setIp("127.0.0.1"); media.setHttpPort(http.getAddress().getPort());
        media.setRecordPath("/record");
        when(assembler.assemble(anyList(), anyDouble(), any(Path.class), any(BooleanSupplier.class))).thenAnswer(call -> {
            Path output = ((Path) call.getArgument(2)).resolve("verified.mp4");
            Files.write(output, new byte[]{0, 0, 0, 12, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
            return output;
        });
    }

    /** 只停止本测试创建的本地HTTP服务。 */
    @AfterEach
    void tearDown() { if (http != null) http.stop(0); }

    /** 构造规范任务，原URL故意使用不可访问主机以证明不会跟随该主机。 */
    private SegmentedDownloadTask task() {
        SegmentedDownloadTask task = new SegmentedDownloadTask();
        task.setId(UUID.randomUUID().toString()); task.setServerId("artifact-test");
        task.setArtifactToken("a".repeat(64)); task.setState(SegmentedDownloadTask.State.MERGING);
        task.setStartTime("2026-10-28 09:20:40"); task.setEndTime("2026-10-28 09:21:40");
        DownloadFileInfo source = new DownloadFileInfo();
        source.setHttpPath("http://untrusted.invalid/index/api/downloadFile?secret=ignored&file_path=%2Frecord%2Fday%2Fa.mp4");
        StreamInfo info = new StreamInfo(); info.setDownLoadFilePath(source);
        SegmentedDownloadTask.Segment segment = new SegmentedDownloadTask.Segment(); segment.setInfo(info);
        task.setSegments(List.of(segment)); return task;
    }

    /** 实际请求只能到指定媒体节点且只转发file_path，发布结果可按原SDK契约读取。 */
    @Test
    void copiesFromFixedMediaAndPublishesOnlyOwnedArtifact() throws Exception {
        SegmentedDownloadTask task = task();
        Path artifact = files.assemble(task, media, () -> false);
        assertEquals("artifact.mp4", artifact.getFileName().toString());
        assertEquals(1, requests.get()); assertEquals("file_path=%2Frecord%2Fday%2Fa.mp4", query.get());
        verify(assembler).assemble(eq(List.of(artifact.resolveSibling("source-0.mp4"))), eq(60.0), eq(artifact.getParent()), any());
        DownloadFileInfo published = files.publish(task, artifact);
        assertEquals("https://zlm.example.test/record-download/" + task.getArtifactToken() + ".mp4", published.getHttpsPath());
        assertEquals(artifact, files.resolve(task.getArtifactToken()));
        assertEquals(3, JSON.parseObject(metadata.values().iterator().next()).size());
    }

    /** 部分录像也只能使用同样的受限源地址，不能改为任意远端URL。 */
    @Test
    void acceptsExistingPartialFileContract() {
        SegmentedDownloadTask task = task(); StreamInfo info = task.getSegments().get(0).getInfo();
        info.setPartialDownloadFilePath(info.getDownLoadFilePath()); info.setDownLoadFilePath(null);
        assertNotNull(files.assemble(task, media, () -> false)); assertEquals(1, requests.get());
    }

    /** 重复参数、双重编码、路径逃逸与非ZLM接口在发起HTTP前全部拒绝。 */
    @Test
    void rejectsUnsafeSourcePathsBeforeNetwork() {
        for (String suffix : List.of("/index/api/downloadFile?file_path=%2Frecord%2Fa.mp4&file%5Fpath=%2Frecord%2Fb.mp4",
                "/index/api/downloadFile?file_path=%252Frecord%252Fa.mp4", "/index/api/downloadFile?file_path=%2Frecord%2F..%2Fprivate.mp4",
                "/index/api/downloadFile?file_path=%2Fetc%2Fpasswd.mp4", "/arbitrary.mp4?file_path=%2Frecord%2Fa.mp4")) {
            SegmentedDownloadTask task = task();
            task.getSegments().get(0).getInfo().getDownLoadFilePath().setHttpPath("http://untrusted.invalid" + suffix);
            assertThrows(RuntimeException.class, () -> files.assemble(task, media, () -> false));
        }
        assertEquals(0, requests.get()); verifyNoInteractions(assembler);
    }

    /** ZLM跳转不能把内部下载变成外部HTTP请求，超限子段也不能进入合并器。 */
    @Test
    void rejectsRedirectAndCumulativeByteOverflow() {
        responseCode.set(302);
        assertThrows(IllegalStateException.class, () -> files.assemble(task(), media, () -> false));
        responseCode.set(200); settings.setMaxJobBytes(20);
        SegmentedDownloadTask task = task(); task.setSegments(List.of(task.getSegments().get(0), task.getSegments().get(0)));
        assertThrows(IllegalStateException.class, () -> files.assemble(task, media, () -> false));
        verifyNoInteractions(assembler);
        assertFalse(Files.exists(root.resolve("jobs").resolve(task.getId()).resolve("source-1.mp4.part")));
    }

    /** 已取消任务不取回数据，也不能将已有本地文件转成新下载令牌。 */
    @Test
    void cancellationStopsCopyAndPublication() throws Exception {
        SegmentedDownloadTask task = task();
        assertThrows(CancellationException.class, () -> files.assemble(task, media, () -> true));
        assertEquals(0, requests.get());
        Path artifact = files.assemble(task, media, () -> false);
        task.setCancelRequested(true);
        assertThrows(CancellationException.class, () -> files.publish(task, artifact)); assertTrue(metadata.isEmpty());
    }

    /** 合并器完成时父任务若已取消，核验临时文件不能被改名发布。 */
    @Test
    void cancellationAfterAssemblyDoesNotExposeArtifact() throws Exception {
        SegmentedDownloadTask task = task();
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(assembler.assemble(anyList(), anyDouble(), any(Path.class), any(BooleanSupplier.class))).thenAnswer(call -> {
            Path verified = Files.write(((Path) call.getArgument(2)).resolve("verified.mp4"), new byte[]{1});
            cancelled.set(true); return verified;
        });
        assertThrows(CancellationException.class, () -> files.assemble(task, media, cancelled::get));
        assertFalse(Files.exists(root.resolve("jobs").resolve(task.getId()).resolve("artifact.mp4")));
        assertTrue(metadata.isEmpty());
    }

    /** 有效令牌仍需本节点、未到期、规范任务目录和普通文件同时成立。 */
    @Test
    void rejectsExpiredForeignCorruptAndSymlinkedArtifacts() throws Exception {
        SegmentedDownloadTask task = task(); Path artifact = files.assemble(task, media, () -> false); files.publish(task, artifact);
        String key = metadata.keySet().iterator().next(); String original = metadata.get(key);
        JSONObject value = JSON.parseObject(original); value.put("expiresAt", clock.millis()); metadata.put(key, value.toJSONString());
        assertNull(files.resolve(task.getArtifactToken()));
        value = JSON.parseObject(original); value.put("serverId", "other"); metadata.put(key, value.toJSONString());
        assertNull(files.resolve(task.getArtifactToken()));
        value = JSON.parseObject(original); value.put("taskId", "../../etc"); metadata.put(key, value.toJSONString());
        assertNull(files.resolve(task.getArtifactToken()));
        metadata.put(key, original); Files.delete(artifact); Path outside = Files.write(root.resolve("private.mp4"), new byte[]{1});
        Files.createSymbolicLink(artifact, outside); assertNull(files.resolve(task.getArtifactToken()));
        verify(values, never()).get(contains("../")); assertNull(files.resolve("../private"));
    }

    /** 清理只撤销本轮令牌和私有树，子项符号链接不影响其他目录中的文件。 */
    @Test
    void cleanupIsIsolatedAndIdempotent() throws Exception {
        SegmentedDownloadTask task = task(); Path artifact = files.assemble(task, media, () -> false); files.publish(task, artifact);
        Path outside = Files.write(root.resolve("keep.mp4"), new byte[]{7});
        Files.createSymbolicLink(artifact.resolveSibling("outside-link"), outside);
        files.cleanup(task); files.cleanup(task);
        assertFalse(Files.exists(artifact.getParent())); assertTrue(Files.exists(outside)); assertTrue(metadata.isEmpty());
    }

    /** 连续任务累计成品必须计入总预算，当前任务失败不删除先前可读取的成品。 */
    @Test
    void totalBudgetIncludesEarlierArtifactsAndRejectsNextMerge() throws Exception {
        settings.setMaxTotalBytes(48);
        java.util.ArrayList<SegmentedDownloadTask> completed = new java.util.ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            SegmentedDownloadTask task = task(); task.setArtifactToken(String.format("%064x", index));
            Path artifact = files.assemble(task, media, () -> false); files.publish(task, artifact); completed.add(task);
            assertFalse(Files.exists(artifact.resolveSibling("source-0.mp4")), "已公开成品不保留原片");
        }
        SegmentedDownloadTask rejected = task(); rejected.setArtifactToken("b".repeat(64));
        assertThrows(IllegalStateException.class, () -> files.assemble(rejected, media, () -> false));
        assertFalse(Files.exists(root.resolve("jobs").resolve(rejected.getId()).resolve("artifact.mp4")));
        assertNull(files.resolve(rejected.getArtifactToken()));
        files.cleanup(rejected);
        for (SegmentedDownloadTask task : completed) assertNotNull(files.resolve(task.getArtifactToken()));
        assertEquals(3, metadata.size());
    }

    /** 其他任务的临时文件同样占预算，不能只计算已完成MP4或本任务子文件。 */
    @Test
    void rejectsCopyWhenAnotherJobsTemporaryFileConsumesCapacity() throws Exception {
        settings.setMaxTotalBytes(20);
        Path other = Files.createDirectories(root.resolve("jobs").resolve(UUID.randomUUID().toString()).resolve(".assembly-test"));
        Files.write(other.resolve("tool.out"), new byte[12]);
        assertThrows(IllegalStateException.class, () -> files.assemble(task(), media, () -> false));
        verifyNoInteractions(assembler);
        assertEquals(12, Files.size(other.resolve("tool.out")));
    }

    /** 将跨任务预算检查传入合并器，使工具运行期间新增输出也能立即触发中止。 */
    @Test
    void assemblerGuardRejectsGrowingTemporaryOutput() throws Exception {
        settings.setMaxTotalBytes(24);
        when(assembler.assemble(anyList(), anyDouble(), any(Path.class), any(BooleanSupplier.class))).thenAnswer(call -> {
            Path work = call.getArgument(2);
            Path staging = Files.createDirectory(work.resolve(".assembly-test"));
            Files.write(staging.resolve("growing.mp4"), new byte[13]);
            ((BooleanSupplier) call.getArgument(3)).getAsBoolean();
            fail("总预算已超出，合并器回调必须中止"); return null;
        });
        SegmentedDownloadTask task = task();
        assertThrows(IllegalStateException.class, () -> files.assemble(task, media, () -> false));
        assertNull(files.resolve(task.getArtifactToken()));
        assertFalse(Files.exists(root.resolve("jobs").resolve(task.getId()).resolve("artifact.mp4")));
    }

    /** 真实工具进程持续运行时若输出超过全根预算，回调必须终止进程而不留下后台任务。 */
    @Test
    void capacityFailureTerminatesActualAssemblerToolProcess() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.FileSystems.getDefault()
                .supportedFileAttributeViews().contains("posix"));
        Path tool = root.resolve("fake-ffprobe.sh");
        Files.writeString(tool, "#!/bin/sh\necho $$ > \"$0.pid\"\nhead -c 2048 /dev/zero\nexec sleep 30\n");
        Files.setPosixFilePermissions(tool, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        settings.setFfprobe(tool.toString()); settings.setMaxTotalBytes(1024);
        UserSetting user = new UserSetting(); user.setServerId("artifact-test");
        SegmentedArtifactFiles actual = new SegmentedArtifactFiles(settings, new SegmentedRecordingAssembler(settings), redis, user, clock);
        assertThrows(IllegalStateException.class, () -> actual.assemble(task(), media, () -> false));
        long pid = Long.parseLong(Files.readString(Path.of(tool + ".pid")).trim());
        ProcessHandle process = ProcessHandle.of(pid).orElse(null);
        try { assertTrue(process == null || !process.isAlive(), "预算中止后工具进程必须已退出"); }
        finally { if (process != null && process.isAlive()) process.destroyForcibly(); }
    }

    /** 总量统计必须拒绝链接和超过有限深度的目录，不能漏算不可安全遍历的用量。 */
    @Test
    void capacityScanRejectsSymlinksAndExcessiveDepth() throws Exception {
        Path other = Files.createDirectories(root.resolve("jobs").resolve(UUID.randomUUID().toString()));
        Path outside = Files.write(root.resolve("outside.mp4"), new byte[]{1});
        Path link = Files.createSymbolicLink(other.resolve("link"), outside);
        assertThrows(IllegalStateException.class, () -> files.assemble(task(), media, () -> false));
        Files.delete(link);
        Path nested = other;
        for (int index = 0; index < 8; index++) nested = Files.createDirectory(nested.resolve("nested"));
        assertThrows(IllegalStateException.class, () -> files.assemble(task(), media, () -> false));
        assertEquals(0, requests.get()); assertTrue(Files.exists(outside));
    }

    /** 发布只删除本轮已知源文件与未公开核验文件，保留成品、诊断文件和其他任务。 */
    @Test
    void publicationDiscardsSourcesWithoutDeletingArtifactOrOtherJobs() throws Exception {
        SegmentedDownloadTask task = task(); Path artifact = files.assemble(task, media, () -> false);
        Path diagnostics = Files.write(artifact.resolveSibling("diagnostics.json"), new byte[]{1});
        Path staleVerified = Files.write(artifact.resolveSibling("verified.mp4"), new byte[]{2});
        Path another = Files.createDirectories(root.resolve("jobs").resolve(UUID.randomUUID().toString()));
        Path anotherSource = Files.write(another.resolve("source-0.mp4"), new byte[]{3});
        files.publish(task, artifact);
        assertNotNull(files.resolve(task.getArtifactToken())); assertTrue(Files.exists(artifact));
        assertFalse(Files.exists(artifact.resolveSibling("source-0.mp4"))); assertFalse(Files.exists(staleVerified));
        assertTrue(Files.exists(diagnostics)); assertTrue(Files.exists(anotherSource));
    }

    /** 不允许发布其他轮次文件，也不允许覆盖已经归属另一轮次的同名令牌。 */
    @Test
    void publicationRejectsForeignFileAndTokenCollision() throws Exception {
        SegmentedDownloadTask first = task(); Path artifact = files.assemble(first, media, () -> false); files.publish(first, artifact);
        SegmentedDownloadTask second = task(); Path secondArtifact = files.assemble(second, media, () -> false);
        assertThrows(IllegalStateException.class, () -> files.publish(second, artifact));
        assertThrows(IllegalStateException.class, () -> files.publish(second, secondArtifact));
        assertEquals(artifact, files.resolve(first.getArtifactToken()));
    }
}
