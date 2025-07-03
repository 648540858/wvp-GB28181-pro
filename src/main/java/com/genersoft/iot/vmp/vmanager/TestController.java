package com.genersoft.iot.vmp.vmanager;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private HookSubscribe subscribe;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @GetMapping("/hook/list")
    public List<Hook> all(){
        return subscribe.getAll();
    }


    @GetMapping("/redis")
    public List<InviteInfo> redis(){
        InviteSessionType type = InviteSessionType.PLAY;
        String channelId = null;
        String stream = null;

        String key = VideoManagerConstants.INVITE_PREFIX;
        String keyPattern = (type != null ? type : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*")
                + ":*";
        ScanOptions options = ScanOptions.scanOptions().match(keyPattern).count(20).build();
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(key, options);
        List<InviteInfo> result = new ArrayList<>();
        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next().getKey());
                result.add((InviteInfo) cursor.next().getValue());
            }
        }catch (Exception e) {

        }finally {
            cursor.close();
        }
        return result;
    }

//    @Bean
//    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
//        ServletRegistrationBean<StatViewServlet> registrationBean = new ServletRegistrationBean<>(new StatViewServlet(),  "/druid/*");
//        registrationBean.addInitParameter("allow", "127.0.0.1");// IP白名单 (没有配置或者为空，则允许所有访问)
//        registrationBean.addInitParameter("deny", "");// IP黑名单 (存在共同时，deny优先于allow)
//        registrationBean.addInitParameter("loginUsername", "admin");
//        registrationBean.addInitParameter("loginPassword", "admin");
//        registrationBean.addInitParameter("resetEnable", "false");
//        return registrationBean;
//    }

}
