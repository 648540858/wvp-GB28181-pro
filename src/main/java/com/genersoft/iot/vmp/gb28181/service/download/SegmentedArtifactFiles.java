package com.genersoft.iot.vmp.gb28181.service.download;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** 将可信媒体节点上的短段取回本机，核验合并后以有限期随机令牌交付成品。 */
@Component
public class SegmentedArtifactFiles {
    private static final String TOKEN_PREFIX = "WVP_SEGMENTED_ARTIFACT_";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DefaultRedisScript<Long> REVOKE = new DefaultRedisScript<>(
            "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", Long.class);
    private final SegmentedDownloadSettings settings;
    private final SegmentedRecordingAssembler assembler;
    private final StringRedisTemplate redis;
    private final String serverId;
    private final Clock clock;
    private final Object[] locks = new Object[64];

    /** 注入现有Redis和节点身份；关闭兼容策略时不创建任何文件或目录。 */
    @Autowired
    public SegmentedArtifactFiles(SegmentedDownloadSettings settings, SegmentedRecordingAssembler assembler,
                                  StringRedisTemplate redis, UserSetting userSetting) {
        this(settings, assembler, redis, userSetting, Clock.systemUTC());
    }

    /** 允许测试固定令牌到期时间，不改变生产使用的系统时钟。 */
    SegmentedArtifactFiles(SegmentedDownloadSettings settings, SegmentedRecordingAssembler assembler,
                           StringRedisTemplate redis, UserSetting userSetting, Clock clock) {
        this.settings = settings;
        this.assembler = assembler;
        this.redis = redis;
        this.serverId = userSetting.getServerId();
        this.clock = clock;
        if (serverId == null || serverId.isBlank()) throw new IllegalStateException("缺少成品所属节点身份");
        Arrays.setAll(locks, ignored -> new Object());
    }

