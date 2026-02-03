package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 物品状态枚举（0基础必懂）
 * 跟踪物品从发布到归档的全生命周期，每个状态对应不同的业务逻辑
 */
@Getter
public enum ItemStatusEnum {
    // 枚举常量：覆盖物品的所有可能状态
    PENDING_AUDIT("待审核", "PENDING_AUDIT"), // 发布后等待管理员审核，普通用户看不到
    PASSED("已通过", "PASSED"), // 审核通过，可被其他用户看到并认领
    REJECTED("已驳回", "REJECTED"), // 审核驳回，无法展示，需发布人修改后重新提交
    CLAIMED("已认领", "CLAIMED"), // 被成功认领，流程结束
    ARCHIVED("已归档", "ARCHIVED"); // 超期未认领，系统自动归档

    private final String desc; // 状态描述（前端展示）
    private final String value; // 状态值（数据库存储）

    // 构造器：初始化枚举常量
    ItemStatusEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    // 根据value获取枚举（数据库查询后转换用）
    public static ItemStatusEnum getByValue(String value) {
        for (ItemStatusEnum status : ItemStatusEnum.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return PENDING_AUDIT; // 默认返回待审核
    }
}