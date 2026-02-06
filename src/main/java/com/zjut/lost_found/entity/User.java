package com.zjut.lost_found.entity;


import com.zjut.lost_found.enums.UserRoleEnum; // 关键：补充枚举正确导入
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // 解决equals/hashCode警告所需
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 用户实体（对应数据库users表）
 * 存储系统用户的核心信息（普通用户、管理员、超级管理员）
 * 继承BaseEntity：获得createTime和updateTime字段
 */
@Data
@EqualsAndHashCode(callSuper = true) // 核心：消除Lombok继承父类的警告
@Entity
@Table(name = "users")
@DynamicUpdate
public class User extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRoleEnum role = UserRoleEnum.USER;

    @Column(nullable = false, name = "is_enabled")
    private Integer isEnabled = 1;
}