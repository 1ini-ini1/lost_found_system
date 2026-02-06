package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.AuditLog;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 操作日志核心业务服务（最终优化版）
 * 核心功能：记录操作日志（通用方法，供所有服务调用）、多条件分页查询日志（审计溯源/后台管理）
 * 优化点：规范异常类型、增强参数校验、补充业务日志、优化代码可读性、适配Spring事务规范
 * 适配性：完美对接UserService的日志记录调用，支持操作用户为null的场景（如系统自动操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;  // 注入操作日志数据访问层

    /**
     * 记录操作日志（通用方法，供所有服务调用）
     * @param operator 操作用户（可为null，如系统自动归档/定时任务）
     * @param operationType 操作类型（如USER_REGISTER、ITEM_PUBLISH、ROLE_UPDATE）
     * @param operationDesc 操作详情描述（建议包含操作人、操作目标、操作结果）
     * @param targetId 操作目标ID（如用户ID、物品ID，可为null）
     * @return 保存到数据库的操作日志实体（含自增ID/创建时间）
     * @throws IllegalArgumentException 操作类型/操作详情为空时抛出非法参数异常
     */
    @Transactional(rollbackFor = Exception.class)
    public AuditLog recordLog(User operator, String operationType, String operationDesc, String targetId) {
        // 1. 严格参数校验：操作类型/详情为必填项，语义化异常提示
        if (!StringUtils.hasText(operationType)) {
            log.error("操作日志记录失败：操作类型不能为空或仅含空白字符");
            throw new IllegalArgumentException("操作类型不能为空");
        }
        if (!StringUtils.hasText(operationDesc)) {
            log.error("操作日志记录失败：操作详情描述不能为空或仅含空白字符");
            throw new IllegalArgumentException("操作详情描述不能为空");
        }

        // 2. 构建操作日志实体（自动填充创建时间，继承BaseEntity）
        AuditLog auditLog = new AuditLog();
        auditLog.setOperationType(operationType.trim());
        auditLog.setOperationDesc(operationDesc.trim());
        auditLog.setTargetId(StringUtils.hasText(targetId) ? targetId.trim() : null);
        auditLog.setOperator(operator); // 系统操作时可为null，数据库字段需设置为允许空

        // 3. 保存日志到数据库并返回
        AuditLog savedLog = auditLogRepository.save(auditLog);
        // 日志记录：区分人工操作/系统操作
        String operatorInfo = operator == null ? "系统自动操作" : String.format("用户ID：%d（%s）", operator.getId(), operator.getUsername());
        log.info("操作日志记录成功 | 日志ID：{} | 操作人：{} | 操作类型：{} | 目标ID：{}",
                savedLog.getId(), operatorInfo, operationType, targetId == null ? "无" : targetId);
        log.debug("操作日志详情：{}", operationDesc);

        return savedLog;
    }

    /**
     * 按操作用户分页查询日志（按创建时间倒序，最新日志在前）
     * @param operator 操作用户（非null）
     * @param pageable 分页参数（页码/页大小/排序）
     * @return 分页日志列表
     */
    public Page<AuditLog> getLogsByOperator(User operator, Pageable pageable) {
        log.info("分页查询操作日志 | 条件：操作用户ID{} | 页码：{} | 页大小：{}",
                operator.getId(), pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logPage = auditLogRepository.findByOperatorOrderByCreateTimeDesc(operator, pageable);
        log.info("分页查询操作日志结果 | 符合条件总数：{} | 总页数：{}", logPage.getTotalElements(), logPage.getTotalPages());
        return logPage;
    }

    /**
     * 按操作类型分页查询日志（按创建时间倒序）
     * @param operationType 操作类型（如USER_REGISTER）
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    public Page<AuditLog> getLogsByType(String operationType, Pageable pageable) {
        if (!StringUtils.hasText(operationType)) {
            throw new IllegalArgumentException("操作类型不能为空");
        }
        String type = operationType.trim();
        log.info("分页查询操作日志 | 条件：操作类型{} | 页码：{} | 页大小：{}",
                type, pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logPage = auditLogRepository.findByOperationTypeOrderByCreateTimeDesc(type, pageable);
        log.info("分页查询操作日志结果 | 符合条件总数：{} | 总页数：{}", logPage.getTotalElements(), logPage.getTotalPages());
        return logPage;
    }

    /**
     * 按时间范围分页查询日志（按创建时间倒序）
     * @param startTime 开始时间（非null）
     * @param endTime 结束时间（非null，需晚于开始时间）
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    public Page<AuditLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("开始时间和结束时间不能为空");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        log.info("分页查询操作日志 | 条件：时间范围{}至{} | 页码：{} | 页大小：{}",
                startTime, endTime, pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logPage = auditLogRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(startTime, endTime, pageable);
        log.info("分页查询操作日志结果 | 符合条件总数：{} | 总页数：{}", logPage.getTotalElements(), logPage.getTotalPages());
        return logPage;
    }

    /**
     * 按操作用户+操作类型组合条件分页查询日志（按创建时间倒序）
     * @param operator 操作用户（非null）
     * @param operationType 操作类型（非空）
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    public Page<AuditLog> getLogsByOperatorAndType(User operator, String operationType, Pageable pageable) {
        if (!StringUtils.hasText(operationType)) {
            throw new IllegalArgumentException("操作类型不能为空");
        }
        String type = operationType.trim();
        log.info("分页查询操作日志 | 条件：操作用户ID{}+操作类型{} | 页码：{} | 页大小：{}",
                operator.getId(), type, pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logPage = auditLogRepository.findByOperatorAndOperationTypeOrderByCreateTimeDesc(operator, type, pageable);
        log.info("分页查询操作日志结果 | 符合条件总数：{} | 总页数：{}", logPage.getTotalElements(), logPage.getTotalPages());
        return logPage;
    }

    /**
     * 查询所有操作日志（分页，按创建时间倒序，仅超级管理员可调用）
     * @param pageable 分页参数
     * @return 分页日志列表
     */
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        log.warn("超级管理员操作：分页查询所有操作日志 | 页码：{} | 页大小：{}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<AuditLog> logPage = auditLogRepository.findAll(pageable);
        log.info("查询所有操作日志结果 | 日志总数：{} | 总页数：{}", logPage.getTotalElements(), logPage.getTotalPages());
        return logPage;
    }
}