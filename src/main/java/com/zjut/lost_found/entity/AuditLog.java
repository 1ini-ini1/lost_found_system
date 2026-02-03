package com.zjut.lost_found.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 操作日志实体（对应数据库audit_logs表）
 * 存储系统所有核心操作，用于审计溯源
 */
@Data
@Entity
@Table(name = "audit_logs")
@DynamicUpdate
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 日志唯一ID

    @Column(name = "operation_type", nullable = false)
    private String operationType;  // 操作类型（如ITEM_PUBLISH、CLAIM_SUBMIT）

    @Column(name = "operation_desc", columnDefinition = "TEXT", nullable = false)
    private String operationDesc;  // 操作详情描述

    @Column(name = "target_id")
    private String targetId;  // 操作目标ID（物品ID、申请ID等）

    // 多对一关联：多个日志对应一个操作用户
    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;  // 操作用户
}