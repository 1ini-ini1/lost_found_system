package com.zjut.lost_found.entity;

import com.zjut.lost_found.enums.ItemStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 物品实体（对应数据库items表）
 * 存储失物/招领物品的核心信息，关联发布人（User）
 * 优化点：补充驳回理由字段、完善字段注释、规范默认值
 */
@Data
@Entity
@Table(name = "items")
@DynamicUpdate // 仅更新修改过的字段
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 物品唯一ID（主键，自增）

    @Column(nullable = false, length = 100) // 限制名称长度，避免过长
    private String name;  // 物品名称（如"苹果手机14"，非空）

    @Column(nullable = false, length = 50) // 限制类型长度
    private String type;  // 物品类型（如"电子设备""证件""生活用品"，非空）

    @Column(nullable = false)
    private LocalDateTime time;  // 丢失/拾取时间（非空，用户填写）

    @Column(columnDefinition = "TEXT")  // 长文本类型（支持大量描述，如500字符）
    private String description;  // 物品特征描述（如"黑色，背面有划痕，手机壳是卡通图案"）

    @Column(length = 255) // 限制URL长度
    private String photoUrl;  // 物品图片URL（可选，用户可上传图片，存储图片链接）

    @Column(nullable = false)
    private Boolean isReward = false;  // 是否悬赏（默认不悬赏，true=悬赏，false=不悬赏）

    @Column(length = 200) // 限制悬赏说明长度
    private String rewardDesc;  // 悬赏说明（可选，如"归还者奖励200元"）

    // 物品状态（枚举类型，存储字符串值，如PENDING_AUDIT、PASSED）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // 限制枚举字段长度
    private ItemStatusEnum status = ItemStatusEnum.PENDING_AUDIT;  // 默认待审核

    // ========== 新增：驳回理由字段（解决setRejectReason报错） ==========
    @Column(length = 500) // 限制驳回理由长度，避免过长
    private String rejectReason;  // 审核驳回理由（仅状态为REJECTED时有值）

    // 多对一关联：多个物品属于一个发布人（一个用户可以发布多个物品）
    @ManyToOne(fetch = FetchType.EAGER) // 懒加载，避免查询物品时冗余加载用户信息
    @JoinColumn(name = "publisher_id", nullable = false)  // 外键：发布人ID，关联users表的id字段
    private User publisher;  // 发布人（关联User实体，存储发布人的所有信息）

    // ========== 补充BaseEntity未覆盖的时间字段（可选，若BaseEntity已包含则无需重复） ==========
    // 若BaseEntity未定义createTime/updateTime，需添加以下字段：
    /*
    @Column(nullable = false, updatable = false) // 创建时间不可更新
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now();
    */
}