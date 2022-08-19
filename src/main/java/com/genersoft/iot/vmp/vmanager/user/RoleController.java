package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.service.IRoleService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name  = "角色管理")
@CrossOrigin
@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @PostMapping("/add")
    @Operation(summary = "添加角色")
    @Parameter(name = "name", description = "角色名", required = true)
    @Parameter(name = "authority", description = "权限（自行定义内容，目前未使用）", required = true)
    public ResponseEntity<WVPResult<Integer>> add(@RequestParam String name,
                                                  @RequestParam(required = false) String authority){
        WVPResult<Integer> result = new WVPResult<>();
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // 只用角色id为1才可以删除和添加用户
            result.setCode(-1);
            result.setMsg("用户无权限");
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }

        Role role = new Role();
        role.setName(name);
        role.setAuthority(authority);
        role.setCreateTime(DateUtil.getNow());
        role.setUpdateTime(DateUtil.getNow());

        int addResult = roleService.add(role);

        result.setCode(addResult > 0 ? 0 : -1);
        result.setMsg(addResult > 0 ? "success" : "fail");
        result.setData(addResult);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除角色")
    @Parameter(name = "id", description = "用户Id", required = true)
    public ResponseEntity<WVPResult<String>> delete(@RequestParam Integer id){
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        WVPResult<String> result = new WVPResult<>();
        if (currenRoleId != 1) {
            // 只用角色id为0才可以删除和添加用户
            result.setCode(-1);
            result.setMsg("用户无权限");
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        }
        int deleteResult = roleService.delete(id);

        result.setCode(deleteResult>0? 0 : -1);
        result.setMsg(deleteResult>0? "success" : "fail");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/all")
    @Operation(summary = "查询角色")
    public ResponseEntity<WVPResult<List<Role>>> all(){
        // 获取当前登录用户id
        List<Role> allRoles = roleService.getAll();
        WVPResult<List<Role>> result = new WVPResult<>();
        result.setCode(0);
        result.setMsg("success");
        result.setData(allRoles);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
