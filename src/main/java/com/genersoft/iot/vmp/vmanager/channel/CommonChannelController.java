package com.genersoft.iot.vmp.vmanager.channel;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.vmanager.bean.*;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Tag(name = "通用国标通道")

@RestController
@RequestMapping("/api/channel")
public class CommonChannelController {

    private final static Logger logger = LoggerFactory.getLogger(CommonChannelController.class);

    @Autowired
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private UserSetting userSetting;


    @Operation(summary = "更新通道信息")
    @Parameter(name = "CommonGbChannel", description = "commonGbChannel", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody CommonGbChannel commonGbChannel
    ){
        commonGbChannelService.updateForForm(commonGbChannel);
    }

    /**
     * TODO 存疑 可以单独创建一个controller
     */
    @Operation(summary = "获取行业编码列表")
    @ResponseBody
    @GetMapping("/industry/list")
    public List<IndustryCodeType> getIndustryCodeList(){
        return commonGbChannelService.getIndustryCodeList();
    }

    /**
     * TODO 存疑 可以单独创建一个controller
     */
    @Operation(summary = "获取编码列表")
    @ResponseBody
    @GetMapping("/type/list")
    public List<DeviceType> getDeviceTypeList(){
        return commonGbChannelService.getDeviceTypeList();
    }

    /**
     * TODO 存疑 可以单独创建一个controller
     */
    @Operation(summary = "获取编码列表")
    @ResponseBody
    @GetMapping("/network/identification/list")
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList(){
        return commonGbChannelService.getNetworkIdentificationTypeList();
    }

