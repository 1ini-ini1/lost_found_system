package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.AuditLog;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.service.AuditLogService;
import com.zjut.lost_found.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志接口控制器（新增）
 * 暴露日志查询接口（审计溯源）
 */
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@Tag(name = "操作日志接口", description = "操作日志查询，用于审计溯源（仅管理员可操作）")
public class AuditLogController extends BaseController {

    private final AuditLogService auditLogService;
    private final UserService userService;

    /**
     * 查询所有操作日志（分页，超级管理员专属）
     */
    @GetMapping("/all")
    @Operation(summary = "查询所有日志", description = "仅超级管理员可调用，查询系统所有操作日志")
    public ApiResponse<Page<AuditLog>> getAllLogs(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        checkSuperAdmin();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<AuditLog> logPage = auditLogService.getAllLogs(pageable);
        return ApiResponse.success("查询成功", logPage);
    }

    /**
     * 按操作用户查询日志（分页）
     */
    @GetMapping("/operator/{userId}")
    @Operation(summary = "按用户查询日志", description = "仅管理员可调用，查询指定用户的操作日志")
    public ApiResponse<Page<AuditLog>> getLogsByOperator(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        checkAdmin();
        User operator = userService.getUserById(userId);
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<AuditLog> logPage = auditLogService.getLogsByOperator(operator, pageable);
        return ApiResponse.success("查询成功", logPage);
    }

    /**
     * 按时间范围查询日志（分页）
     */
    @GetMapping("/time-range")
    @Operation(summary = "按时间范围查询日志", description = "仅管理员可调用，查询指定时间范围内的操作日志")
    public ApiResponse<Page<AuditLog>> getLogsByTimeRange(
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        checkAdmin();
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<AuditLog> logPage = auditLogService.getLogsByTimeRange(startTime, endTime, pageable);
        return ApiResponse.success("查询成功", logPage);
    }
}
