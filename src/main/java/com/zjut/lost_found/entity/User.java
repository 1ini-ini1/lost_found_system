package com.zjut.lost_found.entity;

import com.zjut.lost_found.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 用户实体（对应数据库users表）
 * 存储用户账号、密码、角色等信息
 */
@Data
@Entity  // JPA注解：标识为实体类（映射数据库表）
@Table(name = "users")  // 指定映射的表名
@DynamicUpdate  // 仅更新修改过的字段
public class User extends BaseEntity {  // 继承基类，获取时间字段

    @Id  // 标识为主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 主键自增（依赖MySQL）
    private Long id;  // 用户唯一ID

    @Column(unique = true, nullable = false)  // 字段约束：唯一且非空
    private String username;  // 登录账号（如学号/工号）

    @Column(nullable = false)  // 字段约束：非空
    private String password;  // 登录密码（加密存储）

    @Column(nullable = false)
    private String name;  // 用户真实姓名

    private String phone;  // 联系方式（可选）

    private String email;  // 电子邮箱（可选）

    private Boolean isEnabled = true;  // 账号是否启用（默认启用）

    // 用户角色（枚举类型，数据库存储字符串）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoleEnum role = UserRoleEnum.STUDENT_TEACHER;  // 默认普通用户
}