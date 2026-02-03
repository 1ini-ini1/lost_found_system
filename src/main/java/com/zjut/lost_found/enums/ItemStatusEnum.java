package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 物品状态枚举（0基础必懂）
 * 作用：规范物品的所有可能状态，避免状态值混乱
 */
@Getter  // Lombok自动生成getter方法
public enum ItemStatusEnum {
    PENDING_AUDIT("PENDING_AUDIT", "待审核"),  // 发布后未审核
    PASSED("PASSED", "已通过"),                // 审核通过，可认领
    REJECTED("REJECTED", "已驳回"),            // 审核不通过
    CLAIMED("CLAIMED", "已认领"),              // 被成功认领
    CANCELED("CANCELED", "已取消"),            // 发布人取消发布
    ARCHIVED("ARCHIVED", "已归档");            // 长期未认领自动归档

    // 状态编码（数据库存储）、状态描述（前端展示）
    private final String code;
    private final String desc;

    // 构造方法（枚举类固定写法）
    ItemStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}