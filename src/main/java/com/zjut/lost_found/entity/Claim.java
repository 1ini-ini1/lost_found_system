package com.zjut.lost_found.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 认领申请实体（对应数据库claims表）
 * 存储用户对物品的认领申请信息，关联物品（Item）和认领人（User）
 */
@Data
@Entity
@Table(name = "claims")
@DynamicUpdate
public class Claim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 申请唯一ID（主键，自增）

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proof;  // 认领证明（核心审核依据，如购买凭证、物品细节描述，非空）

    @Column(nullable = false)
    private String status = "PENDING_AUDIT";  // 申请状态：待审核/已通过/已驳回（字符串存储，简化逻辑）

    private String rejectReason;  // 驳回理由（仅状态为驳回时有效，管理员填写）

    // 多对一关联：多个申请对应一个物品（一个物品可以被多个用户申请认领）
    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;  // 被认领物品

    // 多对一关联：多个申请对应一个认领人（一个用户可以提交多个认领申请）
    @ManyToOne
    @JoinColumn(name = "claimer_id", nullable = false)
    private User claimer;  // 认领人（提交申请的用户）
}