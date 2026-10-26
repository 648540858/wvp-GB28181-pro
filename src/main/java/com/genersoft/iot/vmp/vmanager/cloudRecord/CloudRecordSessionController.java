package com.genersoft.iot.vmp.vmanager.cloudRecord;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.ICloudRecordSessionService;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionContent;
import com.genersoft.iot.vmp.vmanager.cloudRecord.bean.CloudRecordSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 通过既有WVP认证提供文件会话控制，所有媒体身份由服务端生成。 */
@RestController
@RequestMapping("/api/cloud/record/sessions")
@Tag(name = "云录像独立会话")
@SecurityRequirement(name = JwtUtils.HEADER)
public class CloudRecordSessionController {
    @Autowired
    private ICloudRecordSessionService sessionService;
    @Autowired
    private UserSetting userSetting;

    /** 创建仅使用已有云录像文件的独立观看会话。 */
    @PostMapping
    @Operation(summary = "创建已有云录像的独立观看会话")
    public CloudRecordSessionContent create(HttpServletRequest request,
                                            @RequestBody CloudRecordSessionRequest param) {
        if (param.getCloudRecordId() == null || param.getCloudRecordId() <= 0) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        String streamHost = userSetting.getUseSourceIpAsStreamIp() ? request.getServerName() : null;
        return sessionService.create(currentUserId(), param.getCloudRecordId(),
                param.getPositionMs() == null ? 0 : param.getPositionMs(), streamHost);
    }

    /** 查询会话而不延长暂停或失联会话的生存时间。 */
    @GetMapping("/{sessionId}")
    @Operation(summary = "查询独立观看会话")
    public CloudRecordSessionContent get(
            @Parameter(description = "创建时返回的会话ID", required = true) @PathVariable String sessionId) {
        return sessionService.get(currentUserId(), sessionId);
    }

    /** 定位当前会话，播放中返回新媒体地址，暂停中仅改变保存的位置。 */
    @PostMapping("/{sessionId}/seek")
    @Operation(summary = "定位独立观看会话")
    public CloudRecordSessionContent seek(
            @Parameter(description = "创建时返回的会话ID", required = true) @PathVariable String sessionId,
            @RequestBody CloudRecordSessionRequest param) {
        return sessionService.seek(currentUserId(), sessionId, requiredPosition(param));
    }

    /** 使用调用方实际播放位置暂停，不把媒体倍速零当作暂停协议。 */
    @PostMapping("/{sessionId}/pause")
    @Operation(summary = "保存实际播放位置并暂停独立会话")
    public CloudRecordSessionContent pause(
            @Parameter(description = "创建时返回的会话ID", required = true) @PathVariable String sessionId,
            @RequestBody CloudRecordSessionRequest param) {
        return sessionService.pause(currentUserId(), sessionId, requiredPosition(param));
    }

    /** 从暂停位置重新加载同一文件，只为当前会话建立媒体源。 */
    @PostMapping("/{sessionId}/resume")
    @Operation(summary = "恢复独立观看会话")
    public CloudRecordSessionContent resume(
            @Parameter(description = "创建时返回的会话ID", required = true) @PathVariable String sessionId) {
        return sessionService.resume(currentUserId(), sessionId);
    }

    /** 幂等释放当前观看会话，不调用设备BYE或删除录像文件。 */
    @DeleteMapping("/{sessionId}")
    @Operation(summary = "关闭独立观看会话")
    public void close(
            @Parameter(description = "创建时返回的会话ID", required = true) @PathVariable String sessionId) {
        sessionService.close(currentUserId(), sessionId);
    }

    /** 位置控制必须由播放器明确提供位置，避免暂停后恢复到错误时间。 */
    private long requiredPosition(CloudRecordSessionRequest param) {
        if (param.getPositionMs() == null) {
            throw new ControllerException(ErrorCode.ERROR400);
        }
        return param.getPositionMs();
    }

    /** 关闭接口认证时沿用现有匿名模式，避免空principal导致空指针。 */
    private int currentUserId() {
        LoginUser user = SecurityUtils.getUserInfo();
        return user == null ? 0 : user.getId();
    }
}
