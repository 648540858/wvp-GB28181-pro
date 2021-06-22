package com.genersoft.iot.vmp.vmanager.server;

import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.media.zlm.ZLMServerConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import gov.nist.javax.sip.SipStackImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipProvider;
import java.util.ArrayList;
import java.util.Iterator;

@SuppressWarnings("rawtypes")
@Api(tags = "服务控制")
@CrossOrigin
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;


    @ApiOperation("流媒体服务列表")
    @GetMapping(value = "/media_server/list")
    @ResponseBody
    public Object getMediaServerList(){
        // TODO 为后续多个zlm支持准备
        ZLMServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        ArrayList<ZLMServerConfig> result = new ArrayList<>();
        result.add(mediaInfo);
        return result;
    }

    @ApiOperation("重启服务")
    @GetMapping(value = "/restart")
    @ResponseBody
    public Object restart(){
        Thread restartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    SipProvider up = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
                    SipStackImpl stack = (SipStackImpl)up.getSipStack();
                    stack.stop();
                    Iterator listener = stack.getListeningPoints();
                    while (listener.hasNext()) {
                        stack.deleteListeningPoint((ListeningPoint) listener.next());
                    }
                    Iterator providers = stack.getSipProviders();
                    while (providers.hasNext()) {
                        stack.deleteSipProvider((SipProvider) providers.next());
                    }
                    VManageBootstrap.restart();
                } catch (InterruptedException ignored) {
                } catch (ObjectInUseException e) {
                    e.printStackTrace();
                }
            }
        });

        restartThread.setDaemon(false);
        restartThread.start();
        return "success";
    }
}
