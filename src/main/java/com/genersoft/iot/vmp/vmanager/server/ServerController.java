package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.SystemAllInfo;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IDeviceService;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.event.mediaServer.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.MediaServerLoad;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.ResourceBaseInfo;
import com.genersoft.iot.vmp.vmanager.bean.ResourceInfo;
import com.genersoft.iot.vmp.vmanager.bean.SystemConfigInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

@SuppressWarnings("rawtypes")
@Tag(name = "服务控制")
@Slf4j
@RestController
@RequestMapping("/api/server")
public class ServerController {


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
    private ApplicationEventPublisher applicationEventPublisher;


    @GetMapping(value = "/media_server/list")
    @ResponseBody
    @Operation(summary = "流媒体服务列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServer> getMediaServerList() {
        return mediaServerService.getAll();
    }

    @GetMapping(value = "/media_server/online/list")
    @ResponseBody
    @Operation(summary = "在线流媒体服务列表", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServer> getOnlineMediaServerList() {
        return mediaServerService.getAllOnline();
    }

    @GetMapping(value = "/media_server/one/{id}")
    @ResponseBody
    @Operation(summary = "停止视频回放", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "流媒体服务ID", required = true)
    public MediaServer getMediaServer(@PathVariable String id) {
        return mediaServerService.getOne(id);
    }

    @Operation(summary = "测试流媒体服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "ip", description = "流媒体服务IP", required = true)
    @Parameter(name = "port", description = "流媒体服务HTT端口", required = true)
    @Parameter(name = "secret", description = "流媒体服务secret", required = true)
    @GetMapping(value = "/media_server/check")
    @ResponseBody
    public MediaServer checkMediaServer(@RequestParam String ip, @RequestParam int port, @RequestParam String secret, @RequestParam String type) {
        return mediaServerService.checkMediaServer(ip, port, secret, type);
    }

    @Operation(summary = "测试流媒体录像管理服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

    @Operation(summary = "保存流媒体服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "mediaServerItem", description = "流媒体信息", required = true)
    @PostMapping(value = "/media_server/save")
    @ResponseBody
    public void saveMediaServer(@RequestBody MediaServer mediaServer) {
        MediaServer mediaServerItemInDatabase = mediaServerService.getOneFromDatabase(mediaServer.getId());

        if (mediaServerItemInDatabase != null) {
            mediaServerService.update(mediaServer);
        } else {
            mediaServerService.add(mediaServer);
            // 发送事件
            MediaServerChangeEvent event = new MediaServerChangeEvent(this);
            event.setMediaServerItemList(mediaServer);
            applicationEventPublisher.publishEvent(event);
        }
    }

    @Operation(summary = "移除流媒体服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "id", description = "流媒体ID", required = true)
    @DeleteMapping(value = "/media_server/delete")
    @ResponseBody
    public void deleteMediaServer(@RequestParam String id) {
        MediaServer mediaServer = mediaServerService.getOne(id);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "流媒体不存在");
        }
        mediaServerService.delete(mediaServer);
    }

    @Operation(summary = "获取流信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @Parameter(name = "app", description = "应用名", required = true)
    @Parameter(name = "stream", description = "流ID", required = true)
    @Parameter(name = "mediaServerId", description = "流媒体ID", required = true)
    @GetMapping(value = "/media_server/media_info")
    @ResponseBody
    public MediaInfo getMediaInfo(String app, String stream, String mediaServerId) {
        MediaServer mediaServer = mediaServerService.getOne(mediaServerId);
        if (mediaServer == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "流媒体不存在");
        }
        return mediaServerService.getMediaInfo(mediaServer, app, stream);
    }


    @Operation(summary = "关闭服务", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/shutdown")
    @ResponseBody
    public void shutdown() {
        log.info("正在关闭服务。。。");
        System.exit(1);
    }

    @Operation(summary = "获取系统配置信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

    @Operation(summary = "获取版本信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    @GetMapping(value = "/version")
    @ResponseBody
    public VersionPo VersionPogetVersion() {
        return versionInfo.getVersion();
    }

    @GetMapping(value = "/config")
    @Operation(summary = "获取配置信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

    @GetMapping(value = "/system/info")
    @ResponseBody
    @Operation(summary = "获取系统信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public SystemAllInfo getSystemInfo() {
        SystemAllInfo systemAllInfo = redisCatchStorage.getSystemInfo();

        return systemAllInfo;
    }

    @GetMapping(value = "/media_server/load")
    @ResponseBody
    @Operation(summary = "获取负载信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<MediaServerLoad> getMediaLoad() {
        List<MediaServerLoad> result = new ArrayList<>();
        List<MediaServer> allOnline = mediaServerService.getAllOnline();
        if (allOnline.isEmpty()) {
            return result;
        } else {
            for (MediaServer mediaServerItem : allOnline) {
                result.add(mediaServerService.getLoad(mediaServerItem));
            }
        }
        return result;
    }

    @GetMapping(value = "/resource/info")
    @ResponseBody
    @Operation(summary = "获取负载信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
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

    @GetMapping(value = "/info")
    @ResponseBody
    @Operation(summary = "获取系统信息", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public Map<String, Map<String, String>> getInfo(HttpServletRequest request) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        Map<String, String> hardwareMap = new LinkedHashMap<>();
        result.put("硬件信息", hardwareMap);

        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        // 获取CPU信息
        CentralProcessor.ProcessorIdentifier processorIdentifier = hardware.getProcessor().getProcessorIdentifier();
        hardwareMap.put("CPU", processorIdentifier.getName());
        // 获取内存
        GlobalMemory memory = hardware.getMemory();
        hardwareMap.put("内存", formatByte(memory.getTotal() - memory.getAvailable()) + "/" + formatByte(memory.getTotal()));
        hardwareMap.put("制造商", systemInfo.getHardware().getComputerSystem().getManufacturer());
        hardwareMap.put("产品名称", systemInfo.getHardware().getComputerSystem().getModel());
        // 网卡
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        StringBuilder ips = new StringBuilder();
        for (int i = 0; i < networkIFs.size(); i++) {
            NetworkIF networkIF = networkIFs.get(i);
            String ipsStr = StringUtils.join(networkIF.getIPv4addr());
            if (ObjectUtils.isEmpty(ipsStr)) {
                continue;
            }
            ips.append(ipsStr);
            if (i < networkIFs.size() - 1) {
                ips.append(",");
            }
        }
        hardwareMap.put("网卡", ips.toString());

        Map<String, String> operatingSystemMap = new LinkedHashMap<>();
        result.put("操作系统", operatingSystemMap);
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        operatingSystemMap.put("名称", operatingSystem.getFamily() + " " + operatingSystem.getVersionInfo().getVersion());
        operatingSystemMap.put("类型", operatingSystem.getManufacturer());

        Map<String, String> platformMap = new LinkedHashMap<>();
        result.put("平台信息", platformMap);
        VersionPo version = versionInfo.getVersion();
        platformMap.put("版本", version.getVersion());
        platformMap.put("构建日期", version.getBUILD_DATE());
        platformMap.put("GIT分支", version.getGIT_BRANCH());
        platformMap.put("GIT地址", version.getGIT_URL());
        platformMap.put("GIT日期", version.getGIT_DATE());
        platformMap.put("GIT版本", version.getGIT_Revision_SHORT());
        platformMap.put("DOCKER环境", new File("/.dockerenv").exists()?"是":"否");

        Map<String, String> docmap = new LinkedHashMap<>();
        result.put("文档地址", docmap);
        docmap.put("部署文档", "https://doc.wvp-pro.cn");
        docmap.put("接口文档", String.format("%s://%s:%s/doc.html", request.getScheme(), request.getServerName(), request.getServerPort()));


        return result;
    }

    @GetMapping(value = "/channel/datatype")
    @ResponseBody
    @Operation(summary = "获取系统接入的数据类型", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<Map<String, Object>> getDataType() {
        List<Map<String, Object>> result = new LinkedList<>();
        for (ChannelDataType item : ChannelDataType.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("key", item.desc);
            map.put("value", item.value);
            result.add(map);
        }
        return result;
    }

    /**
     * 单位转换
     */
    private static String formatByte(long byteNumber) {
        //换算单位
        double FORMAT = 1024.0;
        double kbNumber = byteNumber / FORMAT;
        if (kbNumber < FORMAT) {
            return new DecimalFormat("#.##KB").format(kbNumber);
        }
        double mbNumber = kbNumber / FORMAT;
        if (mbNumber < FORMAT) {
            return new DecimalFormat("#.##MB").format(mbNumber);
        }
        double gbNumber = mbNumber / FORMAT;
        if (gbNumber < FORMAT) {
            return new DecimalFormat("#.##GB").format(gbNumber);
        }
        double tbNumber = gbNumber / FORMAT;
        return new DecimalFormat("#.##TB").format(tbNumber);
    }
}
