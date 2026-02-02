
package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 物品状态枚举（0基础必懂）
 * 作用：规范物品的所有可能状态，避免状态值混乱
 * 核心设计：每个枚举常量对应“数据库存储编码”和“前端展示描述”，兼顾存储和展示需求
 */
@Getter  // Lombok自动生成getter方法，用于获取code和desc
public enum ItemStatusEnum {
    PENDING_AUDIT("PENDING_AUDIT", "待审核"),  // 发布后未审核：用户发布物品后，等待管理员审核
    PASSED("PASSED", "已通过"),                // 审核通过，可认领：管理员审核通过后，其他用户可提交认领申请
    REJECTED("REJECTED", "已驳回"),            // 已驳回：管理员审核不通过（如信息不完整）
    CLAIMED("CLAIMED", "已认领"),              // 已认领：认领申请审核通过，物品归还给失主
    CANCELED("CANCELED", "已取消"),            // 已取消：发布人主动取消物品发布
    ARCHIVED("ARCHIVED", "已归档");            // 已归档：已通过审核但超期（如30天）未认领，系统自动归档

    // 状态编码（数据库存储）：字符串类型，避免数字编码的语义不明确问题
    // 状态描述（前端展示）：中文，提升用户体验
    private final String code;
    private final String desc;

    // 构造方法（枚举类固定写法，默认private，不可修改）
    // 作用：初始化每个枚举常量的code和desc
    ItemStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}