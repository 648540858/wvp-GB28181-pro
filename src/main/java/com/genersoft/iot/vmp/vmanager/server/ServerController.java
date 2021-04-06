package com.genersoft.iot.vmp.vmanager.server;

import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.vmanager.gbStream.bean.GbStreamParam;
import gov.nist.javax.sip.SipStackImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.*;

import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipProvider;
import java.util.Iterator;

@CrossOrigin
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ConfigurableApplicationContext context;


    @RequestMapping(value = "/restart")
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
