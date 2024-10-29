package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.gb28181.session.SseSessionManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * SSE 推送.
 *
 * @author lawrencehj
 * @author <a href="mailto:xiaoQQya@126.com">xiaoQQya</a>
 * @since 2021/01/20
 */
@Tag(name = "SSE 推送")
@RestController
@RequestMapping("/api")
public class SseController {

    @Resource
    private SseSessionManager sseSessionManager;

    /**
     * SSE 推送.
     *
     * @param browserId 浏览器ID
     */
    @GetMapping("/emit")
    public SseEmitter emit(HttpServletResponse response, @RequestParam String browserId) throws IOException, InterruptedException {
//        response.setContentType("text/event-stream");
//        response.setCharacterEncoding("utf-8");
        return sseSessionManager.conect(browserId);
    }
}
