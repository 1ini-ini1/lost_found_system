package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 用户角色枚举（0基础必懂）
 * 作用：规范用户权限，不同角色对应不同操作权限
 */
@Getter
public enum UserRoleEnum {
    STUDENT_TEACHER("STUDENT_TEACHER", "学生/老师"),  // 普通用户（发布/认领物品）
    ADMIN("ADMIN", "失物招领管理员"),                // 审核物品/认领申请
    SUPER_ADMIN("SUPER_ADMIN", "系统管理员");         // 系统配置/用户管理

    private final String code;  // 角色编码（数据库存储）
    private final String desc;  // 角色描述（前端展示）

    UserRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}