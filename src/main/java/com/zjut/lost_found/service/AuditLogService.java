package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.AuditLog;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志核心业务服务（新增）
 * 核心功能：记录操作日志、多条件查询日志（审计溯源）
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;  // 注入操作日志数据访问层

    /**
     * 记录操作日志（通用方法，供所有服务调用）
     * @param operator 操作用户（可为null，如自动归档）
     * @param operationType 操作类型（如USER_REGISTER、ITEM_PUBLISH）
     * @param operationDesc 操作详情描述
     * @param targetId 操作目标ID
     */
    @Transactional(rollbackFor = Exception.class)
    public AuditLog recordLog(User operator, String operationType, String operationDesc, String targetId) {
        // 1. 校验必填参数
        if (operationType == null || operationType.trim().isEmpty()) {
            throw new RuntimeException("操作类型不能为空");
        }
        if (operationDesc == null || operationDesc.trim().isEmpty()) {
            throw new RuntimeException("操作详情不能为空");
        }

        // 2. 构建操作日志实体
        AuditLog auditLog = new AuditLog();
        auditLog.setOperationType(operationType);
        auditLog.setOperationDesc(operationDesc);
        auditLog.setTargetId(targetId);
        auditLog.setOperator(operator); // 自动归档等操作可为null，数据库允许空值

        // 3. 保存日志到数据库（时间自动填充，继承BaseEntity）
        return auditLogRepository.save(auditLog);
    }

    /**
     * 按操作用户查询日志（分页）
     */
    public Page<AuditLog> getLogsByOperator(User operator, Pageable pageable) {
        return auditLogRepository.findByOperatorOrderByCreateTimeDesc(operator,pageable);
    }
    /**
     * 按操作类型查询日志（分页）
     */
    public Page<AuditLog> getLogsByType(String operationType, Pageable pageable) {
        return auditLogRepository.findByOperationTypeOrderByCreateTimeDesc(operationType, pageable);
    }

    /**
     * 按时间范围查询日志（分页）
     */
    public Page<AuditLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditLogRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(startTime, endTime, pageable);
    }

    /**
     * 多条件查询日志（操作用户+操作类型，分页）
     */
    public Page<AuditLog> getLogsByOperatorAndType(User operator, String operationType, Pageable pageable) {
        return auditLogRepository.findByOperatorAndOperationTypeOrderByCreateTimeDesc(operator, operationType, pageable);
    }

    /**
     * 查询所有日志（分页，仅超级管理员可调用）
     */
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
