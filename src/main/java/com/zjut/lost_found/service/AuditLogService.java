package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.AuditLog;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.repository.AuditLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志服务（0基础必懂）
 * 核心功能：记录系统操作（发布、审核、认领等），用于审计溯源
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;  // 注入日志数据访问层

    /**
     * 记录操作日志（通用方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public AuditLog recordLog(User operator, String operationType, String operationDesc, String targetId) {
        // 校验操作用户信息完整
        if (operator == null || operator.getId() == null) {
            throw new RuntimeException("操作用户信息不完整，无法记录日志");
        }

        // 构建日志实体
        AuditLog auditLog = new AuditLog();
        auditLog.setOperator(operator);        // 关联操作用户
        auditLog.setOperationType(operationType);  // 操作类型（如ITEM_PUBLISH）
        auditLog.setOperationDesc(operationDesc);  // 操作详情
        auditLog.setTargetId(targetId);        // 操作目标ID（物品/申请ID）

        // 保存日志（时间自动填充）
        return auditLogRepository.save(auditLog);
    }

    /**
     * 查看日志详情（管理员操作）
     */
    public AuditLog getLogById(Long logId, User queryUser) {
        // 校验权限（仅管理员可查看）
        if (!UserRoleEnum.ADMIN.equals(queryUser.getRole()) && !UserRoleEnum.SUPER_ADMIN.equals(queryUser.getRole())) {
            throw new RuntimeException("无权限查看操作日志");
        }

        return auditLogRepository.findById(logId)
                .orElseThrow(() -> new EntityNotFoundException("日志不存在，ID：" + logId));
    }
}