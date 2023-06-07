package com.genersoft.iot.vmp.vmanager.sip;

import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import com.genersoft.iot.vmp.sip.bean.SipVideo;
import com.genersoft.iot.vmp.sip.service.ISipService;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * SIP接口
 */
@Tag(name = "SIP接口", description = "")
@Controller

@RequestMapping(value = "/api/sip")
public class SipController {

    private final static Logger logger = LoggerFactory.getLogger(SipController.class);

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private ISipService sipService;

    @Operation(summary = "分页获取SIP服务")
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @GetMapping(value = "/server/list")
    @ResponseBody
    public PageInfo<SipServer> getServerList(@RequestParam(required = false)Integer page,
                                    @RequestParam(required = false)Integer count ){

        return sipService.getServerList(page, count);
    }

    @Operation(summary = "添加SIP服务")
    @PostMapping(value = "/server/add")
    @ResponseBody
    public void addServer(@RequestBody SipServer server){
        sipService.addSipServer(server);
    }

    @Operation(summary = "更新SIP服务")
    @PostMapping(value = "/server/update")
    @ResponseBody
    public void updateServer(@RequestBody SipServer server){
        sipService.updateSipServer(server);
    }

    @Operation(summary = "删除SIP服务")
    @DeleteMapping(value = "/server/remove")
    @ResponseBody
    public void deleteServer(Integer serverId){
        sipService.removeSipServer(serverId);
    }



    @Operation(summary = "分页获取SIP帐号")
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @GetMapping(value = "/account/list")
    @ResponseBody
    public PageInfo<SipServerAccount> getAccountList(@RequestParam(required = false)Integer page,
                                                    @RequestParam(required = false)Integer count,
                                                     Integer serverId){

        return sipService.getAccountList(serverId, page, count);
    }

    @Operation(summary = "添加SIP帐号")
    @PostMapping(value = "/account/add")
    @ResponseBody
    public void addServerAccount(@RequestBody SipServerAccount account){
        sipService.addSipServerAccount(account);
    }

    @Operation(summary = "更新SIP帐号")
    @PostMapping(value = "/account/update")
    @ResponseBody
    public void updateServerAccount(@RequestBody SipServerAccount account){
        sipService.updateSipServerAccount(account);
    }

    @Operation(summary = "删除SIP帐号")
    @DeleteMapping(value = "/account/remove")
    @ResponseBody
    public void deleteServerAccount(Integer accountId){
        sipService.removeSipServerAccount(accountId);
    }

    @Operation(summary = "分页获取SIP推送视频")
    @Parameter(name = "page", description = "当前页")
    @Parameter(name = "count", description = "每页查询数量")
    @GetMapping(value = "/video/list")
    @ResponseBody
    public PageInfo<SipVideo> getVideoList(@RequestParam(required = false)Integer page,
                                           @RequestParam(required = false)Integer count,
                                           Integer serverId,
                                           Integer accountId){

        return sipService.getVideoList(serverId, accountId, page, count);
    }

    @Operation(summary = "添加SIP推送视频")
    @PostMapping(value = "/video/add")
    @ResponseBody
    public void addVideo(@RequestBody SipVideo video){
        sipService.addSipVideo(video);
    }

    @Operation(summary = "更新SIP推送视频")
    @PostMapping(value = "/video/update")
    @ResponseBody
    public void updateVideo(@RequestBody SipVideo video){
        sipService.updateSipVideo(video);
    }

    @Operation(summary = "删除SIP推送视频")
    @DeleteMapping(value = "/video/remove")
    @ResponseBody
    public void deleteVideo(Integer videoId){
        sipService.removeSipVideo(videoId);
    }


}
