package com.genersoft.iot.vmp.gb28181.controller;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "分组管理")
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private IGroupService groupService;

    @Operation(summary = "添加区域")
    @Parameter(name = "group", description = "group", required = true)
    @ResponseBody
    @PostMapping("/add")
    public void add(@RequestBody Group group){
        groupService.add(group);
    }

    @Operation(summary = "查询区域")
    @Parameter(name = "query", description = "要搜索的内容", required = true)
    @Parameter(name = "parent", description = "所属分组编号", required = true)
    @ResponseBody
    @GetMapping("/tree/list")
    public List<GroupTree> queryForTree(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String parent
    ){
        if (ObjectUtils.isEmpty(parent)) {
            parent = null;
        }
        if (ObjectUtils.isEmpty(query)) {
            query = null;
        }
        return groupService.queryForTree(query, parent);
    }

    @Operation(summary = "更新区域")
    @Parameter(name = "group", description = "Group", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void update(@RequestBody Group group){
        groupService.update(group);
    }

    @Operation(summary = "删除区域")
    @Parameter(name = "deviceId", description = "区域编码", required = true)
    @ResponseBody
    @DeleteMapping("/delete")
    public void delete(String deviceId){
        Assert.hasLength(deviceId, "区域编码（deviceId）不需要存在");
        boolean result = groupService.deleteByDeviceId(deviceId);
        if (!result) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "移除失败");
        }
    }

    @Operation(summary = "根据区域Id查询区域")
    @Parameter(name = "groupDeviceId", description = "分组节点编号", required = true)
    @ResponseBody
    @GetMapping("/one")
    public Group queryGroupByDeviceId(
            @RequestParam(required = true) String deviceId
    ){
        Assert.hasLength(deviceId, "");
        return groupService.queryGroupByDeviceId(deviceId);
    }

    @Operation(summary = "获取所属的分组下的分组")
    @Parameter(name = "parent", description = "所属的分组", required = false)
    @ResponseBody
    @GetMapping("/base/child/list")
    public List<Group> getAllChild(@RequestParam(required = false) String parent){
        if (ObjectUtils.isEmpty(parent)) {
            parent = null;
        }
        return groupService.getAllChild(parent);
    }

    @Operation(summary = "从通道中同步分组")
    @ResponseBody
    @GetMapping("/sync")
    public void sync(){
        groupService.syncFromChannel();
    }
}
