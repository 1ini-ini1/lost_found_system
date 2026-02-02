
package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 用户角色枚举（0基础必懂）
 * 作用：规范用户权限，不同角色对应不同操作权限，避免权限混乱
 * 核心设计：每个角色对应唯一编码，便于权限校验（如仅超级管理员可修改用户角色）
 */
@Getter
public enum UserRoleEnum {
    STUDENT_TEACHER("STUDENT_TEACHER", "学生/老师"),  // 普通用户：可发布物品、提交认领申请
    ADMIN("ADMIN", "失物招领管理员"),                // 审核管理员：可审核物品和认领申请
    SUPER_ADMIN("SUPER_ADMIN", "系统管理员");         // 超级管理员：可管理用户角色、系统配置

    private final String code;  // 角色编码（数据库存储）：用于权限判断
    private final String desc;  // 角色描述（前端展示）：如“学生/老师”

    // 构造方法：初始化角色编码和描述
    UserRoleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}