package com.genersoft.iot.vmp.web.custom.conf;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sip.message.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * sign token 过滤器
 */

@Slf4j
@Component
@ConditionalOnProperty(value = "sy.enable", havingValue = "true")
public class SignAuthenticationFilter extends OncePerRequestFilter {

    private final static String WSHeader = "sec-websocket-protocol";


    @Autowired
    private UserSetting userSetting;


    @Override
    protected void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 忽略登录请求的token验证
        String requestURI = servletRequest.getRequestURI();
        // 包装原始请求，缓存请求体
        CachedBodyHttpServletRequest request = new CachedBodyHttpServletRequest(servletRequest);
        if (!requestURI.startsWith("/api/sy")) {
            chain.doFilter(request, response);
            return;
        }
        // 设置响应内容类型
        response.setContentType("application/json;charset=UTF-8");

        try {
            String sign = request.getParameter("sign");
            String appKey = request.getParameter("appKey");
            String accessToken = request.getParameter("accessToken");
            String timestampStr = request.getParameter("timestamp");

            if (sign == null || appKey == null || accessToken == null || timestampStr == null) {
                log.info("[SY-接口验签] 缺少关键参数：sign/appKey/accessToken/timestamp, 请求地址: {} ", requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(6017, "缺少关键参数"));
                out.close();
                return;
            }
            if (SyTokenManager.INSTANCE.appMap.get(appKey) == null) {
                log.info("[SY-接口验签] appKey {} 对应的 secret 不存在, 请求地址: {} ", appKey, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(6017, "缺少关键参数"));
                out.close();
                return;
            }

            Map<String, String[]> parameterMap = request.getParameterMap();
            // 参数排序
            Set<String> paramKeys = new TreeSet<>(parameterMap.keySet());

            // 拼接签名信息
            // 参数拼接
            StringBuilder beforeSign = new StringBuilder();
            for (String paramKey : paramKeys) {
                if (paramKey.equals("sign")) {
                    continue;
                }
                beforeSign.append(paramKey).append(parameterMap.get(paramKey)[0]);
            }
            // 如果是post请求的json消息，拼接body字符串
            if (request.getContentLength() > 0
                    && request.getMethod().equalsIgnoreCase("POST")
                    && request.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
                // 读取body内容 - 使用自定义缓存机制
                String requestBody = request.getCachedBody();
                if (!ObjectUtils.isEmpty(requestBody)) {
                    beforeSign.append(requestBody);
                    log.debug("[SY-接口验签] 读取到请求体内容，长度: {}", requestBody.length());
                } else {
                    log.warn("[SY-接口验签] 请求体内容为空");
                }
            }
            beforeSign.append(SyTokenManager.INSTANCE.appMap.get(appKey));
            // 生成签名
            String buildSign = SmUtil.sm3(beforeSign.toString());
            if (!buildSign.equals(sign)) {
                log.info("[SY-接口验签] 失败，加密前内容： {}, 请求地址: {} ", beforeSign, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(6017, "接口鉴权失败"));
                out.close();
                return;
            }
            // 验证请求时间戳
            long timestamp = Long.parseLong(timestampStr);
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis > SyTokenManager.INSTANCE.expires * 60 * 1000 + timestamp ) {
                log.info("[SY-接口验签] 时间戳已经过期, 请求时间戳：{}， 当前时间： {}, 过期时间： {}, 请求地址: {} ", timestamp, currentTimeMillis, timestamp + SyTokenManager.INSTANCE.expires * 60 * 1000, requestURI);
                response.setStatus(Response.OK);
                PrintWriter out = response.getWriter();
                out.println(getErrorResult(6016, "接口过期"));
                out.close();
                return;
            }
            // accessToken校验
            if (accessToken.equals(SyTokenManager.INSTANCE.adminToken)) {
                log.info("[SY-接口验签] adminToken已经默认放行, 请求地址: {} ", requestURI);
                chain.doFilter(request, response);
                return;
            }else {
                // 对token进行解密
                SM4 sm4 = SmUtil.sm4(HexUtil.decodeHex(SyTokenManager.INSTANCE.sm4Key));
                String decryptStr = sm4.decryptStr(accessToken, CharsetUtil.CHARSET_UTF_8);
                if (decryptStr == null) {
                    log.info("[SY-接口验签] accessToken解密失败, 请求地址: {} ", requestURI);
                    response.setStatus(Response.OK);
                    PrintWriter out = response.getWriter();
                    out.println(getErrorResult(6017, "接口鉴权失败"));
                    out.close();
                    return;
                }
                JSONObject jsonObject = JSON.parseObject(decryptStr);
                Long expirationTime = jsonObject.getLong("expirationTime");
                if (expirationTime < System.currentTimeMillis()) {
                    log.info("[SY-接口验签] accessToken 已经过期, 请求地址: {} ", requestURI);
                    response.setStatus(Response.OK);
                    PrintWriter out = response.getWriter();
                    out.println(getErrorResult(6018, "Token已过期"));
                    out.close();
                    return;
                }
            }
        }catch (Exception e) {
            log.info("[SY-接口验签] 读取body失败, 请求地址: {}  ", requestURI, e);
            response.setStatus(Response.OK);
            PrintWriter out = response.getWriter();
            out.println(getErrorResult(6017, "接口鉴权异常"));
            out.close();
            return;
        }
        chain.doFilter(request, response);
    }

    private String getErrorResult(Integer code, String message) {
        WVPResult<Object> wvpResult = new WVPResult<>();
        wvpResult.setCode(code);
        wvpResult.setMsg(message);
        return JSON.toJSONString(wvpResult);
    }

}
