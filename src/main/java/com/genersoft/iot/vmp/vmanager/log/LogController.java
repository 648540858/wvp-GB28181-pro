package com.genersoft.iot.vmp.vmanager.log;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.service.ILogService;
import com.genersoft.iot.vmp.storager.dao.dto.LogDto;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@Tag(name  = "日志管理")
@CrossOrigin
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final static Logger logger = LoggerFactory.getLogger(LogController.class);

    @Autowired
    private ILogService logService;

    @Autowired
    private UserSetting userSetting;

    /**
     *  分页查询日志
     *
     * @param query 查询内容
     * @param page 当前页
     * @param count 每页查询数量
     * @param type  类型
     * @param startTime  开始时间
     * @param endTime 结束时间
     * @return
     */
    @GetMapping("/all")
    @Operation(summary = "分页查询报警")
    @Parameter(name = "query", description = "查询内容", required = true)
    @Parameter(name = "page", description = "当前页", required = true)
    @Parameter(name = "count", description = "每页查询数量", required = true)
    @Parameter(name = "type", description = "类型", required = true)
    @Parameter(name = "startTime", description = "开始时间", required = true)
    @Parameter(name = "endTime", description = "结束时间", required = true)
    public ResponseEntity<PageInfo<LogDto>> getAll(
            @RequestParam int page,
            @RequestParam int count,
            @RequestParam(required = false)  String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        if (StringUtils.isEmpty(query)) {
            query = null;
        }
        if (StringUtils.isEmpty(startTime)) {
            startTime = null;
        }
        if (StringUtils.isEmpty(endTime)) {
            endTime = null;
        }
        if (!userSetting.getLogInDatebase()) {
            logger.warn("自动记录日志功能已关闭，查询结果可能不完整。");
        }

        if (!DateUtil.verification(startTime, DateUtil.formatter) || !DateUtil.verification(endTime, DateUtil.formatter)){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        PageInfo<LogDto> allLog = logService.getAll(page, count, query, type, startTime, endTime);
        return new ResponseEntity<>(allLog, HttpStatus.OK);
    }

    /**
     *  清空日志
     *
     */
    @Operation(summary = "停止视频回放")
    @DeleteMapping("/clear")
    public ResponseEntity<WVPResult<String>> clear() {

        int count = logService.clear();
        WVPResult wvpResult = new WVPResult();
        wvpResult.setCode(0);
        wvpResult.setMsg("success");
        wvpResult.setData(count);
        return new ResponseEntity<WVPResult<String>>(wvpResult, HttpStatus.OK);
    }

}
