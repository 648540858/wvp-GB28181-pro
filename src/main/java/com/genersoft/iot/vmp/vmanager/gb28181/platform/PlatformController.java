package com.genersoft.iot.vmp.vmanager.gb28181.platform;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatformCatch;
import com.genersoft.iot.vmp.gb28181.bean.PlatformCatalog;
import com.genersoft.iot.vmp.gb28181.bean.SubscribeHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.UpdateChannelParam;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import com.genersoft.iot.vmp.conf.SipConfig;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 级联平台管理
 */
@Tag(name  = "级联平台管理")

@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final static Logger logger = LoggerFactory.getLogger(PlatformController.class);

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SubscribeHolder subscribeHolder;

    @Autowired
    private SipConfig sipConfig;

	@Autowired
	private IPlatformService platformService;

    /**
     * 获取国标服务的配置
     *
     * @return
     */
    @Operation(summary = "获取国标服务的配置")
    @GetMapping("/server_config")
    public JSONObject serverConfig() {
        JSONObject result = new JSONObject();
        result.put("deviceIp", sipConfig.getIp());
        result.put("devicePort", sipConfig.getPort());
        result.put("username", sipConfig.getId());
        result.put("password", sipConfig.getPassword());
        return result;
    }

    /**
     * 获取级联服务器信息
     *
     * @return
     */
    @Operation(summary = "获取级联服务器信息")
    @Parameter(name = "id", description = "平台国标编号", required = true)
    @GetMapping("/info/{id}")
    public ParentPlatform getPlatform(@PathVariable String id) {
        ParentPlatform parentPlatform = platformService.queryPlatformByServerGBId(id);
        if (parentPlatform != null) {
            return  parentPlatform;
        } else {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "未查询到此平台");
        }
    }

    /**
     * 分页查询级联平台
     *
     * @param page  当前页
     * @param count 每页条数
     * @return
     */
    @GetMapping("/query/{count}/{page}")
    @Operation(summary = "分页查询级联平台")
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    public PageInfo<ParentPlatform> platforms(@PathVariable int page, @PathVariable int count) {

        PageInfo<ParentPlatform> parentPlatformPageInfo = platformService.queryParentPlatformList(page, count);
        if (parentPlatformPageInfo.getList().size() > 0) {
            for (ParentPlatform platform : parentPlatformPageInfo.getList()) {
                platform.setMobilePositionSubscribe(subscribeHolder.getMobilePositionSubscribe(platform.getId()) != null);
                platform.setCatalogSubscribe(subscribeHolder.getCatalogSubscribe(platform.getId()) != null);
            }
        }
        return parentPlatformPageInfo;
    }

    /**
     * 添加上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @Operation(summary = "添加上级平台信息")
    @PostMapping("/add")
    @ResponseBody
    public void addPlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIP())
                || ObjectUtils.isEmpty(parentPlatform.getServerPort())
                || ObjectUtils.isEmpty(parentPlatform.getDeviceGBId())
                || ObjectUtils.isEmpty(parentPlatform.getExpires())
                || ObjectUtils.isEmpty(parentPlatform.getKeepTimeout())
                || ObjectUtils.isEmpty(parentPlatform.getTransport())
                || ObjectUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        if (parentPlatform.getServerPort() < 0 || parentPlatform.getServerPort() > 65535) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "error severPort");
        }

        ParentPlatform parentPlatformOld = storager.queryParentPlatByServerGBId(parentPlatform.getServerGBId());
        if (parentPlatformOld != null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "平台 " + parentPlatform.getServerGBId() + " 已存在");
        }
        parentPlatform.setCreateTime(DateUtil.getNow());
        parentPlatform.setUpdateTime(DateUtil.getNow());
        boolean updateResult = platformService.add(parentPlatform);

        if (!updateResult) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"写入数据库失败");
        }
    }

    /**
     * 保存上级平台信息
     *
     * @param parentPlatform
     * @return
     */
    @Operation(summary = "保存上级平台信息")
    @PostMapping("/save")
    @ResponseBody
    public void savePlatform(@RequestBody ParentPlatform parentPlatform) {

        if (logger.isDebugEnabled()) {
            logger.debug("保存上级平台信息API调用");
        }
        if (ObjectUtils.isEmpty(parentPlatform.getName())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBId())
                || ObjectUtils.isEmpty(parentPlatform.getServerGBDomain())
                || ObjectUtils.isEmpty(parentPlatform.getServerIP())
                || ObjectUtils.isEmpty(parentPlatform.getServerPort())
                || ObjectUtils.isEmpty(parentPlatform.getDeviceGBId())
                || ObjectUtils.isEmpty(parentPlatform.getExpires())
                || ObjectUtils.isEmpty(parentPlatform.getKeepTimeout())
                || ObjectUtils.isEmpty(parentPlatform.getTransport())
                || ObjectUtils.isEmpty(parentPlatform.getCharacterSet())
        ) {
            throw new ControllerException(ErrorCode.ERROR400);
        }

        platformService.update(parentPlatform);
    }

    /**
     * 删除上级平台
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @Operation(summary = "删除上级平台")
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @DeleteMapping("/delete/{serverGBId}")
    @ResponseBody
    public void deletePlatform(@PathVariable String serverGBId) {

        if (logger.isDebugEnabled()) {
            logger.debug("删除上级平台API调用");
        }
        if (ObjectUtils.isEmpty(serverGBId)) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        boolean deleteResult = platformService.delete(serverGBId);
        if (!deleteResult) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    /**
     * 查询上级平台是否存在
     *
     * @param serverGBId 上级平台国标ID
     * @return
     */
    @Operation(summary = "查询上级平台是否存在")
    @Parameter(name = "serverGBId", description = "上级平台的国标编号")
    @GetMapping("/exit/{serverGBId}")
    @ResponseBody
    public Boolean exitPlatform(@PathVariable String serverGBId) {

        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(serverGBId);
        return parentPlatform != null;
    }

}
