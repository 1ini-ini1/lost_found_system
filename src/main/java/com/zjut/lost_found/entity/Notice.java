package com.zjut.lost_found.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * 通知实体（对应数据库notices表）
 * 存储系统给用户的各类通知（审核结果/认领提醒/系统公告），关联User实体为接收人
 * 多对一关联：一个用户可接收多个通知，一个通知仅属于一个用户
 */
@Data
@EqualsAndHashCode(callSuper = true) // 解决Lombok的equals/hashCode警告
@Entity
@Table(name = "notices", indexes = {
        @Index(name = "idx_notice_receive_user", columnList = "receive_user_id"),
        @Index(name = "idx_notice_is_read", columnList = "is_read")
})
@DynamicUpdate // 动态更新：仅修改已读状态等变更字段
public class Notice extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 通知唯一ID（主键）
     * 自增策略：适配MySQL AUTO_INCREMENT，数据库自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 通知标题（非空）
     * 简短描述通知类型，如「物品审核结果通知」「失物认领提醒」
     */
    @Column(name = "notice_title", nullable = false, length = 100)
    private String noticeTitle;

    /**
     * 通知内容（非空）
     * 详细通知信息，使用TEXT类型支持长文本，如「您发布的XX物品已审核通过」
     */
    @Column(name = "notice_content", nullable = false, columnDefinition = "TEXT")
    private String noticeContent;

    /**
     * 是否已读状态（非空）
     * 默认值：false（未读），用户查看后由Service层改为true（已读）
     * 数据库列名：is_read（适配MySQL下划线命名规范）
     */
    @Column(name = "is_read", nullable = false, columnDefinition = "tinyint(1) default 0")
    private Boolean isRead = false;

    /**
     * 通知接收人（非空，多对一关联User）
     * 关联字段：receive_user_id（数据库外键，关联users表的id）
     * 抓取策略：EAGER（查询通知时立即加载用户信息，满足展示需求）
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "receive_user_id", // 数据库外键列名
            nullable = false,         // 非空：通知必须有接收人
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private User receiveUser;
}