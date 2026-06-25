package com.genersoft.iot.vmp.conf;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 默认异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public WVPResult<String> exceptionHandler(Exception e) {
        log.error("[全局异常]： ", e);
        return WVPResult.fail(ErrorCode.ERROR500.getCode(), e.getMessage());
    }

    /**
     * 默认异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public WVPResult<String> exceptionHandler(MaxUploadSizeExceededException e) {
        return WVPResult.fail(ErrorCode.ERROR403);
    }

    /**
     * 默认异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public WVPResult<String> exceptionHandler(NoResourceFoundException e) {
        return WVPResult.fail(ErrorCode.ERROR404);
    }

    /**
     * 默认异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WVPResult<String> exceptionHandler(IllegalStateException e) {
        return WVPResult.fail(ErrorCode.ERROR400);
    }

    /**
     * 默认异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WVPResult<String> exceptionHandler(HttpRequestMethodNotSupportedException e) {
        return WVPResult.fail(ErrorCode.ERROR400);
    }
    /**
     * 断言异常处理
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public WVPResult<String> exceptionHandler(IllegalArgumentException e) {
        return WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMessage());
    }


    /**
     * 自定义异常处理， 处理controller中返回的错误
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(ControllerException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<WVPResult<String>> exceptionHandler(ControllerException e) {
        return new ResponseEntity<>(WVPResult.fail(e.getCode(), e.getMsg()), HttpStatus.OK);
    }

    /**
     * 登陆失败
     * @param e 异常
     * @return 统一返回结果
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<WVPResult<String>> exceptionHandler(BadCredentialsException e) {
        return new ResponseEntity<>(WVPResult.fail(ErrorCode.ERROR100.getCode(), e.getMessage()), HttpStatus.OK);
    }
}
