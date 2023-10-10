package com.genersoft.iot.vmp.vmanager.gb28181.gbStream;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.service.IPlatformService;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.gb28181.gbStream.bean.GbStreamParam;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name  = "视频流关联到级联平台")

@RestController
@RequestMapping("/api/gbStream")
public class GbStreamController {

    private final static Logger logger = LoggerFactory.getLogger(GbStreamController.class);

    @Autowired
    private IGbStreamService gbStreamService;

    @Autowired
    private IPlatformService platformService;


    /**
     * 查询国标通道
     * @param page 当前页
     * @param count 每页条数
     * @param platformId 平台ID
     * @return
     */
    @Operation(summary = "查询国标通道")
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页条数", required = true)
    @Parameter(name = "platformId", description = "平台ID", required = true)
    @Parameter(name = "catalogId", description = "目录ID")
    @Parameter(name = "query", description = "查询内容")
    @Parameter(name = "mediaServerId", description = "流媒体ID")
    @GetMapping(value = "/list")
    @ResponseBody
    public PageInfo<GbStream> list(@RequestParam(required = true)Integer page,
                                   @RequestParam(required = true)Integer count,
                                   @RequestParam(required = true)String platformId,
                                   @RequestParam(required = false)String catalogId,
                                   @RequestParam(required = false)String query,
                                   @RequestParam(required = false)String mediaServerId){
        if (ObjectUtils.isEmpty(catalogId)) {
            catalogId = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        if (ObjectUtils.isEmpty(mediaServerId)) {
            mediaServerId = null;
        }

        // catalogId 为null 查询未在平台下分配的数据
        // catalogId 不为null 查询平台下这个，目录下的通道
        return gbStreamService.getAll(page, count, platformId, catalogId, query, mediaServerId);
    }


    /**
     * 移除国标关联
     * @param gbStreamParam
     * @return
     */
    @Operation(summary = "移除国标关联")
    @DeleteMapping(value = "/del")
    @ResponseBody
    public void del(@RequestBody GbStreamParam gbStreamParam){

        if (gbStreamParam.getGbStreams() == null || gbStreamParam.getGbStreams().size() == 0) {
            if (gbStreamParam.isAll()) {
                gbStreamService.delAllPlatformInfo(gbStreamParam.getPlatformId(), gbStreamParam.getCatalogId());
            }
        }else {
            gbStreamService.delPlatformInfo(gbStreamParam.getPlatformId(), gbStreamParam.getGbStreams());
        }

    }

    /**
     * 保存国标关联
     * @param gbStreamParam
     * @return
     */
    @Operation(summary = "保存国标关联")
    @PostMapping(value = "/add")
    @ResponseBody
    public void add(@RequestBody GbStreamParam gbStreamParam){
        if (gbStreamParam.getGbStreams() == null || gbStreamParam.getGbStreams().size() == 0) {
            if (gbStreamParam.isAll()) {
                List<GbStream> allGBChannels = gbStreamService.getAllGBChannels(gbStreamParam.getPlatformId());
                gbStreamService.addPlatformInfo(allGBChannels, gbStreamParam.getPlatformId(), gbStreamParam.getCatalogId());
            }
        }else {
            gbStreamService.addPlatformInfo(gbStreamParam.getGbStreams(), gbStreamParam.getPlatformId(), gbStreamParam.getCatalogId());
        }
    }

    /**
     * 保存国标关联
     * @param gbId
     * @return
     */
    @Operation(summary = "保存国标关联")
    @GetMapping(value = "/addWithGbid")
    @ResponseBody
    public void add(String gbId, String platformGbId, @RequestParam(required = false) String catalogGbId){
        List<GbStream> gbStreams = gbStreamService.getGbChannelWithGbid(gbId);
        if (gbStreams.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "gbId的信息未找到");
        }
        gbStreamService.addPlatformInfo(gbStreams, platformGbId, catalogGbId);
    }
}
