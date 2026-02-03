package com.zjut.lost_found.config;

import com.zjut.lost_found.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest; // 新增：导入HttpServletRequest
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException; // 新增：导入静态资源未找到异常

/**
 * 全局异常处理配置（0基础必懂）
 * 作用：统一捕获系统异常，返回标准化响应，避免前端接收混乱错误信息
 */
@RestControllerAdvice  // 全局异常捕获，作用于所有@RestController
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（如@NotBlank、@Pattern校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        // 拼接所有校验失败信息
        StringBuilder errorMsg = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMsg.append(fieldError.getField()).append("：").append(fieldError.getDefaultMessage()).append("；");
        }
        // 返回400参数错误
        return ApiResponse.fail(400, errorMsg.toString().substring(0, errorMsg.length() - 1));
    }

    /**
     * 处理资源不存在异常（如查询不存在的用户、物品）
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ApiResponse<Void> handleEntityNotFoundException(EntityNotFoundException e) {
        // 返回404资源不存在
        return ApiResponse.fail(404, e.getMessage());
    }

    /**
     * 处理业务逻辑异常（如权限不足、重复申请）
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        // 返回400业务错误
        return ApiResponse.fail(400, e.getMessage());
    }

    /**
     * 处理系统异常（兜底）
     * 新增：放行Swagger相关请求，避免/v3/api-docs被包装成500错误
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) { // 新增：HttpServletRequest参数
        // ========== 核心新增逻辑 ==========
        String requestURI = request.getRequestURI();
        // 1. 如果是Swagger相关请求，直接抛出原始异常（让SpringDoc处理，不包装）
        if (requestURI.startsWith("/v3/api-docs") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/webjars")) {
            throw new RuntimeException(e);
        }
        // 2. 如果是静态资源未找到异常（非Swagger），返回404
        if (e instanceof NoResourceFoundException) {
            return ApiResponse.fail(404, "请求的资源不存在");
        }
        // ========== 原有逻辑保留 ==========
        // 返回500系统错误，隐藏具体异常信息，避免泄露
        return ApiResponse.fail(500, "系统异常，请联系管理员");
    }
}