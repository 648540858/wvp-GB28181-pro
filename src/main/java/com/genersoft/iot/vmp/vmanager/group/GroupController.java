package com.genersoft.iot.vmp.vmanager.group;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.service.IGroupService;
import com.genersoft.iot.vmp.service.bean.Group;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分组管理")
@RestController
@RequestMapping("/api/group")
public class GroupController {

    private final static Logger logger = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private IGroupService groupService;

    @Operation(summary = "添加区域")
    @Parameter(name = "group", description = "Group", required = true)
    @ResponseBody
    @PostMapping("/add")
    public void add(@RequestBody Group group){
        groupService.add(group);
    }

    @Operation(summary = "查询区域")
    @Parameter(name = "query", description = "查询内容", required = true)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/list")
    public PageInfo<Group> queryGroup(
            @RequestParam(required = false) String query,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
        return groupService.queryGroup(query, page, count);
    }

    @Operation(summary = "更新区域")
    @Parameter(name = "group", description = "Group", required = true)
    @ResponseBody
    @PostMapping("/update")
    public void updateGroup(@RequestBody Group group){
        groupService.update(group);
    }

    @Operation(summary = "删除区域")
    @Parameter(name = "groupDeviceId", description = "待删除的节点编号", required = true)
    @ResponseBody
    @DeleteMapping("/delete")
    public void deleteGroup(String groupDeviceId){
       boolean result = groupService.remove(groupDeviceId);
       if (!result) {
           throw new ControllerException(ErrorCode.ERROR100.getCode(), "移除失败");
       }
    }

    @Operation(summary = "查询区域的子节点")
    @Parameter(name = "groupParentId", description = "待查询的节点", required = true)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @ResponseBody
    @GetMapping("/child/list")
    public PageInfo<Group> queryChildGroupList(
            @RequestParam(required = true) String groupParentId,
            @RequestParam(required = true) int page,
            @RequestParam(required = true) int count
    ){
       return groupService.queryChildGroupList(groupParentId, page, count);
    }
}
