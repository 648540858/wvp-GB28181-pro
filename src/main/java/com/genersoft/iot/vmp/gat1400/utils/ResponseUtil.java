package com.genersoft.iot.vmp.gat1400.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusListObject;
import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;


public class ResponseUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 设置响应
     *
     * @param response    HttpServletResponse
     * @param contentType content-type
     * @param status      http状态码
     * @param value       响应内容
     * @throws IOException IOException
     */
    public static void makeResponse(HttpServletResponse response, String contentType,
                                    int status, Object value) throws IOException {
        response.setContentType(contentType);
        response.setStatus(status);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(value));
    }


    public static boolean validAllResponse(VIIDResponseStatusObject response) {
        return Optional.of(response)
                .map(VIIDResponseStatusObject::getResponseStatusListObject)
                .map(ResponseStatusListObject::getResponseStatusObject)
                .filter(CollectionUtils::isNotEmpty)
                .map(list ->
                        list.stream().allMatch(s -> "0".equals(s.getStatusCode()))
                )
                .orElse(false);
    }

    public static ResponseStatusObject emptyDataStatusObject() {
        ResponseStatusObject statusObject = new ResponseStatusObject();
        statusObject.setStatusCode("1");
        statusObject.setStatusString("不存在请求数据");
        return statusObject;
    }

    public static List<ResponseStatusObject> emptyDataStatusObjects() {
        return Collections.singletonList(emptyDataStatusObject());
    }

    public static ResponseStatusObject subscribeNotExists() {
        ResponseStatusObject statusObject = new ResponseStatusObject();
        statusObject.setStatusCode("1");
        statusObject.setStatusString("订阅不存在");
        return statusObject;
    }
}
