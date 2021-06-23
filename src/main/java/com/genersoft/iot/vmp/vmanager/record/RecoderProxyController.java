package com.genersoft.iot.vmp.vmanager.record;

import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@RestController
@RequestMapping("/record_proxy")
public class RecoderProxyController {


    // private final static Logger logger = LoggerFactory.getLogger(ZLMHTTPProxyController.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private MediaConfig mediaConfig;

    @ResponseBody
    @RequestMapping(value = "/**/**/**", produces = "application/json;charset=UTF-8")
    public Object proxy(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException{


        String baseRequestURI = request.getRequestURI();
        String[] split = baseRequestURI.split("/");
        if (split.length <= 2) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }
        String mediaId = split[2];
        if (StringUtils.isEmpty(mediaId)){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return null;
        }
        // 后续改为根据Id获取对应的ZLM
        ZLMServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String requestURI = String.format("http://%s:%s%s?%s",
                mediaInfo.getSdpIp(),
                mediaConfig.getRecordAssistPort(),
                baseRequestURI.substring(baseRequestURI.indexOf(mediaId) + mediaId.length()),
                URLDecoder.decode(request.getQueryString(), "UTF-8")
        );
        // 发送请求
        RestTemplate restTemplate = new RestTemplate();
        //将指定的url返回的参数自动封装到自定义好的对应类对象中
        Object result = null;
        try {
            result = restTemplate.getForObject(requestURI,Object.class);

        }catch (HttpClientErrorException httpClientErrorException) {
            response.setStatus(httpClientErrorException.getStatusCode().value());
        }
        return result;
    }
}
