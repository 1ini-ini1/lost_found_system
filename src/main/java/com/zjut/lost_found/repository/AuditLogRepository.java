package com.zjut.lost_found.repository;

import com.zjut.lost_found.entity.AuditLog;
import com.zjut.lost_found.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层（新增）
 * 提供日志的CRUD及多条件查询功能
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    // 按操作用户查询日志
    Page<AuditLog> findByOperatorOrderByCreateTimeDesc(User operator, Pageable pageable);

    // 按操作类型查询日志
    Page<AuditLog> findByOperationTypeOrderByCreateTimeDesc(String operationType, Pageable pageable);

    // 按时间范围查询日志
    Page<AuditLog> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // 按操作用户和操作类型查询日志
    Page<AuditLog> findByOperatorAndOperationTypeOrderByCreateTimeDesc(User operator, String operationType, Pageable pageable);
}