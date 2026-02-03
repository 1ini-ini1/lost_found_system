package com.zjut.lost_found.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 通知实体（对应数据库notices表）
 * 存储系统发送给用户的各类通知
 */
@Data
@Entity
@Table(name = "notices")
@DynamicUpdate
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 通知唯一ID

    @Column(name = "notice_title", nullable = false)
    private String noticeTitle;  // 通知标题

    @Column(name = "notice_content", columnDefinition = "TEXT", nullable = false)
    private String noticeContent;  // 通知内容

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;  // 是否已读（默认未读）

    // 多对一关联：多个通知属于一个接收用户
    @ManyToOne
    @JoinColumn(name = "receive_user_id", nullable = false)
    private User receiveUser;  // 通知接收人
}