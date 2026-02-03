package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.Notice;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知接口控制器（0基础必懂）
 * 暴露通知查询、标记已读等接口
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
@Tag(name = "通知管理接口", description = "通知查询、标记已读等操作")
public class NoticeController extends BaseController {

    private final NoticeService noticeService;

    /**
     * 查询我的所有通知
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的通知", description = "登录后可查询自己的所有通知（按时间倒序）")
    public ApiResponse<List<Notice>> getMyNotices() {
        User currentUser = getCurrentUser();
        List<Notice> notices = noticeService.getUserNotices(currentUser);
        return ApiResponse.success("查询成功", notices);
    }

    /**
     * 查询我的未读通知
     */
    @GetMapping("/my/unread")
    @Operation(summary = "查询未读通知", description = "登录后可查询自己的所有未读通知")
    public ApiResponse<List<Notice>> getMyUnreadNotices() {
        User currentUser = getCurrentUser();
        List<Notice> notices = noticeService.getUserUnreadNotices(currentUser);
        return ApiResponse.success("查询成功", notices);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{noticeId}/read")
    @Operation(summary = "标记通知已读", description = "登录后可标记自己的通知为已读")
    public ApiResponse<Notice> markAsRead(@PathVariable Long noticeId) {
        User currentUser = getCurrentUser();
        Notice notice = noticeService.markAsRead(noticeId, currentUser);
        return ApiResponse.success("标记成功", notice);
    }

    /**
     * 批量标记通知为已读
     */
    @PutMapping("/batch-read")
    @Operation(summary = "批量标记已读", description = "登录后可批量标记自己的通知为已读")
    public ApiResponse<Void> batchMarkAsRead(@RequestParam List<Long> noticeIds) {
        User currentUser = getCurrentUser();
        noticeService.batchMarkAsRead(noticeIds, currentUser);
        return ApiResponse.success("批量标记已读成功");
    }
}
