package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.IHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import gov.nist.javax.sip.SipStackImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipProvider;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@SuppressWarnings("rawtypes")
@Tag(name = "服务控制")
@CrossOrigin
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ZLMHttpHookSubscribe zlmHttpHookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VersionInfo versionInfo;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DynamicTask dynamicTask;

    @Value("${server.port}")
    private int serverPort;


    @GetMapping(value = "/media_server/list")
    @ResponseBody
    @Operation(summary = "流媒体服务列表")
    public WVPResult<List<MediaServerItem>> getMediaServerList() {
        WVPResult<List<MediaServerItem>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getAll());
        return result;
    }

    @GetMapping(value = "/media_server/online/list")
    @ResponseBody
    @Operation(summary = "在线流媒体服务列表")
    public WVPResult<List<MediaServerItem>> getOnlineMediaServerList() {
        WVPResult<List<MediaServerItem>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getAllOnline());
        return result;
    }

    @GetMapping(value = "/media_server/one/{id}")
    @ResponseBody
    @Operation(summary = "停止视频回放")
    @Parameter(name = "id", description = "流媒体服务ID", required = true)
    public WVPResult<MediaServerItem> getMediaServer(@PathVariable String id) {
        WVPResult<MediaServerItem> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getOne(id));
        return result;
    }

    @Operation(summary = "测试流媒体服务")
    @Parameter(name = "ip", description = "流媒体服务IP", required = true)
    @Parameter(name = "port", description = "流媒体服务HTT端口", required = true)
    @Parameter(name = "secret", description = "流媒体服务secret", required = true)
    @GetMapping(value = "/media_server/check")
    @ResponseBody
    public WVPResult<MediaServerItem> checkMediaServer(@RequestParam String ip, @RequestParam int port, @RequestParam String secret) {
        return mediaServerService.checkMediaServer(ip, port, secret);
    }

    @Operation(summary = "测试流媒体录像管理服务")
    @Parameter(name = "ip", description = "流媒体服务IP", required = true)
    @Parameter(name = "port", description = "流媒体服务HTT端口", required = true)
    @GetMapping(value = "/media_server/record/check")
    @ResponseBody
    public WVPResult<String> checkMediaRecordServer(@RequestParam String ip, @RequestParam int port) {
        boolean checkResult = mediaServerService.checkMediaRecordServer(ip, port);
        WVPResult<String> result = new WVPResult<>();
        if (checkResult) {
            result.setCode(0);
            result.setMsg("success");

        } else {
            result.setCode(-1);
            result.setMsg("连接失败");
        }
        return result;
    }

    @Operation(summary = "保存流媒体服务")
    @Parameter(name = "mediaServerItem", description = "流媒体信息", required = true)
    @PostMapping(value = "/media_server/save")
    @ResponseBody
    public WVPResult<String> saveMediaServer(@RequestBody MediaServerItem mediaServerItem) {
        MediaServerItem mediaServerItemInDatabase = mediaServerService.getOne(mediaServerItem.getId());

        if (mediaServerItemInDatabase != null) {
            if (StringUtils.isEmpty(mediaServerItemInDatabase.getSendRtpPortRange()) && StringUtils.isEmpty(mediaServerItem.getSendRtpPortRange())) {
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
            mediaServerService.update(mediaServerItem);
        } else {
            if (StringUtils.isEmpty(mediaServerItem.getSendRtpPortRange())) {
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
            return mediaServerService.add(mediaServerItem);
        }

        WVPResult<String> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        return result;
    }

    @Operation(summary = "移除流媒体服务")
    @Parameter(name = "id", description = "流媒体ID", required = true)
    @DeleteMapping(value = "/media_server/delete")
    @ResponseBody
    public WVPResult<String> deleteMediaServer(@RequestParam String id) {
        if (mediaServerService.getOne(id) != null) {
            mediaServerService.delete(id);
            mediaServerService.deleteDb(id);
        } else {
            WVPResult<String> result = new WVPResult<>();
            result.setCode(-1);
            result.setMsg("未找到此节点");
            return result;
        }
        WVPResult<String> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        return result;
    }


    @Operation(summary = "重启服务")
    @GetMapping(value = "/restart")
    @ResponseBody
    public Object restart() {
        Thread restartThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    SipProvider up = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
                    SipStackImpl stack = (SipStackImpl) up.getSipStack();
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

    @Operation(summary = "获取版本信息")
    @GetMapping(value = "/version")
    @ResponseBody
    public WVPResult<VersionPo> getVersion() {
        WVPResult<VersionPo> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(versionInfo.getVersion());
        return result;
    }

    @GetMapping(value = "/config")
    @Operation(summary = "获取配置信息")
    @Parameter(name = "type", description = "配置类型（sip, base）", required = true)
    @ResponseBody
    public WVPResult<JSONObject> getVersion(String type) {
        WVPResult<JSONObject> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server.port", serverPort);
        if (StringUtils.isEmpty(type)) {
            jsonObject.put("sip", JSON.toJSON(sipConfig));
            jsonObject.put("base", JSON.toJSON(userSetting));
        } else {
            switch (type) {
                case "sip":
                    jsonObject.put("sip", sipConfig);
                    break;
                case "base":
                    jsonObject.put("base", userSetting);
                    break;
                default:
                    break;
            }
        }
        result.setData(jsonObject);
        return result;
    }

    @GetMapping(value = "/hooks")
    @ResponseBody
    @Operation(summary = "获取当前所有hook")
    public WVPResult<List<IHookSubscribe>> getHooks() {
        WVPResult<List<IHookSubscribe>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        List<IHookSubscribe> all = zlmHttpHookSubscribe.getAll();
        result.setData(all);
        return result;
    }

//     //@ApiOperation("当前进行中的动态任务")
//    @GetMapping(value = "/dynamicTask")
//    @ResponseBody
//    public WVPResult<JSONObject> getDynamicTask(){
//        WVPResult<JSONObject> result = new WVPResult<>();
//        result.setCode(0);
//        result.setMsg("success");
//
//        JSONObject jsonObject = new JSONObject();
//
//        Set<String> allKeys = dynamicTask.getAllKeys();
//        jsonObject.put("server.port", serverPort);
//        if (StringUtils.isEmpty(type)) {
//            jsonObject.put("sip", JSON.toJSON(sipConfig));
//            jsonObject.put("base", JSON.toJSON(userSetting));
//        }else {
//            switch (type){
//                case "sip":
//                    jsonObject.put("sip", sipConfig);
//                    break;
//                case "base":
//                    jsonObject.put("base", userSetting);
//                    break;
//                default:
//                    break;
//            }
//        }
//        result.setData(jsonObject);
//        return result;
//    }
}
