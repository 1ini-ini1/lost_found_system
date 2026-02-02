package com.zjut.lost_found.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 所有实体的基类（0基础必懂）
 * 作用：统一封装创建时间、修改时间，避免每个实体重复定义
 */
@Data  // Lombok自动生成get/set/toString等方法
@MappedSuperclass  // JPA注解：标识为父类，不映射表，仅被子类继承字段
public class BaseEntity {

    /** 创建时间：数据插入时自动填充 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 修改时间：数据更新时自动填充 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * JPA生命周期注解：插入数据前执行
     * 无需手动调用，保存实体时自动触发
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    /** JPA生命周期注解：更新数据前执行 */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}