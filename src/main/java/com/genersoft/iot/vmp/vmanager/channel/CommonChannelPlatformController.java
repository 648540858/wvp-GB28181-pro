package com.genersoft.iot.vmp.vmanager.channel;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.service.bean.DeviceType;
import com.genersoft.iot.vmp.service.bean.IndustryCodeType;
import com.genersoft.iot.vmp.service.bean.NetworkIdentificationType;
import com.genersoft.iot.vmp.vmanager.bean.*;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.UpdateChannelParam;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
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
@RequestMapping("/api/channel/platform")
public class CommonChannelPlatformController {

    private final static Logger logger = LoggerFactory.getLogger(CommonChannelPlatformController.class);

    @Autowired
    private ICommonGbChannelService commonGbChannelService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IPlatformChannelService platformChannelService;

    /**
     * 向上级平台添加国标通道
     */
    @Operation(summary = "向上级平台添加国标通道")
    @PostMapping("/add")
    @ResponseBody
    public void addChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台添加国标通道API调用");
        }
        ParentPlatform platform = platformService.queryPlatformByServerGBId(param.getPlatformId());
        if (platform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        if (platform.isShareAllChannel()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "已开启共享所有通道，不需要添加了");
        }
        if (param.getCommonGbChannelIds() == null || param.getCommonGbChannelIds().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        if (platformChannelService.addChannelForGB(platform,param.getCommonGbChannelIds()) <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 从上级平台移除国标通道
     *
     * @param param 通道关联参数
     * @return
     */
    @Operation(summary = "从上级平台移除国标通道")
    @DeleteMapping("/delete")
    @ResponseBody
    public void delChannelForGB(@RequestBody UpdateChannelParam param) {

        if (logger.isDebugEnabled()) {
            logger.debug("给上级平台删除国标通道API调用");
        }
        ParentPlatform platform = platformService.queryPlatformByServerGBId(param.getPlatformId());
        if (platform == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台不存在");
        }
        if (platform.isShareAllChannel()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "已开启共享所有通道，不支持部分移除");
        }
        if (param.getCommonGbChannelIds() == null || param.getCommonGbChannelIds().isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        if (platformChannelService.removeChannelForGB(platform,param.getCommonGbChannelIds()) <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }


}
