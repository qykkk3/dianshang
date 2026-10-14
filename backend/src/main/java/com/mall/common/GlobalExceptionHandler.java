package com.mall.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把所有异常统一转换为 {@link Result} 结构
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常：@RequestBody + @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR, msg.isEmpty() ? ResultCode.PARAM_ERROR.getMessage() : msg);
    }

    /** 参数校验异常：表单对象绑定 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR, msg.isEmpty() ? ResultCode.PARAM_ERROR.getMessage() : msg);
    }

    /** 参数校验异常：@RequestParam / @PathVariable 上的约束 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return Result.error(ResultCode.PARAM_ERROR, msg);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ResultCode.PARAM_ERROR, "缺少必填参数：" + e.getParameterName());
    }

    /** 请求体格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ResultCode.PARAM_ERROR, "请求体格式错误，请检查提交的数据");
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ResultCode.PARAM_ERROR, "不支持的请求方法：" + e.getMethod());
    }

    /** 认证失败（账号密码错误等） */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        return Result.error(ResultCode.UNAUTHORIZED, "账号或密码错误");
    }

    /** 权限不足 */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.error(ResultCode.FORBIDDEN);
    }

    /** 上传文件过大 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.error(ResultCode.PARAM_ERROR, "上传文件过大，单个文件不能超过 10MB");
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        return Result.error(ResultCode.NOT_FOUND, "接口不存在：" + e.getRequestURL());
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(ResultCode.ERROR);
    }
}
