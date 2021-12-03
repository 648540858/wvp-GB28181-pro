package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
public class ProxyServletConfig {

    private final static Logger logger = LoggerFactory.getLogger(ProxyServletConfig.class);

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
        if (logger.isDebugEnabled()) {
            servletRegistrationBean.addInitParameter("log", "true");
        }
        return servletRegistrationBean;
    }

    class ZlmProxyServlet extends ProxyServlet{
        @Override
        protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
            String queryStr = super.rewriteQueryStringFromRequest(servletRequest, queryString);
            MediaServerItem mediaInfo = getMediaInfoByUri(servletRequest.getRequestURI());
            if (mediaInfo != null) {
                if (!StringUtils.isEmpty(queryStr)) {
                    queryStr += "&secret=" + mediaInfo.getSecret();
                }else {
                    queryStr = "secret=" + mediaInfo.getSecret();
                }
            }
            return queryStr;
        }

        /**
         * 异常处理
         */
        @Override
        protected void handleRequestException(HttpRequest proxyRequest, HttpResponse proxyResonse, Exception e){
            try {
                super.handleRequestException(proxyRequest, proxyResonse, e);
            } catch (ServletException servletException) {
                logger.error("zlm 代理失败： ", e);
            } catch (IOException ioException) {
                if (ioException instanceof ConnectException) {
                    logger.error("zlm 连接失败");
                } else if (ioException instanceof ClientAbortException) {
                    logger.error("zlm: 用户已中断连接，代理终止");
                } else {
                    logger.error("zlm 代理失败： ", e);
                }
            } catch (RuntimeException exception){
                logger.error("zlm 代理失败： ", e);
            }
        }

        /**
         * 对于为按照格式请求的可以直接返回404
         */
        @Override
        protected String getTargetUri(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);

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
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);
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
        MediaServerItem getMediaInfoByUri(String uri){
            String[] split = uri.split("/");
            String mediaServerId = split[2];
            if ("default".equals(mediaServerId)) {
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
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);
            String url = super.rewriteUrlFromRequest(servletRequest);
            if (mediaInfo == null) {
                logger.error("[ZLM服务访问代理]，错误：处理url信息时未找到流媒体信息=>{}", requestURI);
                return  url;
            }
            if (!StringUtils.isEmpty(mediaInfo.getId())) {
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
        if (logger.isDebugEnabled()) {
            servletRegistrationBean.addInitParameter("log", "true");
        }
        return servletRegistrationBean;
    }

    class RecordProxyServlet extends ProxyServlet{

        @Override
        protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
            String queryStr = super.rewriteQueryStringFromRequest(servletRequest, queryString);
            MediaServerItem mediaInfo = getMediaInfoByUri(servletRequest.getRequestURI());
            String remoteHost = String.format("http://%s:%s", mediaInfo.getIp(), mediaInfo.getHttpPort());
            if (mediaInfo != null) {
                if (!StringUtils.isEmpty(queryStr)) {
                    queryStr += "&remoteHost=" + remoteHost;
                }else {
                    queryStr = "remoteHost=" + remoteHost;
                }
            }
            return queryStr;
        }

        /**
         * 异常处理
         */
        @Override
        protected void handleRequestException(HttpRequest proxyRequest, HttpResponse proxyResponse, Exception e){
            try {
                super.handleRequestException(proxyRequest, proxyResponse, e);
            } catch (ServletException servletException) {
                logger.error("录像服务 代理失败： ", e);
            } catch (IOException ioException) {
                if (ioException instanceof ConnectException) {
                    logger.error("录像服务 连接失败");
                } else if (ioException instanceof ClientAbortException) {
                    logger.error("录像服务:用户已中断连接，代理终止");
                } else {
                    logger.error("录像服务 代理失败： ", e);
                }
            } catch (RuntimeException exception){
                logger.error("录像服务 代理失败： ", e);
            }
        }

        /**
         * 对于为按照格式请求的可以直接返回404
         */
        @Override
        protected String getTargetUri(HttpServletRequest servletRequest) {
            String requestURI = servletRequest.getRequestURI();
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);

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
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);
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
        MediaServerItem getMediaInfoByUri(String uri){
            String[] split = uri.split("/");
            String mediaServerId = split[2];
            if ("default".equals(mediaServerId)) {
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
            MediaServerItem mediaInfo = getMediaInfoByUri(requestURI);
            String url = super.rewriteUrlFromRequest(servletRequest);
            if (mediaInfo == null) {
                logger.error("[录像服务访问代理]，错误：处理url信息时未找到流媒体信息=>{}", requestURI);
                return  url;
            }
            if (!StringUtils.isEmpty(mediaInfo.getId())) {
                url = url.replace(mediaInfo.getId() + "/", "");
            }
            return url.replace("default/", "");
        }
    }

}
