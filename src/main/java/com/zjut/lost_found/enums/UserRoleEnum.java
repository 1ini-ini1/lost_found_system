package com.zjut.lost_found.enums;

import lombok.Getter;

/**
 * 用户角色枚举（0基础必懂）
 * 区分普通用户、管理员、超级管理员，控制接口访问权限（如管理员能审核物品，普通用户不能）
 * 枚举的核心作用：固定取值，避免写错，提升代码可读性
 */
@Getter // Lombok注解，自动生成getter方法（获取desc和value的值）
public enum UserRoleEnum {
    // 枚举常量：格式 常量名(描述, 存储值)，每个常量用逗号分隔，最后一个用分号
    USER("普通用户", "USER"), // 普通用户：能发布物品、提交认领申请
    ADMIN("管理员", "ADMIN"), // 管理员：能审核物品、审核认领、管理用户
    SUPER_ADMIN("超级管理员", "SUPER_ADMIN"); // 超级管理员：拥有所有权限（可选）

    private final String desc; // 角色描述（用于前端展示，如给用户看“普通用户”）
    private final String value; // 角色值（用于数据库存储，如存“USER”，不存中文）

    // 构造器：枚举常量初始化（枚举的构造器默认是private，无需手动写）
    UserRoleEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    // 静态方法：根据value获取枚举（用于数据库查询后转换，如查出来“USER”，转换成USER枚举）
    public static UserRoleEnum getByValue(String value) {
        // 遍历所有枚举常量，匹配value
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.getValue().equals(value)) {
                return role;
            }
        }
        return USER; // 默认返回普通用户（防止查询不到时报错）
    }

    // ========== 补充：适配UserService中的getCode/getName调用 ==========
    public String getCode() {
        return this.value; // 代码/存储值=原value
    }

    public String getName() {
        return this.desc; // 名称/描述=原desc
    }
}