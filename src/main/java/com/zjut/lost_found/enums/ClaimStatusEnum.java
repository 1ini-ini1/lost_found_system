package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 认领申请状态枚举（覆盖全生命周期）
 * 兼容测试用例中的PASSED值，补充状态描述和工具方法
 */
@Getter
public enum ClaimStatusEnum {
    PENDING_AUDIT("待审核"),  // 提交认领后默认状态
    APPROVED("已通过"),         // 兼容测试用例的核心值
    REJECTED("已驳回"),       // 审核驳回
    CANCELED("已取消");       // 用户取消认领

    private final String desc; // 状态描述，支持getDesc()调用

    ClaimStatusEnum(String desc) {
        this.desc = desc;
    }

    // 保留原有工具方法（兼容历史代码）
    public static boolean contains(String status) {
        for (ClaimStatusEnum value : values()) {
            if (value.name().equals(status)) {
                return true;
            }
        }
        return false;
    }

    public static String getValidValues() {
        StringBuilder sb = new StringBuilder();
        for (ClaimStatusEnum value : values()) {
            sb.append(value.name()).append(",");
        }
        return sb.length() > 0 ? sb.deleteCharAt(sb.length() - 1).toString() : "";
    }
}