    @Operation(summary = "查询分组或区域下的通道")
    @Parameter(name = "groupDeviceId", description = "分组的编号", required = false)
    @Parameter(name = "regionDeviceId", description = "区域的编号", required = false)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "inGroup", description = "是否已经在分组下了", required = false)
    @Parameter(name = "inRegion", description = "是否已经在地区下了", required = false)
    @Parameter(name = "type", description = "通道类型", required = false)
    @Parameter(name = "query", description = "要搜索的内容", required = false)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/list")
    public PageInfo<CommonGbChannel> queryChannelListInGroup(
            @RequestParam(required = false) String groupDeviceId,
            @RequestParam(required = false) String regionDeviceId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String ptzType,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) Boolean inGroup,
            @RequestParam(required = false) Boolean inRegion,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count ){

        if (query != null && ObjectUtils.isEmpty(query.trim())) {
            query = null;
        }
        if (groupDeviceId != null && ObjectUtils.isEmpty(groupDeviceId.trim())) {
            groupDeviceId = null;
        }
        if (regionDeviceId != null && ObjectUtils.isEmpty(regionDeviceId.trim())) {
            regionDeviceId = null;
        }
        if (type != null && ObjectUtils.isEmpty(type.trim())) {
            type = null;
        }
        if (ptzType != null && ObjectUtils.isEmpty(ptzType.trim())) {
            ptzType = null;
        }
        assert !ObjectUtils.isEmpty(groupDeviceId);
        return commonGbChannelService.queryChannelListInGroup(page, count, query, groupDeviceId, regionDeviceId,
                inGroup, inRegion, type, ptzType, online);
    }

    @Operation(summary = "为区域添加分组")
    @ResponseBody
    @PostMapping("/region/update")
    public void updateChannelToRegion(@RequestBody UpdateCommonChannelToRegion params){
        assert params.getCommonGbCivilCode() != null;
        assert !params.getCommonGbIds().isEmpty();
        commonGbChannelService.updateChannelToRegion(params);
    }

    @Operation(summary = "从区域中移除通道")
    @ResponseBody
    @PostMapping("/region/remove")
    public void removeFromRegion(@RequestBody UpdateCommonChannelToRegion params){
        assert params.getCommonGbCivilCode() != null || !params.getCommonGbIds().isEmpty();
        commonGbChannelService.removeFromRegion(params);
    }

    @Operation(summary = "为通道添加分组")
    @ResponseBody
    @PostMapping("/group/update")
    public void updateChannelToGroup(@RequestBody UpdateCommonChannelToGroup params){
        assert params.getCommonGbBusinessGroupID() != null;
        assert !params.getCommonGbIds().isEmpty();
        commonGbChannelService.updateChannelToGroup(params);
    }

    @Operation(summary = "从分组中移除通道")
    @ResponseBody
    @PostMapping("/group/remove")
    public void removeFromGroup(@RequestBody UpdateCommonChannelToGroup params){
        assert params.getCommonGbBusinessGroupID() != null || !params.getCommonGbIds().isEmpty();
        commonGbChannelService.removeFromGroup(params);
    }


    @Operation(summary = "播放通道")
    @Parameter(name = "channelDeviceId", description = "通道国标编号", required = true)
    @ResponseBody
    @GetMapping("/play")
    public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request, String channelDeviceId){
        logger.info("[播放通道] channelDeviceId：{} ", channelDeviceId);
        assert !ObjectUtils.isEmpty(channelDeviceId);

        CommonGbChannel channel = commonGbChannelService.getChannel(channelDeviceId);
        assert channel != null;

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

        result.onTimeout(()->{
            logger.info("[播放通道] 超时 channelDeviceId：{} ", channelDeviceId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("播放通道超时");
            result.setResult(wvpResult);
            commonGbChannelService.stopPlay(channel, null);
        });
        commonGbChannelService.startPlay(channel, (callbackChannel, mediaServerItem, code, message, streamInfo) -> {
            if (code == ErrorCode.SUCCESS.getCode()) {
                WVPResult<StreamContent> wvpResult = new WVPResult<>();
                wvpResult.setCode(ErrorCode.SUCCESS.getCode());
                wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

                if (streamInfo != null) {
                    if (userSetting.getUseSourceIpAsStreamIp()) {
                        streamInfo = streamInfo.clone();//深拷贝
                        String host;
                        try {
                            URL url = new URL(request.getRequestURL().toString());
                            host = url.getHost();
                        } catch (MalformedURLException e) {
                            host = request.getLocalAddr();
                        }
                        streamInfo.changeStreamIp(host);
                    }
                    wvpResult.setData(new StreamContent(streamInfo));
                    result.setResult(wvpResult);
                }
            }else {
                WVPResult<StreamContent> wvpResult = new WVPResult<>();
                wvpResult.setCode(code);
                wvpResult.setMsg(message);
                result.setResult(wvpResult);
                commonGbChannelService.stopPlay(channel, null);
            }
        });
        return result;
    }


    @Operation(summary = "停止播放通道")
    @Parameter(name = "channelDeviceId", description = "通道国标编号", required = true)
    @GetMapping("/stopPlay")
    public void playStop(String channelDeviceId) {

        logger.info("[停止播放通道] channelDeviceId：{} ", channelDeviceId);

        assert !ObjectUtils.isEmpty(channelDeviceId);

        CommonGbChannel channel = commonGbChannelService.getChannel(channelDeviceId);
        assert channel != null;

        DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>();

        result.onTimeout(()->{
            logger.info("[停止播放通道] 超时 channelDeviceId：{} ", channelDeviceId);
            // 释放rtpserver
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(ErrorCode.ERROR100.getCode());
            wvpResult.setMsg("停止播放通道超时");
            result.setResult(wvpResult);
            commonGbChannelService.stopPlay(channel, null);
        });

        commonGbChannelService.stopPlay(channel, (commonGbChannel, mediaServerItem, code, message, streamInfo) -> {
            WVPResult<StreamContent> wvpResult = new WVPResult<>();
            wvpResult.setCode(code);
            wvpResult.setMsg(message);
            result.setResult(wvpResult);
        });
    }

    // 将通道共享到上级平台

    // 从上级平台共享中移除通道


}
