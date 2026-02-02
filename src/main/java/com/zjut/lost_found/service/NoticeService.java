package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.Notice;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.repository.NoticeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息通知服务（0基础必懂）
 * 核心功能：发送系统通知（审核结果、认领提醒等）
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;  // 注入消息数据访问层

    /**
     * 发送通知（通用方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public Notice sendNotice(User receiver, String title, String content) {
        // 校验接收人信息完整
        if (receiver.getId() == null || receiver.getUsername() == null) {
            throw new RuntimeException("接收人信息不完整，无法发送通知");
        }

        // 构建通知实体
        Notice notice = new Notice();
        notice.setReceiveUser(receiver);  // 关联接收人
        notice.setNoticeTitle(title);     // 通知标题
        notice.setNoticeContent(content); // 通知内容
        notice.setIsRead(false);          // 默认未读

        // 保存通知
        return noticeRepository.save(notice);
    }

    /**
     * 标记通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public Notice markAsRead(Long noticeId, User operator) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在，ID：" + noticeId));

        // 校验权限（仅本人可标记自己的通知）
        if (!operator.getId().equals(notice.getReceiveUser().getId())) {
            throw new RuntimeException("无权限操作他人的通知");
        }

        notice.setIsRead(true);
        return noticeRepository.save(notice);
    }
}