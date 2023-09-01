package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IRoleService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Tag(name  = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IUserService userService;

    @Autowired
    private IRoleService roleService;

    @GetMapping("/login")
    @PostMapping("/login")
    @Operation(summary = "登录", description = "登录成功后返回AccessToken， 可以从返回值获取到也可以从响应头中获取到，" +
            "后续的请求需要添加请求头 'access-token'或者放在参数里")

    @Parameter(name = "username", description = "用户名", required = true)
    @Parameter(name = "password", description = "密码（32位md5加密）", required = true)
    public LoginUser login(HttpServletRequest request, HttpServletResponse response, @RequestParam String username, @RequestParam String password){
        LoginUser user;
        try {
            user = SecurityUtils.login(username, password, authenticationManager);
        } catch (AuthenticationException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
        if (user == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "用户名或密码错误");
        }else {
            String jwt = JwtUtils.createToken(username);
            response.setHeader(JwtUtils.getHeader(), jwt);
            user.setAccessToken(jwt);
        }
        return user;
    }


    @PostMapping("/changePassword")
    @Operation(summary = "修改密码")
    @Parameter(name = "username", description = "用户名", required = true)
    @Parameter(name = "oldpassword", description = "旧密码（已md5加密的密码）", required = true)
    @Parameter(name = "password", description = "新密码（未md5加密的密码）", required = true)
    public void changePassword(@RequestParam String oldPassword, @RequestParam String password){
        // 获取当前登录用户id
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo== null) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        String username = userInfo.getUsername();
        LoginUser user = null;
        try {
            user = SecurityUtils.login(username, oldPassword, authenticationManager);
            if (user == null) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
            //int userId = SecurityUtils.getUserId();
            boolean result = userService.changePassword(user.getId(), DigestUtils.md5DigestAsHex(password.getBytes()));
            if (!result) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        } catch (AuthenticationException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), e.getMessage());
        }
    }


    @PostMapping("/add")
    @Operation(summary = "添加用户")
    @Parameter(name = "username", description = "用户名", required = true)
    @Parameter(name = "password", description = "密码（未md5加密的密码）", required = true)
    @Parameter(name = "roleId", description = "角色ID", required = true)
    public void add(@RequestParam String username,
                                                 @RequestParam String password,
                                                 @RequestParam Integer roleId){
        if (ObjectUtils.isEmpty(username) || ObjectUtils.isEmpty(password) || roleId == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "参数不可为空");
        }
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // 只用角色id为1才可以删除和添加用户
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "用户无权限");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        //新增用户的pushKey的生成规则为md5(时间戳+用户名)
        user.setPushKey(DigestUtils.md5DigestAsHex((System.currentTimeMillis()+password).getBytes()));
        Role role = roleService.getRoleById(roleId);

        if (role == null) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "角色不存在");
        }
        user.setRole(role);
        user.setCreateTime(DateUtil.getNow());
        user.setUpdateTime(DateUtil.getNow());
        int addResult = userService.addUser(user);
        if (addResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "用户Id", required = true)
    public void delete(@RequestParam Integer id){
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        if (currenRoleId != 1) {
            // 只用角色id为0才可以删除和添加用户
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "用户无权限");
        }
        int deleteResult = userService.deleteUser(id);
        if (deleteResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @GetMapping("/all")
    @Operation(summary = "查询用户")
    public List<User> all(){
        // 获取当前登录用户id
        return userService.getAllUsers();
    }

    /**
     * 分页查询用户
     *
     * @param page  当前页
     * @param count 每页查询数量
     * @return 分页用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "分页查询用户")
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    public PageInfo<User> users(int page, int count) {
        return userService.getUsers(page, count);
    }

    @RequestMapping("/changePushKey")
    @Operation(summary = "修改pushkey")
    @Parameter(name = "userId", description = "用户Id", required = true)
    @Parameter(name = "pushKey", description = "新的pushKey", required = true)
    public void changePushKey(@RequestParam Integer userId,@RequestParam String pushKey) {
        // 获取当前登录用户id
        int currenRoleId = SecurityUtils.getUserInfo().getRole().getId();
        WVPResult<String> result = new WVPResult<>();
        if (currenRoleId != 1) {
            // 只用角色id为0才可以删除和添加用户
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "用户无权限");
        }
        int resetPushKeyResult = userService.changePushKey(userId,pushKey);
        if (resetPushKeyResult <= 0) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
    }

    @PostMapping("/changePasswordForAdmin")
    @Operation(summary = "管理员修改普通用户密码")
    @Parameter(name = "adminId", description = "管理员id", required = true)
    @Parameter(name = "userId", description = "用户id", required = true)
    @Parameter(name = "password", description = "新密码（未md5加密的密码）", required = true)
    public void changePasswordForAdmin(@RequestParam int userId, @RequestParam String password) {
        // 获取当前登录用户id
        LoginUser userInfo = SecurityUtils.getUserInfo();
        if (userInfo == null) {
            throw new ControllerException(ErrorCode.ERROR100);
        }
        Role role = userInfo.getRole();
        if (role != null && role.getId() == 1) {
            boolean result = userService.changePassword(userId, DigestUtils.md5DigestAsHex(password.getBytes()));
            if (!result) {
                throw new ControllerException(ErrorCode.ERROR100);
            }
        }
    }
}
