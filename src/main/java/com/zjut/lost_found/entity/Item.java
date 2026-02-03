package com.zjut.lost_found.entity;

import com.zjut.lost_found.enums.ItemStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 物品实体（对应数据库items表）
 * 存储失物/招领物品的核心信息，关联发布人（User）
 */
@Data
@Entity
@Table(name = "items")
@DynamicUpdate
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 物品唯一ID（主键，自增）

    @Column(nullable = false)
    private String name;  // 物品名称（如"苹果手机14"，非空）

    @Column(nullable = false)
    private String type;  // 物品类型（如"电子设备""证件""生活用品"，非空）

    @Column(nullable = false)
    private LocalDateTime time;  // 丢失/拾取时间（非空，用户填写）

    @Column(columnDefinition = "TEXT")  // 长文本类型（支持大量描述，如500字符）
    private String description;  // 物品特征描述（如"黑色，背面有划痕，手机壳是卡通图案"）

    private String photoUrl;  // 物品图片URL（可选，用户可上传图片，存储图片链接）

    private Boolean isReward = false;  // 是否悬赏（默认不悬赏，true=悬赏，false=不悬赏）

    private String rewardDesc;  // 悬赏说明（可选，如"归还者奖励200元"）

    // 物品状态（枚举类型，存储value值，如PENDING_AUDIT、PASSED）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatusEnum status = ItemStatusEnum.PENDING_AUDIT;  // 默认待审核

    // 多对一关联：多个物品属于一个发布人（一个用户可以发布多个物品）
    @ManyToOne // JPA多对一注解，关联User实体
    @JoinColumn(name = "publisher_id", nullable = false)  // 外键：发布人ID，关联users表的id字段
    private User publisher;  // 发布人（关联User实体，存储发布人的所有信息）
}