    /** 仅从任务所属ZLM取回文件，累计限制源文件字节，再执行完整性核验与合并。 */
    public Path assemble(SegmentedDownloadTask task, MediaServer media, BooleanSupplier cancelled) {
        validateTask(task);
        synchronized (lock(task.getId())) {
            try {
                checkCancelled(task, cancelled);
                Path directory = jobDirectory(task.getId(), true);
                checkGlobalBudget(0);
                long copied = 0;
                List<Path> sources = new ArrayList<>();
                if (task.getSegments() == null || task.getSegments().isEmpty()) {
                    throw new IllegalStateException("没有可合并的录像分段");
                }
                for (int index = 0; index < task.getSegments().size(); index++) {
                    SegmentedDownloadTask.Segment segment = task.getSegments().get(index);
                    if (segment == null || segment.getInfo() == null) throw new IllegalStateException("录像分段信息不完整");
                    DownloadFileInfo info = segment.getInfo().getDownLoadFilePath();
                    if (info == null) info = segment.getInfo().getPartialDownloadFilePath();
                    String filePath = sourceFilePath(info, media);
                    Path source = directory.resolve("source-" + index + ".mp4");
                    copied = copySource(task, media, filePath, source, copied, cancelled);
                    sources.add(source);
                }
                double requested = Duration.between(LocalDateTime.parse(task.getStartTime(), TIME),
                        LocalDateTime.parse(task.getEndTime(), TIME)).toMillis() / 1000.0;
                if (requested <= 0 || requested > settings.getMaxRequestSeconds()) {
                    throw new IllegalStateException("录像申请区间超出边界");
                }
                // 工具进程每100ms检查此回调，跨任务临时文件与已完成成品都计入同一根目录预算。
                BooleanSupplier guarded = () -> {
                    checkCancelled(task, cancelled);
                    checkGlobalBudget(0);
                    return false;
                };
                Path verified = assembler.assemble(sources, requested, directory, guarded);
                checkCancelled(task, cancelled);
                checkGlobalBudget(0);
                requireOwnedFile(directory, verified);
                Path artifact = directory.resolve("artifact.mp4");
                if (!verified.equals(artifact)) {
                    // ATOMIC_MOVE在部分文件系统可能替换目标，先拒绝已有成品以保留原轮次证据。
                    if (Files.exists(artifact, LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("录像成品已存在");
                    Files.move(verified, artifact, StandardCopyOption.ATOMIC_MOVE);
                }
                return artifact;
            } catch (IOException ex) {
                throw new IllegalStateException("录像成品准备失败", ex);
            }
        }
    }

    /** 只发布本轮核验完的固定成品，不在Redis中保存可被改写为任意路径的文件名。 */
    public DownloadFileInfo publish(SegmentedDownloadTask task, Path verified) {
        validateTask(task);
        validateToken(task.getArtifactToken());
        synchronized (lock(task.getId())) {
            checkCancelled(task, () -> false);
            try {
                Path directory = jobDirectory(task.getId(), false);
                Path artifact = directory.resolve("artifact.mp4");
                requireOwnedFile(directory, verified);
                if (!artifact.equals(verified.toAbsolutePath().normalize()) || Files.size(artifact) <= 0) {
                    throw new IllegalStateException("录像成品尚未核验");
                }
                String base = settings.getPublicBaseUrl();
                URI uri = URI.create(base == null ? "" : base);
                if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                        || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                        || !"/record-download".equals(uri.getRawPath()) || settings.getRetentionSeconds() <= 0) {
                    throw new IllegalStateException("录像成品出口配置无效");
                }
                long expires = Math.addExact(clock.millis(), settings.getRetentionSeconds() * 1000L);
                JSONObject metadata = new JSONObject();
                metadata.put("taskId", task.getId()); metadata.put("serverId", serverId); metadata.put("expiresAt", expires);
                String key = TOKEN_PREFIX + task.getArtifactToken();
                if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, metadata.toJSONString(),
                        Duration.ofSeconds(settings.getRetentionSeconds())))) {
                    JSONObject existing = readMetadata(redis.opsForValue().get(key));
                    if (!belongsTo(existing, task.getId())) throw new IllegalStateException("录像成品令牌不可复用");
                }
                // 令牌与成品身份均已确认后立即释放原始短段，24小时保留期只保留可交付成品。
                discardSources(directory);
                DownloadFileInfo result = new DownloadFileInfo();
                String url = base + "/" + task.getArtifactToken() + ".mp4";
                if ("https".equals(uri.getScheme())) {
                    result.setHttpsPath(url); result.setHttpsDomainPath(url);
                } else {
                    result.setHttpPath(url); result.setHttpDomainPath(url);
                }
                return result;
            } catch (IOException ex) {
                throw new IllegalStateException("录像成品发布失败", ex);
            }
        }
    }

    /** 匿名请求只能凭有效令牌读取本节点固定成品；缺失和过期均不透露任务身份。 */
    public Path resolve(String token) {
        if (!validToken(token)) return null;
        JSONObject metadata = readMetadata(redis.opsForValue().get(TOKEN_PREFIX + token));
        if (metadata == null || !serverId.equals(metadata.getString("serverId"))
                || metadata.getLongValue("expiresAt") <= clock.millis()) return null;
        String id = metadata.getString("taskId");
        if (!validId(id)) return null;
        synchronized (lock(id)) {
            try {
                Path directory = jobDirectory(id, false);
                Path artifact = directory.resolve("artifact.mp4");
                requireOwnedFile(directory, artifact);
                return Files.size(artifact) > 0 ? artifact : null;
            } catch (IOException | IllegalStateException ex) {
                return null;
            }
        }
    }

    /** 先撤销本轮令牌再删除私有目录；比较删除避免误撤销其他任务的令牌。 */
    public void cleanup(SegmentedDownloadTask task) {
        validateTask(task);
        synchronized (lock(task.getId())) {
            RuntimeException revokeFailure = null;
            if (validToken(task.getArtifactToken())) {
                try {
                    String key = TOKEN_PREFIX + task.getArtifactToken();
                    String raw = redis.opsForValue().get(key);
                    if (belongsTo(readMetadata(raw), task.getId())) redis.execute(REVOKE, List.of(key), raw);
                } catch (RuntimeException ex) { revokeFailure = ex; }
            }
            try {
                Path directory = jobDirectory(task.getId(), false);
                Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                    /** 不跟随子项符号链接，只删除本轮私有树中的目录项。 */
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file); return FileVisitResult.CONTINUE;
                    }
                    /** 在全部子项处理完毕后删除本轮目录。 */
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                        if (error != null) throw error;
                        Files.delete(dir); return FileVisitResult.CONTINUE;
                    }
                });
            } catch (NoSuchFileException ignored) {
                // 重复清理和本机重启后目录丢失都已达到不可读取的目标。
            } catch (IOException ex) {
                throw new IllegalStateException("录像临时文件清理失败", ex);
            }
            if (revokeFailure != null) throw new IllegalStateException("录像成品令牌撤销失败", revokeFailure);
        }
    }

    /** 固定锁槽避免每轮任务留下永久锁对象，同时串行化同一目录的发布与删除。 */
    private Object lock(String id) { return locks[(id.hashCode() & Integer.MAX_VALUE) % locks.length]; }

    /** 严格验证本节点任务身份，禁止将外部路径拼接为工作目录。 */
    private void validateTask(SegmentedDownloadTask task) {
        if (task == null || !validId(task.getId()) || !serverId.equals(task.getServerId())) {
            throw new IllegalStateException("录像任务身份无效");
        }
    }

    /** UUID使用规范小写形式，避免同一目录存在多个逻辑身份。 */
    private boolean validId(String id) {
        try { return id != null && UUID.fromString(id).toString().equals(id); }
        catch (IllegalArgumentException ex) { return false; }
    }

    /** 限制成品令牌为256位随机值的固定十六进制表示。 */
    private boolean validToken(String token) { return token != null && token.matches("[0-9a-f]{64}"); }

    /** 发布时拒绝无效令牌，而匿名查询统一返回不存在。 */
    private void validateToken(String token) {
        if (!validToken(token)) throw new IllegalStateException("录像成品令牌无效");
    }

    /** 创建或读取本轮私有目录，不允许任务目录通过符号链接逃出管理员指定根目录。 */
    private Path jobDirectory(String id, boolean create) throws IOException {
        Path configured = Path.of(settings.getDirectory()).toAbsolutePath().normalize();
        if (create) Files.createDirectories(configured);
        Path root = configured.toRealPath();
        Path directory = root.resolve(id);
        if (create && !Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(directory);
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException("录像任务目录不存在");
            throw new IllegalStateException("录像任务目录无效");
        }
        if (!directory.toRealPath().getParent().equals(root)) throw new IllegalStateException("录像任务目录越界");
        return directory;
    }

    /** 文件必须位于本轮工作目录且不是符号链接，不能交付其他任务或系统文件。 */
    private void requireOwnedFile(Path directory, Path file) throws IOException {
        if (file == null || !file.toAbsolutePath().normalize().getParent().equals(directory)
                || Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !file.toRealPath().getParent().equals(directory)) throw new IllegalStateException("录像成品路径无效");
    }

    /** 只解码一次file_path并限制在该媒体节点录像根目录内；原URL主机永远不参与请求。 */
    private String sourceFilePath(DownloadFileInfo info, MediaServer media) {
        if (info == null || media == null || media.getRecordPath() == null) throw new IllegalStateException("录像源信息不完整");
        String path = null;
        for (String candidate : Arrays.asList(info.getHttpPath(), info.getHttpsPath(), info.getHttpDomainPath(), info.getHttpsDomainPath())) {
            if (candidate == null || candidate.isBlank()) continue;
            URI uri;
            try { uri = URI.create(candidate); } catch (IllegalArgumentException ex) { throw new IllegalStateException("录像源地址无效"); }
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || !"/index/api/downloadFile".equals(uri.getRawPath()) || uri.getRawQuery() == null) {
                throw new IllegalStateException("录像源地址不受支持");
            }
            String found = null;
            for (String parameter : uri.getRawQuery().split("&", -1)) {
                String[] pair = parameter.split("=", 2);
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                if (!"file_path".equals(key)) continue;
                if (found != null || pair.length != 2) throw new IllegalStateException("录像源文件参数不唯一");
                found = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
            if (found == null || found.contains("%") || found.contains("\\") || found.chars().anyMatch(c -> c < 32 || c == 127)) {
                throw new IllegalStateException("录像源文件参数无效");
            }
            Path root = Path.of(media.getRecordPath()).normalize();
            Path source = Path.of(found);
            if (!root.isAbsolute() || !source.isAbsolute() || !source.normalize().equals(source)
                    || !source.startsWith(root) || source.equals(root) || !found.endsWith(".mp4")) {
                throw new IllegalStateException("录像源文件超出录像目录");
            }
            if (path != null && !path.equals(found)) throw new IllegalStateException("录像源文件身份不一致");
            path = found;
        }
        if (path == null) throw new IllegalStateException("缺少录像源文件地址");
        return path;
    }

    /** 有限超时且禁止跳转，从固定ZLM端点流式取回文件并计算整轮实际磁盘开销。 */
    private long copySource(SegmentedDownloadTask task, MediaServer media, String remote, Path source,
                            long total, BooleanSupplier cancelled) throws IOException {
        checkCancelled(task, cancelled);
        checkGlobalBudget(0);
        if (media.getIp() == null || media.getHttpPort() <= 0 || media.getHttpPort() > 65535) {
            throw new IllegalStateException("媒体节点下载端点无效");
        }
        URI base;
        try { base = new URI("http", null, media.getIp(), media.getHttpPort(), "/index/api/downloadFile", null, null); }
        catch (Exception ex) { throw new IllegalStateException("媒体节点下载端点无效"); }
        HttpURLConnection connection = (HttpURLConnection) URI.create(base + "?file_path="
                + URLEncoder.encode(remote, StandardCharsets.UTF_8)).toURL().openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(30000); connection.setInstanceFollowRedirects(false);
        Path partial = source.resolveSibling(source.getFileName() + ".part");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("媒体文件未返回完整响应");
            long announced = connection.getContentLengthLong();
            long remaining = settings.getMaxJobBytes() - total;
            if (remaining <= 0 || announced == 0 || announced > remaining) throw new IOException("录像源文件超出字节上限");
            checkGlobalBudget(Math.max(0, announced));
            String type = connection.getContentType();
            if (type != null && (type.toLowerCase(Locale.ROOT).contains("json") || type.toLowerCase(Locale.ROOT).contains("html"))) {
                throw new IOException("媒体节点返回了非录像内容");
            }
            if (Files.exists(source, LinkOption.NOFOLLOW_LINKS) || Files.exists(partial, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("录像分段文件已存在");
            }
            long received = 0;
            try (InputStream input = connection.getInputStream();
                 OutputStream output = Files.newOutputStream(partial, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled(task, cancelled);
                    checkGlobalBudget(count);
                    if (count > remaining - received || Files.getFileStore(partial).getUsableSpace() < count) {
                        throw new IOException("录像下载超出本机存储边界");
                    }
                    output.write(buffer, 0, count); received += count;
                }
            }
            if (received == 0 || (announced >= 0 && received != announced)) throw new IOException("录像源文件传输不完整");
            checkCancelled(task, cancelled);
            Files.move(partial, source, StandardCopyOption.ATOMIC_MOVE);
            return Math.addExact(total, received);
        } finally {
            connection.disconnect(); Files.deleteIfExists(partial);
        }
    }

    /** 跨任务计算根目录真实用量；有限深度和条目数使损坏或异常目录不能拖垮调度线程。 */
    private void checkGlobalBudget(long additional) {
        long maximum = settings.getMaxTotalBytes();
        if (maximum <= 0 || additional < 0 || additional > maximum) throw new IllegalStateException("录像临时存储预算无效");
        Path configured = Path.of(settings.getDirectory()).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(configured)) throw new IllegalStateException("录像临时目录不能是符号链接");
        long[] observed = new long[2];
        try {
            Path directory = configured.toRealPath();
            Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), 8, new SimpleFileVisitor<>() {
                /** 对目录也计数，避免大量空目录绕过遍历边界。 */
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    countEntry(); return FileVisitResult.CONTINUE;
                }

                /** 只计普通文件且不跟随链接；深度上限处仍为目录时拒绝不完整统计。 */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    countEntry();
                    if (!attrs.isRegularFile() || attrs.isSymbolicLink()) throw new IOException("录像临时目录包含不支持的条目");
                    observed[1] = Math.addExact(observed[1], attrs.size());
                    if (observed[1] > maximum - additional) throw new IOException("录像临时存储总量超出预算");
                    return FileVisitResult.CONTINUE;
                }

                /** 其他任务可以同时删除过期文件，已经消失的条目不再占用预算。 */
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException error) throws IOException {
                    if (error instanceof NoSuchFileException) return FileVisitResult.CONTINUE;
                    throw error;
                }

                /** 并发清理移走整个任务目录时按已释放空间处理，其他遍历故障仍失败关闭。 */
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                    if (error != null && !(error instanceof NoSuchFileException)) throw error;
                    return FileVisitResult.CONTINUE;
                }

                /** 用固定上限终止异常大目录，不能静默漏计剩余文件。 */
                private void countEntry() throws IOException {
                    if (++observed[0] > 10000) throw new IOException("录像临时目录条目超出扫描边界");
                }
            });
            if (observed[1] > maximum - additional) throw new IOException("录像临时存储总量超出预算");
        } catch (IOException | ArithmeticException ex) {
            throw new IllegalStateException("录像临时存储已满或无法安全统计", ex);
        }
    }

    /** 发布后只清理本任务的已知源文件，避免原始短段与成品重复占用整个保留期。 */
    private void discardSources(Path directory) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path file : entries) {
                String name = file.getFileName().toString();
                if (name.matches("source-[0-9]+\\.mp4(?:\\.part)?") || "verified.mp4".equals(name)) {
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
                        throw new IOException("录像源文件清理路径无效");
                    }
                    Files.delete(file);
                }
            }
        }
    }

    /** 外部取消和父任务取消均终止本机工作，避免取消后仍发布可读取成品。 */
    private void checkCancelled(SegmentedDownloadTask task, BooleanSupplier cancelled) {
        if (task.isCancelRequested() || task.getState() == SegmentedDownloadTask.State.CANCELLED
                || task.getState() == SegmentedDownloadTask.State.FAILED || cancelled.getAsBoolean()) {
            throw new CancellationException("录像下载已取消");
        }
    }

    /** 仅解释有限长度的自有元数据，损坏数据不能变成磁盘路径或永久令牌。 */
    private JSONObject readMetadata(String raw) {
        if (raw == null || raw.length() > 1024) return null;
        try {
            JSONObject value = JSON.parseObject(raw);
            if (value == null || value.size() != 3 || !validId(value.getString("taskId"))
                    || value.getString("serverId") == null || !(value.get("expiresAt") instanceof Number)) return null;
            return value;
        } catch (RuntimeException ex) { return null; }
    }

    /** 复用或撤销令牌前必须验证其仍属于本节点同一轮任务。 */
    private boolean belongsTo(JSONObject value, String taskId) {
        return value != null && serverId.equals(value.getString("serverId")) && taskId.equals(value.getString("taskId"))
                && value.getLongValue("expiresAt") > clock.millis();
    }
}
