package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.IHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.ResourceInfo;
import com.genersoft.iot.vmp.vmanager.bean.SystemConfigInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
@Tag(name = "服务控制")

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

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceChannelService channelService;

    @Autowired
    private IStreamPushService pushService;


    @Autowired
    private IStreamProxyService proxyService;


    @Value("${server.port}")
    private int serverPort;


    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SendRtpPortManager sendRtpPortManager;


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
            mediaServerService.update(mediaServerItem);
        } else {
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
//        taskExecutor.execute(()-> {
//            try {
//                Thread.sleep(3000);
//                SipProvider up = (SipProvider) SpringBeanFactory.getBean("udpSipProvider");
//                SipStackImpl stack = (SipStackImpl) up.getSipStack();
//                stack.stop();
//                Iterator listener = stack.getListeningPoints();
//                while (listener.hasNext()) {
//                    stack.deleteListeningPoint((ListeningPoint) listener.next());
//                }
//                Iterator providers = stack.getSipProviders();
//                while (providers.hasNext()) {
//                    stack.deleteSipProvider((SipProvider) providers.next());
//                }
//                VManageBootstrap.restart();
//            } catch (InterruptedException | ObjectInUseException e) {
//                throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
//            }
//        });
    };

    @Operation(summary = "获取系统信息信息")
    @GetMapping(value = "/system/configInfo")
    @ResponseBody
    public SystemConfigInfo getConfigInfo() {
        SystemConfigInfo systemConfigInfo = new SystemConfigInfo();
        systemConfigInfo.setVersion(versionInfo.getVersion());
        systemConfigInfo.setSip(sipConfig);
        systemConfigInfo.setAddOn(userSetting);
        systemConfigInfo.setServerPort(serverPort);
        return systemConfigInfo;
    }

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

    @GetMapping(value = "/system/info")
    @ResponseBody
    @Operation(summary = "获取系统信息")
    public SystemAllInfo getSystemInfo() {
        SystemAllInfo systemAllInfo = redisCatchStorage.getSystemInfo();

        return systemAllInfo;
    }

    @GetMapping(value = "/media_server/load")
    @ResponseBody
    @Operation(summary = "获取负载信息")
    public List<MediaServerLoad> getMediaLoad() {
        List<MediaServerLoad> result = new ArrayList<>();
        List<MediaServerItem> allOnline = mediaServerService.getAllOnline();
        if (allOnline.size() == 0) {
            return result;
        }else {
            for (MediaServerItem mediaServerItem : allOnline) {
                result.add(mediaServerService.getLoad(mediaServerItem));
            }
        }
        return result;
    }

    @GetMapping(value = "/resource/info")
    @ResponseBody
    @Operation(summary = "获取负载信息")
    public ResourceInfo getResourceInfo() {
        ResourceInfo result = new ResourceInfo();
        ResourceBaseInfo deviceInfo = deviceService.getOverview();
        result.setDevice(deviceInfo);
        ResourceBaseInfo channelInfo = channelService.getOverview();
        result.setChannel(channelInfo);
        ResourceBaseInfo pushInfo = pushService.getOverview();
        result.setPush(pushInfo);
        ResourceBaseInfo proxyInfo = proxyService.getOverview();
        result.setProxy(proxyInfo);

        return result;
    }
}
