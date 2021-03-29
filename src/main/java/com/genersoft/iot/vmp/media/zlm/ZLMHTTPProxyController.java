package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/zlm")
public class ZLMHTTPProxyController {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @ResponseBody
    @RequestMapping(value = "/**/**/**", produces = "application/json;charset=UTF-8")
    public Object proxy(HttpServletRequest request, HttpServletResponse response) {

        if (redisCatchStorage.getMediaInfo() == null) {
            return "未接入流媒体";
        }
        String mediaServerIp = request.getParameter("mediaServerIp");
        if (StringUtils.isBlank(mediaServerIp)) {
            String channelId = request.getParameter("channelId");
            String streamId = request.getParameter("stream");
            mediaServerIp = streamSession.getMediaServerIp(channelId, streamId);
        }
        if (StringUtils.isBlank(mediaServerIp)) {
            // TODO 前端要提供zlm选择列表，传递回来
            mediaServerIp = "127.0.0.1";
        }
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String requestURI = String.format("http://%s:%s%s?%s&%s",
                mediaServerIp,
                mediaInfo.getHttpPort(),
                request.getRequestURI().replace("/zlm", ""),
                mediaInfo.getHookAdminParams(),
                request.getQueryString()
        );
        // 发送请求
        RestTemplate restTemplate = new RestTemplate();
        //将指定的url返回的参数自动封装到自定义好的对应类对象中
        Object result = null;
        try {
            result = restTemplate.getForObject(requestURI, Object.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            response.setStatus(httpClientErrorException.getStatusCode().value());
        }
        return result;
    }
}
