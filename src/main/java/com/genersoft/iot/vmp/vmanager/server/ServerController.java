package com.genersoft.iot.vmp.vmanager.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.VManageBootstrap;
import com.genersoft.iot.vmp.common.VersionPo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.conf.VersionInfo;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.utils.SpringBeanFactory;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import gov.nist.javax.sip.SipStackImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

@SuppressWarnings("rawtypes")
@Api(tags = "服务控制")
@CrossOrigin
@RestController
@RequestMapping("/api/server")
public class ServerController {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    VersionInfo versionInfo;

    @Autowired
    SipConfig sipConfig;

    @Autowired
    UserSetup userSetup;

    @Value("${server.port}")
    private int serverPort;


    @ApiOperation("流媒体服务列表")
    @GetMapping(value = "/media_server/list")
    @ResponseBody
    public WVPResult<List<MediaServerItem>> getMediaServerList(boolean detail){
        WVPResult<List<MediaServerItem>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getAll());
        return result;
    }

    @ApiOperation("在线流媒体服务列表")
    @GetMapping(value = "/media_server/online/list")
    @ResponseBody
    public WVPResult<List<MediaServerItem>> getOnlineMediaServerList(){
        WVPResult<List<MediaServerItem>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getAllOnline());
        return result;
    }

    @ApiOperation("获取流媒体服务")
    @GetMapping(value = "/media_server/one/{id}")
    @ResponseBody
    public WVPResult<MediaServerItem> getMediaServer(@PathVariable String id){
        WVPResult<MediaServerItem> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(mediaServerService.getOne(id));
        return result;
    }

    @ApiOperation("测试流媒体服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name="ip", value = "流媒体服务IP", dataTypeClass = String.class),
            @ApiImplicitParam(name="port", value = "流媒体服务HTT端口", dataTypeClass = Integer.class),
            @ApiImplicitParam(name="secret", value = "流媒体服务secret", dataTypeClass = String.class),
    })
    @GetMapping(value = "/media_server/check")
    @ResponseBody
    public WVPResult<MediaServerItem> checkMediaServer(@RequestParam String ip, @RequestParam int port, @RequestParam String secret){
        return mediaServerService.checkMediaServer(ip, port, secret);
    }

    @ApiOperation("测试流媒体录像管理服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name="ip", value = "流媒体服务IP", dataTypeClass = String.class),
            @ApiImplicitParam(name="port", value = "流媒体服务HTT端口", dataTypeClass = Integer.class),
            @ApiImplicitParam(name="secret", value = "流媒体服务secret", dataTypeClass = String.class),
    })
    @GetMapping(value = "/media_server/record/check")
    @ResponseBody
    public WVPResult<String> checkMediaRecordServer(@RequestParam String ip, @RequestParam int port){
        boolean checkResult = mediaServerService.checkMediaRecordServer(ip, port);
        WVPResult<String> result = new WVPResult<>();
        if (checkResult) {
            result.setCode(0);
            result.setMsg("success");

        }else {
            result.setCode(-1);
            result.setMsg("连接失败");
        }
        return result;
    }

    @ApiOperation("保存流媒体服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name="mediaServerItem", value = "流媒体信息", dataTypeClass = MediaServerItem.class)
    })
    @PostMapping(value = "/media_server/save")
    @ResponseBody
    public WVPResult<String> saveMediaServer(@RequestBody  MediaServerItem mediaServerItem){
        MediaServerItem mediaServerItemInDatabase = mediaServerService.getOne(mediaServerItem.getId());

        if (mediaServerItemInDatabase != null) {
            if (StringUtils.isEmpty(mediaServerItemInDatabase.getSendRtpPortRange())
                    && StringUtils.isEmpty(mediaServerItem.getSendRtpPortRange())){
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
           mediaServerService.update(mediaServerItem);
        }else {
            if (StringUtils.isEmpty(mediaServerItem.getSendRtpPortRange())){
                mediaServerItem.setSendRtpPortRange("30000,30500");
            }
            return mediaServerService.add(mediaServerItem);
        }

        WVPResult<String> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        return result;
    }

    @ApiOperation("移除流媒体服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id", value = "流媒体ID", dataTypeClass = String.class)
    })
    @DeleteMapping(value = "/media_server/delete")
    @ResponseBody
    public WVPResult<String> deleteMediaServer(@RequestParam  String id){
        if (mediaServerService.getOne(id) != null) {
            mediaServerService.delete(id);
            mediaServerService.deleteDb(id);
        }else {
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

    @ApiOperation("版本信息")
    @GetMapping(value = "/version")
    @ResponseBody
    public WVPResult<VersionPo> getVersion(){
        WVPResult<VersionPo> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(versionInfo.getVersion());
        return result;
    }

    @ApiOperation("配置信息")
    @GetMapping(value = "/config")
    @ApiImplicitParams({
            @ApiImplicitParam(name="type", value = "配置类型（sip, base）", dataTypeClass = String.class),
    })
    @ResponseBody
    public WVPResult<JSONObject> getVersion(String type){
        WVPResult<JSONObject> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("server.port", serverPort);
        if (StringUtils.isEmpty(type)) {
            jsonObject.put("sip", JSON.toJSON(sipConfig));
            jsonObject.put("base", JSON.toJSON(userSetup));
        }else {
            switch (type){
                case "sip":
                    jsonObject.put("sip", sipConfig);
                    break;
                case "base":
                    jsonObject.put("base", userSetup);
                    break;
                default:
                    break;
            }
        }
        result.setData(jsonObject);
        return result;
    }
}
