package com.zjut.lost_found.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 通知实体（对应数据库notices表）
 * 存储系统发送给用户的各类通知，关联接收人（User）
 */
@Data
@Entity
@Table(name = "notices")
@DynamicUpdate
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 通知唯一ID（主键，自增）

    @Column(name = "notice_title", nullable = false)
    private String noticeTitle;  // 通知标题（如"物品审核结果通知"，非空）

    @Column(name = "notice_content", columnDefinition = "TEXT", nullable = false)
    private String noticeContent;  // 通知内容（如"您发布的苹果手机14已审核通过"，非空）

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;  // 是否已读（默认未读，用户查看后改为true）

    // 多对一关联：多个通知属于一个接收用户（一个用户可以收到多个通知）
    @ManyToOne
    @JoinColumn(name = "receive_user_id", nullable = false)
    private User receiveUser;  // 通知接收人
}