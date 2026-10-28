package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.gb28181.service.download.SegmentedArtifactFiles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;

/** 使用随机令牌提供有限期成品，沿用无Cookie的MP4下载与Range探测契约。 */
@RestController
public class SegmentedDownloadFileController {
    private final SegmentedArtifactFiles files;

    /** 仅接收成品解析能力，控制器不能构造任意磁盘路径。 */
    public SegmentedDownloadFileController(SegmentedArtifactFiles files) { this.files = files; }

    /** 直接写原始字节避免全局JSON包装；GET和HEAD共享相同鉴权、长度与单区间语义。 */
    @RequestMapping(value = "/record-download/{token}.mp4", method = {RequestMethod.GET, RequestMethod.HEAD})
    public void download(@PathVariable("token") String token, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        Path path;
        try { path = files.resolve(token); }
        catch (RuntimeException ex) { empty(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE); return; }
        if (path == null) { empty(response, HttpServletResponse.SC_NOT_FOUND); return; }
        // 拒绝符号链接并在发送头部前打开文件，清理竞态不会暴露其他文件或伪造成功响应。
        try (SeekableByteChannel input = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            long size = input.size();
            String header = request.getHeader("Range");
            if (Collections.list(request.getHeaders("Range")).size() > 1) header = "invalid";
            // 未提供资源验证器时无法满足If-Range；按HTTP语义退回完整响应。
            if (request.getHeader("If-Range") != null) header = null;
            long[] range;
            try { range = parseRange(header, size); }
            catch (IllegalArgumentException ex) {
                response.setHeader("Content-Range", "bytes */" + size);
                empty(response, HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE); return;
            }
            long length = range[1] - range[0] + 1;
            response.setStatus(header == null ? HttpServletResponse.SC_OK : HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentType("video/mp4");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Disposition", "attachment; filename=\"recording.mp4\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges");
            if (header != null) response.setHeader("Content-Range", "bytes " + range[0] + "-" + range[1] + "/" + size);
            response.setContentLengthLong(length);
            if (!"HEAD".equals(request.getMethod())) {
                input.position(range[0]);
                OutputStream output = response.getOutputStream();
                ByteBuffer buffer = ByteBuffer.allocate(65536);
                long remaining = length;
                while (remaining > 0) {
                    buffer.clear(); buffer.limit((int) Math.min(buffer.capacity(), remaining));
                    int count = input.read(buffer);
                    if (count < 0) throw new IOException("录像成品在读取期间发生变化");
                    if (count == 0) continue;
                    output.write(buffer.array(), 0, count); remaining -= count;
                }
            }
            response.flushBuffer();
        } catch (NoSuchFileException ex) {
            if (!response.isCommitted()) empty(response, HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException ex) {
            if (!response.isCommitted()) empty(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            else throw ex;
        }
    }

    /** 严格支持一个字节区间，拒绝溢出、多区间和空区间，避免拼接响应与额外内存开销。 */
    private long[] parseRange(String header, long size) {
        if (size <= 0) throw new IllegalArgumentException("空文件");
        if (header == null) return new long[]{0, size - 1};
        if (!header.matches("bytes=(?:[0-9]+-[0-9]*|-[0-9]+)")) throw new IllegalArgumentException("无效区间");
        String[] parts = header.substring(6).split("-", -1);
        try {
            long start;
            long end;
            if (parts[0].isEmpty()) {
                long suffix = Long.parseLong(parts[1]);
                if (suffix <= 0) throw new IllegalArgumentException("空后缀");
                start = Math.max(0, size - suffix); end = size - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts[1].isEmpty() ? size - 1 : Math.min(Long.parseLong(parts[1]), size - 1);
            }
            if (start >= size || end < start) throw new IllegalArgumentException("区间越界");
            return new long[]{start, end};
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("区间超出整数范围"); }
    }

    /** 错误响应不输出内部路径、令牌状态或全局JSON包装。 */
    private void empty(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status); response.setContentLengthLong(0); response.flushBuffer();
    }
}
