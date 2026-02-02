package com.zjut.lost_found.entity;

import com.zjut.lost_found.enums.ItemStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 物品实体（对应数据库items表）
 * 存储失物/招领物品的核心信息
 */
@Data
@Entity
@Table(name = "items")
@DynamicUpdate
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 物品唯一ID

    @Column(nullable = false)
    private String name;  // 物品名称（如"苹果手机14"）

    @Column(nullable = false)
    private String type;  // 物品类型（如"电子设备""证件"）

    @Column(nullable = false)
    private LocalDateTime time;  // 丢失/拾取时间

    @Column(columnDefinition = "TEXT")  // 长文本类型（支持大量描述）
    private String description;  // 物品特征描述（如"黑色，背面有划痕"）

    private String photoUrl;  // 物品图片URL（可选）

    private Boolean isReward = false;  // 是否悬赏（默认不悬赏）

    private String rewardDesc;  // 悬赏说明（可选）

    // 物品状态（枚举类型）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatusEnum status = ItemStatusEnum.PENDING_AUDIT;  // 默认待审核

    // 多对一关联：多个物品属于一个发布人（关联users表）
    @ManyToOne
    @JoinColumn(name = "publisher_id", nullable = false)  // 外键：发布人ID
    private User publisher;  // 发布人
}