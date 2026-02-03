package com.zjut.lost_found.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 基础实体（0基础必懂）
 * 所有业务实体都继承此类，统一维护创建时间、更新时间
 * 关键注解说明（JPA注解，用于和数据库映射）：
 * @MappedSuperclass：标识为父类实体，不单独建表，子类继承其字段（如User、Item都会有createTime字段）
 * @EntityListeners(AuditingEntityListener.class)：启用JPA审计监听，自动填充时间（无需手动设置）
 * @Data：Lombok注解，自动生成get/set、toString等方法
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    // 创建时间：新增数据时自动填充，不可修改（如发布物品时自动记录时间）
    @CreatedDate // JPA注解，自动填充创建时间
    @Column(name = "create_time", nullable = false, updatable = false) // 映射数据库字段，非空、不可更新
    private LocalDateTime createTime;

    // 更新时间：新增、修改数据时自动填充（如修改物品信息时，自动更新时间）
    @LastModifiedDate // JPA注解，自动填充更新时间
    @Column(name = "update_time", nullable = false) // 映射数据库字段，非空
    private LocalDateTime updateTime;
}