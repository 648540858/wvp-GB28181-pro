package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEventListener;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


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
    private AlarmEventListener alarmEventListener;

    /**
     * SSE 推送.
     *
     * @param response  响应
     * @param browserId 浏览器ID
     * @throws IOException IOEXCEPTION
     * @author <a href="mailto:xiaoQQya@126.com">xiaoQQya</a>
     * @since 2023/11/06
     */
    @GetMapping("/emit")
    public void emit(HttpServletResponse response, @RequestParam String browserId) throws IOException, InterruptedException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("utf-8");

        PrintWriter writer = response.getWriter();
        alarmEventListener.addSseEmitter(browserId, writer);

        while (!writer.checkError()) {
            Thread.sleep(1000);
            writer.write(":keep alive\n\n");
            writer.flush();
        }
        alarmEventListener.removeSseEmitter(browserId, writer);
    }
}
