package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.IHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import gov.nist.javax.sip.SipStackImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.SipProvider;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("rawtypes")
@Tag(name = "服务控制")
@CrossOrigin
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ZlmHttpHookSubscribe zlmHttpHookSubscribe;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private VersionInfo versionInfo;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Value("${server.port}")
    private int serverPort;


    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    @GetMapping(value = "/media_server/list")
    @ResponseBody
    @Operation(summary = "流媒体服务列表")
    public List<MediaServerItem> getMediaServerList() {
        return mediaServerService.getAll();
    }

    @GetMapping(value = "/media_server/online/list")
    @ResponseBody
    @Operation(summary = "在线流媒体服务列表")
    public List<MediaServerItem> getOnlineMediaServerList() {
        return mediaServerService.getAllOnline();
    }

    @GetMapping(value = "/media_server/one/{id}")
    @ResponseBody
    @Operation(summary = "停止视频回放")
    @Parameter(name = "id", description = "流媒体服务ID", required = true)
    public MediaServerItem getMediaServer(@PathVariable String id) {
        return mediaServerService.getOne(id);
    }

    @Operation(summary = "测试流媒体服务")
    @Parameter(name = "ip", description = "流媒体服务IP", required = true)
    @Parameter(name = "port", description = "流媒体服务HTT端口", required = true)
    @Parameter(name = "secret", description = "流媒体服务secret", required = true)
    @GetMapping(value = "/media_server/check")
    @ResponseBody
    public MediaServerItem checkMediaServer(@RequestParam String ip, @RequestParam int port, @RequestParam String secret) {
        return mediaServerService.checkMediaServer(ip, port, secret);
    }

    @Operation(summary = "测试流媒体录像管理服务")
    @Parameter(name = "ip", description = "流媒体服务IP", required = true)
    @Parameter(name = "port", description = "流媒体服务HTT端口", required = true)
    @GetMapping(value = "/media_server/record/check")
    @ResponseBody
    public void checkMediaRecordServer(@RequestParam String ip, @RequestParam int port) {
        boolean checkResult = mediaServerService.checkMediaRecordServer(ip, port);
        if (!checkResult) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接失败");
        }
    }

    @Operation(summary = "保存流媒体服务")
    @Parameter(name = "mediaServerItem", description = "流媒体信息", required = true)
    @PostMapping(value = "/media_server/save")
    @ResponseBody
    public void saveMediaServer(@RequestBody MediaServerItem mediaServerItem) {
        MediaServerItem mediaServerItemInDatabase = mediaServerService.getOne(mediaServerItem.getId());

        if (mediaServerItemInDatabase != null) {
            if (ObjectUtils.isEmpty(mediaServerItemInDatabase.getSendRtpPortRange()) && ObjectUtils.isEmpty(mediaServerItem.getSendRtpPortRange())) {
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
            mediaServerService.update(mediaServerItem);
        } else {
            if (ObjectUtils.isEmpty(mediaServerItem.getSendRtpPortRange())) {
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
            mediaServerService.add(mediaServerItem);
        }
    }

    @Operation(summary = "移除流媒体服务")
    @Parameter(name = "id", description = "流媒体ID", required = true)
    @DeleteMapping(value = "/media_server/delete")
    @ResponseBody
    public void deleteMediaServer(@RequestParam String id) {
        if (mediaServerService.getOne(id) == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到此节点");
        }
        mediaServerService.delete(id);
        mediaServerService.deleteDb(id);
    }


    @Operation(summary = "重启服务")
    @GetMapping(value = "/restart")
    @ResponseBody
    public void restart() {
        taskExecutor.execute(()-> {
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
            } catch (InterruptedException | ObjectInUseException e) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
            }
        });
    };

    @Operation(summary = "获取版本信息")
    @GetMapping(value = "/version")
    @ResponseBody
    public VersionPo VersionPogetVersion() {
        return versionInfo.getVersion();
    }

    @GetMapping(value = "/config")
    @Operation(summary = "获取配置信息")
    @Parameter(name = "type", description = "配置类型（sip, base）", required = true)
    @ResponseBody
    public JSONObject getVersion(String type) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server.port", serverPort);
        if (ObjectUtils.isEmpty(type)) {
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
        return jsonObject;
    }

    @GetMapping(value = "/hooks")
    @ResponseBody
    @Operation(summary = "获取当前所有hook")
    public List<IHookSubscribe> getHooks() {
        return zlmHttpHookSubscribe.getAll();
    }
}
