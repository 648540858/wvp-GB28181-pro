package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.ConnectException;

/**
 * @author lin
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})
@Configuration
@Order(1)
@Slf4j
public class ProxyServletConfig {

    @Autowired
    private IMediaServerService mediaServerService;

    @Value("${server.port}")
    private int serverPort;

    @Bean
    public ServletRegistrationBean zlmServletRegistrationBean(){
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new ZlmProxyServlet(),"/zlm/*");
        servletRegistrationBean.setName("zlm_Proxy");
        servletRegistrationBean.addInitParameter("targetUri", "http://127.0.0.1:6080");
        servletRegistrationBean.addUrlMappings();
        if (log.isDebugEnabled()) {
            servletRegistrationBean.addInitParameter("log", "true");
        }
        return servletRegistrationBean;
    }

    class ZlmProxyServlet extends ProxyServlet{
        @Override
        protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
            String queryStr = super.rewriteQueryStringFromRequest(servletRequest, queryString);
            MediaServer mediaInfo = getMediaInfoByUri(servletRequest.getRequestURI());
            if (mediaInfo != null) {
                if (!ObjectUtils.isEmpty(queryStr)) {
                    queryStr += "&secret=" + mediaInfo.getSecret();
                }else {
                    queryStr = "secret=" + mediaInfo.getSecret();
                }
            }
            return queryStr;
        }


        @Override
        protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                         HttpRequest proxyRequest) throws IOException {
            HttpResponse response = super.doExecute(servletRequest, servletResponse, proxyRequest);
            response.removeHeaders("Access-Control-Allow-Origin");
            response.setHeader("Access-Control-Allow-Credentials","true");
            response.removeHeaders("Access-Control-Allow-Credentials");

            return response;
        }

        /**
         * 异常处理
         */
        @Override
        protected void handleRequestException(HttpRequest proxyRequest, HttpResponse proxyResonse, Exception e){
            try {
                super.handleRequestException(proxyRequest, proxyResonse, e);
            } catch (ServletException servletException) {
                log.error("zlm 代理失败： ", e);
            } catch (IOException ioException) {
                if (ioException instanceof ConnectException) {
                    log.error("zlm 连接失败");
                }  else {
                    log.error("zlm 代理失败： ", e);
                }
            } catch (RuntimeException exception){
                log.error("zlm 代理失败： ", e);
            }
        }

        /**
         * 对于为按照格式请求的可以直接返回404
         */
        @Override
        protected String getTargetUri(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);

            String uri = null;
            if (mediaInfo != null) {
//                String realRequestURI = requestURI.substring(requestURI.indexOf(mediaInfo.getId())+ mediaInfo.getId().length());
                uri = String.format("http://%s:%s", mediaInfo.getIp(), mediaInfo.getHttpPort());
            }else {
                uri = "http://127.0.0.1:" + serverPort +"/index/hook/null"; // 只是一个能返回404的请求而已， 其他的也可以
            }
            return uri;
        }

        /**
         * 动态替换请求目标
         */
        @Override
        protected HttpHost getTargetHost(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);
            HttpHost host;
            if (mediaInfo != null) {
                host = new HttpHost(mediaInfo.getIp(), mediaInfo.getHttpPort());
            }else {
                host = new HttpHost("127.0.0.1", serverPort);
            }
            return host;

        }

        /**
         * 根据uri获取流媒体信息
         */
        MediaServer getMediaInfoByUri(String uri){
            String[] split = uri.split("/");
            String mediaServerId = split[2];
            if ("default".equalsIgnoreCase(mediaServerId)) {
                return mediaServerService.getDefaultMediaServer();
            }else {
                return mediaServerService.getOne(mediaServerId);
            }
        }

        /**
         * 去掉url中的标志信息
         */
        @Override
        protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);
            String url = super.rewriteUrlFromRequest(servletRequest);
            if (mediaInfo == null) {
                log.error("[ZLM服务访问代理]，错误：处理url信息时未找到流媒体信息=>{}", requestURI);
                return  url;
            }
            if (!ObjectUtils.isEmpty(mediaInfo.getId())) {
                url = url.replace(mediaInfo.getId() + "/", "");
            }
            return url.replace("default/", "");
        }
    }

    @Bean
    public ServletRegistrationBean recordServletRegistrationBean(){
        ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new RecordProxyServlet(),"/record_proxy/*");
        servletRegistrationBean.setName("record_proxy");
        servletRegistrationBean.addInitParameter("targetUri", "http://127.0.0.1:18081");
        servletRegistrationBean.addUrlMappings();
        if (log.isDebugEnabled()) {
            servletRegistrationBean.addInitParameter("log", "true");
        }
        return servletRegistrationBean;
    }

    class RecordProxyServlet extends ProxyServlet{

        @Override
        protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
            String queryStr = super.rewriteQueryStringFromRequest(servletRequest, queryString);
            MediaServer mediaInfo = getMediaInfoByUri(servletRequest.getRequestURI());
            if (mediaInfo == null) {
                return null;
            }
            String remoteHost = String.format("http://%s:%s", mediaInfo.getStreamIp(), mediaInfo.getRecordAssistPort());
            if (!ObjectUtils.isEmpty(queryStr)) {
                queryStr += "&remoteHost=" + remoteHost;
            }else {
                queryStr = "remoteHost=" + remoteHost;
            }
            return queryStr;
        }


        @Override
        protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                         HttpRequest proxyRequest) throws IOException {
            HttpResponse response = super.doExecute(servletRequest, servletResponse, proxyRequest);
            String origin = servletRequest.getHeader("origin");
            response.setHeader("Access-Control-Allow-Origin",origin);
            response.setHeader("Access-Control-Allow-Credentials","true");

            return response;
        }

        /**
         * 异常处理
         */
        @Override
        protected void handleRequestException(HttpRequest proxyRequest, HttpResponse proxyResponse, Exception e){
            try {
                super.handleRequestException(proxyRequest, proxyResponse, e);
            } catch (ServletException servletException) {
                log.error("录像服务 代理失败： ", e);
            } catch (IOException ioException) {
                if (ioException instanceof ConnectException) {
                    log.error("录像服务 连接失败");
//                }else if (ioException instanceof ClientAbortException) {
//                    /**
//                     * TODO 使用这个代理库实现代理在遇到代理视频文件时，如果是206结果，会遇到报错蛋市目前功能正常，
//                     * TODO 暂时去除异常处理。后续使用其他代理框架修改测试
//                     */

                }else {
                    log.error("录像服务 代理失败： ", e);
                }
            } catch (RuntimeException exception){
                log.error("录像服务 代理失败： ", e);
            }
        }

        /**
         * 对于为按照格式请求的可以直接返回404
         */
        @Override
        protected String getTargetUri(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);

            String uri = null;
            if (mediaInfo != null) {
//                String realRequestURI = requestURI.substring(requestURI.indexOf(mediaInfo.getId())+ mediaInfo.getId().length());
                uri = String.format("http://%s:%s", mediaInfo.getIp(), mediaInfo.getRecordAssistPort());
            }else {
                uri = "http://127.0.0.1:" + serverPort +"/index/hook/null"; // 只是一个能返回404的请求而已， 其他的也可以
            }
            return uri;
        }

        /**
         * 动态替换请求目标
         */
        @Override
        protected HttpHost getTargetHost(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);
            HttpHost host;
            if (mediaInfo != null) {
                host = new HttpHost(mediaInfo.getIp(), mediaInfo.getRecordAssistPort());
            }else {
                host = new HttpHost("127.0.0.1", serverPort);
            }
            return host;

        }

        /**
         * 根据uri获取流媒体信息
         */
        MediaServer getMediaInfoByUri(String uri){
            String[] split = uri.split("/");
            String mediaServerId = split[2];
            if ("default".equalsIgnoreCase(mediaServerId)) {
                return mediaServerService.getDefaultMediaServer();
            }else {
                return mediaServerService.getOne(mediaServerId);
            }

        }

        /**
         * 去掉url中的标志信息
         */
        @Override
        protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServer mediaInfo = getMediaInfoByUri(requestURI);
            String url = super.rewriteUrlFromRequest(servletRequest);
            if (mediaInfo == null) {
                log.error("[录像服务访问代理]，错误：处理url信息时未找到流媒体信息=>{}", requestURI);
                return  url;
            }
            if (!ObjectUtils.isEmpty(mediaInfo.getId())) {
                url = url.replace(mediaInfo.getId() + "/", "");
            }
            return url.replace("default/", "");
        }
    }

}
