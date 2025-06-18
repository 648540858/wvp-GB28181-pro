package com.genersoft.iot.vmp.gat1400.framework.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.genersoft.iot.vmp.gat1400.framework.domain.core.BaseResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "cz.data.viid.fe.api")
public class GlobalExceptionHandler {

    /**
     * 请求的 JSON 参数在请求体内的参数校验
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(","));
        log.warn("参数校验异常信息 ex={}", e.getMessage());
        return BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    /**
     * 请求的 URL 参数检验
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(","));
        log.warn("参数校验异常信息 ex={}", e.getMessage());
        return BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("http参数转化错误: {}", e.getMessage());
        String message;
        if (e.contains(InvalidFormatException.class)) {
            message = "参数格式错误";
        } else {
            message = e.getMessage();
        }
        return BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse handleException(Exception e) {
        log.error(String.format("自定义异常信息 ex=%s", e.getMessage()), e);
        return BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    @ExceptionHandler(VIIDRuntimeException.class)
    public BaseResponse handleVIIDException(VIIDRuntimeException e) {
        log.error(e.getMessage());
        return BaseResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
}
