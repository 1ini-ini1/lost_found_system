package com.zjut.lost_found.config;

import com.zjut.lost_found.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（0基础必懂）
 * 作用：统一捕获系统所有异常，返回标准化错误响应，避免前端收到500错误
 */
@RestControllerAdvice  // 作用于所有@RestController
@Slf4j  // Lombok日志注解，打印异常信息
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（DTO中@NotBlank等注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 获取所有参数错误信息
        BindingResult bindingResult = e.getBindingResult();
        String errorMsg = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));

        log.warn("参数校验失败：{}", errorMsg);
        return ApiResponse.fail(400, errorMsg);
    }

    /**
     * 处理实体不存在异常（如查询ID不存在）
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("实体不存在：{}", e.getMessage());
        return ApiResponse.fail(404, e.getMessage());
    }

    /**
     * 处理业务异常（Service层抛出的RuntimeException）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(RuntimeException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ApiResponse.fail(400, e.getMessage());
    }

    /**
     * 处理系统异常（如空指针、数据库连接失败）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleSystemException(Exception e) {
        log.error("系统异常：", e);  // 打印完整异常栈，便于排查
        return ApiResponse.fail(500, "系统繁忙，请稍后再试");
    }
}