package com.zjut.lost_found.entity;

import com.zjut.lost_found.enums.UserRoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 用户实体（对应数据库users表）
 * 存储系统用户的核心信息（普通用户、管理员、超级管理员）
 * 关键注解说明：
 * @Entity：标识这是一个JPA实体，对应数据库中的一张表
 * @Table(name = "users")：指定映射的数据库表名（避免和MySQL关键字冲突，用users而非user）
 * @DynamicUpdate：动态更新，仅更新修改过的字段（优化性能，避免更新所有字段）
 * @Data：Lombok注解，自动生成get/set等方法
 * 继承BaseEntity：获得createTime和updateTime字段
 */
@Data
@Entity
@Table(name = "users")
@DynamicUpdate
public class User extends BaseEntity {

    @Id // 主键标识（数据库表的主键字段）
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键自增（适配MySQL，自动生成ID）
    private Long id; // 用户唯一ID（主键，自增，无需手动设置）

    @Column(nullable = false, unique = true) // 映射数据库列：非空、唯一（账号不可重复）
    private String username; // 登录账号（用于登录验证，如zhangsan）

    @Column(nullable = false) // 非空（密码必须存储）
    private String password; // 登录密码（加密存储，不存储明文，后续Service层处理加密）

    @Column(nullable = false) // 非空（真实姓名必填，用于认领验证、通知展示）
    private String name; // 真实姓名（如张三）

    private String phone; // 联系方式（可选，用于联系用户，如13800138000）

    private String email; // 电子邮箱（可选，用于找回密码、发送通知）

    // 用户角色（枚举类型，存储枚举的value值，如USER、ADMIN）
    @Column(nullable = false)
    @Enumerated(EnumType.STRING) // 存储枚举的字符串值（而非索引，避免枚举顺序变化导致错误）
    private UserRoleEnum role = UserRoleEnum.USER; // 默认普通用户（新注册用户都是普通用户）

    // 账号启用状态（true：启用；false：禁用，管理员可禁用账号）
    @Column(nullable = false, name = "is_enabled") // 数据库列名is_enabled（适配MySQL命名规范）
    private Boolean isEnabled = true; // 默认启用（新注册账号默认可登录）
}