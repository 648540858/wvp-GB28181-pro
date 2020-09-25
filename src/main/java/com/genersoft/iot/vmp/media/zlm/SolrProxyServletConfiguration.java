package com.genersoft.iot.vmp.media.zlm;

import com.google.common.collect.ImmutableMap;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Servlet;
import java.util.Map;

/**
 * 对查询流媒体信息的请求进行反向代理
 */
@Configuration
public class SolrProxyServletConfiguration {

    // 读取配置文件中路由设置
    @Value("${proxy.servlet_url}")
    private String servlet_url;
    // 读取配置中代理目标地址
    @Value("${proxy.target_url}")
    private String target_url;



    @Bean
    public Servlet createProxyServlet(){
        // 创建新的ProxyServlet
        return new ProxyServlet();
    }
    @Bean
    public ServletRegistrationBean proxyServletRegistration(){
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(createProxyServlet(), servlet_url);
        //设置网址以及参数
        Map<String, String> params = ImmutableMap.of(
                "targetUri", target_url,
                "log", "true");
        registrationBean.setInitParameters(params);
        return registrationBean;
    }
}
