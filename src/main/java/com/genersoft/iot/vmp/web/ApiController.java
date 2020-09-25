package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.SipConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 兼容LiveGBS的API：系统接口
 */
@Controller
@CrossOrigin
@RequestMapping(value = "/api/v1")
public class ApiController {

    private final static Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private SipConfig sipConfig;


    @RequestMapping("/getserverinfo")
    private JSONObject getserverinfo(){
        JSONObject result = new JSONObject();
        result.put("Authorization","ceshi");
        result.put("Hardware","");
        result.put("InterfaceVersion","2.5.5");
        result.put("IsDemo","");
        result.put("Hardware","false");
        result.put("APIAuth","false");
        result.put("RemainDays","永久");
        result.put("RunningTime","");
        result.put("ServerTime","2020-09-02 17：11");
        result.put("StartUpTime","2020-09-02 17：11");
        result.put("Server","");
        result.put("SIPSerial", sipConfig.getSipId());
        result.put("SIPRealm", sipConfig.getSipDomain());
        result.put("SIPHost", sipConfig.getSipIp());
        result.put("SIPPort", sipConfig.getSipPort());
        result.put("ChannelCount","1000");
        result.put("VersionType","");
        result.put("LogoMiniText","");
        result.put("LogoText","");
        result.put("CopyrightText","");

        return result;
    }

    @RequestMapping(value = "/userinfo")
    private JSONObject userinfo(){
//        JSONObject result = new JSONObject();
//        result.put("ID","ceshi");
//        result.put("Hardware","");
//        result.put("InterfaceVersion","2.5.5");
//        result.put("IsDemo","");
//        result.put("Hardware","false");
//        result.put("APIAuth","false");
//        result.put("RemainDays","永久");
//        result.put("RunningTime","");
//        result.put("ServerTime","2020-09-02 17：11");
//        result.put("StartUpTime","2020-09-02 17：11");
//        result.put("Server","");
//        result.put("SIPSerial", sipConfig.getSipId());
//        result.put("SIPRealm", sipConfig.getSipDomain());
//        result.put("SIPHost", sipConfig.getSipIp());
//        result.put("SIPPort", sipConfig.getSipPort());
//        result.put("ChannelCount","1000");
//        result.put("VersionType","");
//        result.put("LogoMiniText","");
//        result.put("LogoText","");
//        result.put("CopyrightText","");

        return null;
    }

    /**
     *  系统接口 - 登录
     * @param username 用户名
     * @param password 密码(经过md5加密,32位长度,不带中划线,不区分大小写)
     * @return
     */
    @RequestMapping(value = "/login")
    @ResponseBody
    private JSONObject login(String username,String password ){
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("模拟接口> 登录 API调用，username：%s ，password：%s ",
                    username, password));
        }

        JSONObject result = new JSONObject();
        result.put("CookieToken","ynBDDiKMg");
        result.put("URLToken","MOBkORkqnrnoVGcKIAHXppgfkNWRdV7utZSkDrI448Q.oxNjAxNTM4NDk3LCJwIjoiZGJjODg5NzliNzVj" +
                "Nzc2YmU5MzBjM2JjNjg1ZWFiNGI5ZjhhN2Y0N2RlZjg3NWUyOTJkY2VkYjkwYmEwMTA0NyIsInQiOjE2MDA5MzM2OTcsInUiOiI" +
                "4ODlkZDYyM2ViIn0eyJlIj.GciOiJIUzI1NiIsInR5cCI6IkpXVCJ9eyJhb");
        result.put("TokenTimeout",604800);
        result.put("AuthToken","MOBkORkqnrnoVGcKIAHXppgfkNWRdV7utZSkDrI448Q.oxNjAxNTM4NDk3LCJwIjoiZGJjODg5NzliNzVj" +
                "Nzc2YmU5MzBjM2JjNjg1ZWFiNGI5ZjhhN2Y0N2RlZjg3NWUyOTJkY2VkYjkwYmEwMTA0NyIsInQiOjE2MDA5MzM2OTcsInUiOiI" +
                "4ODlkZDYyM2ViIn0eyJlIj.GciOiJIUzI1NiIsInR5cCI6IkpXVCJ9eyJhb");
        result.put("Token","ynBDDiKMg");
        return result;
    }
}
