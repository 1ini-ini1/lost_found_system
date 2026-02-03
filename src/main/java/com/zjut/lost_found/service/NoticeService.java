package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.Notice;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.repository.NoticeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知核心业务服务（0基础必懂）
 * 核心功能：发送通知、标记已读、查询用户通知
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;  // 注入通知数据访问层
    private final UserService userService;            // 注入用户服务

    /**
     * 发送系统通知（通用方法）
     */
    @Transactional(rollbackFor = Exception.class)
    public Notice sendNotice(User receiveUser, String title, String content) {
        // 1. 校验接收人账号启用
        if (!receiveUser.getIsEnabled()) {
            throw new RuntimeException("接收人账号已禁用，无法发送通知");
        }

        // 2. 构建通知实体
        Notice notice = new Notice();
        notice.setNoticeTitle(title);
        notice.setNoticeContent(content);
        notice.setReceiveUser(receiveUser);
        // 默认为未读，时间自动填充（继承BaseEntity）

        // 3. 保存通知到数据库
        return noticeRepository.save(notice);
    }

    /**
     * 标记通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public Notice markAsRead(Long noticeId, User operator) {
        // 1. 校验通知存在
        Notice notice = getNoticeById(noticeId);

        // 2. 校验操作权限（仅通知接收人可操作）
        if (!notice.getReceiveUser().getId().equals(operator.getId())) {
            throw new RuntimeException("无权限操作该通知");
        }

        // 3. 标记已读并保存
        notice.setIsRead(true);
        return noticeRepository.save(notice);
    }

    /**
     * 批量标记通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchMarkAsRead(List<Long> noticeIds, User operator) {
        List<Notice> notices = noticeRepository.findAllById(noticeIds);
        // 校验所有通知的接收人均为当前用户
        for (Notice notice : notices) {
            if (!notice.getReceiveUser().getId().equals(operator.getId())) {
                throw new RuntimeException("存在无权操作的通知，批量标记失败");
            }
            notice.setIsRead(true);
        }
        noticeRepository.saveAll(notices);
    }

    /**
     * 查询用户的所有通知（按时间倒序）
     */
    public List<Notice> getUserNotices(User user) {
        return noticeRepository.findByReceiveUserOrderByCreateTimeDesc(user);
    }

    /**
     * 查询用户的未读通知
     */
    public List<Notice> getUserUnreadNotices(User user) {
        return noticeRepository.findByReceiveUserAndIsRead(user, false);
    }

    /**
     * 按ID查询通知详情
     */
    public Notice getNoticeById(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("通知不存在，ID：" + noticeId));
    }
}