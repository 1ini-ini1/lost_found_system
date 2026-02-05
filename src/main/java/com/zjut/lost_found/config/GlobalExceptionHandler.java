package com.zjut.lost_found.config;

import com.zjut.lost_found.exception.BusinessException;
import com.zjut.lost_found.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器（完整可运行）
 * 优化点：
 * 1. 优先处理自定义业务异常，返回精准状态码和提示
 * 2. 新增UsernameNotFoundException处理（用户不存在）
 * 3. 区分令牌无效的RuntimeException（返回401）
 * 4. 兼容原有异常处理逻辑，不破坏现有功能
 * 5. 处理响应重复写入异常，避免500错误
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 核心优化：优先处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append("：").append(fieldError.getDefaultMessage()).append("；");
        }
        String finalMsg = errorMsg.length() > 0 ? errorMsg.substring(0, errorMsg.length() - 1) : "参数校验失败";
        log.warn("参数校验异常：{}", finalMsg);
        return ApiResponse.fail(400, finalMsg);
    }

    // 处理资源不存在异常
    @ExceptionHandler(EntityNotFoundException.class)
    public ApiResponse<Void> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("资源不存在异常：{}", e.getMessage());
        return ApiResponse.fail(404, e.getMessage());
    }

    // 兼容处理UsernameNotFoundException
    @ExceptionHandler(UsernameNotFoundException.class)
    public ApiResponse<Void> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("用户不存在异常：{}", e.getMessage());
        return ApiResponse.fail(404, "用户不存在");
    }

    // 处理业务逻辑异常（仅捕获未定义的RuntimeException）
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        log.warn("未捕获的运行时异常：{}", e.getMessage());
        if ("用户未登录".equals(e.getMessage())) {
            return ApiResponse.fail(401, "令牌无效或用户未登录");
        }
        return ApiResponse.fail(400, e.getMessage());
    }

    // 处理系统异常（兜底）
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 放行Swagger相关请求
        if (uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui") || uri.startsWith("/webjars")) {
            throw new RuntimeException(e);
        }
        // 静态资源不存在
        if (e instanceof NoResourceFoundException) {
            log.warn("资源不存在：{}", uri);
            return ApiResponse.fail(404, "请求的资源不存在");
        }
        // 解决响应重复写入异常
        if (e.getMessage() != null && e.getMessage().contains("getWriter() has already been called")) {
            log.warn("响应流重复写入异常：{}", e.getMessage());
            return ApiResponse.fail(400, "请求处理失败，请检查令牌或用户信息");
        }
        // 系统异常
        log.error("系统异常", e);
        return ApiResponse.fail(500, "系统异常，请联系管理员");
    }
}