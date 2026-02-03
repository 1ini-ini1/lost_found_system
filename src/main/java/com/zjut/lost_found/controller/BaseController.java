package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 所有控制器的基类（0基础必懂）
 * 作用：封装公共方法，减少重复代码
 */
public class BaseController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户（从Spring Security上下文获取）
     */
    protected User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        // 从认证信息中获取用户名，查询用户详情
        String username = authentication.getName();
        return userService.getUserRepository().findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("当前登录用户不存在"));
    }

    /**
     * 校验当前用户是否为超级管理员
     */
    protected void checkSuperAdmin() {
        User currentUser = getCurrentUser();
        if (!UserRoleEnum.SUPER_ADMIN.equals(currentUser.getRole())) {
            throw new RuntimeException("无超级管理员权限，无法执行该操作");
        }
    }

    /**
     * 校验当前用户是否为管理员（含超级管理员）
     */
    protected void checkAdmin() {
        User currentUser = getCurrentUser();
        if (!UserRoleEnum.ADMIN.equals(currentUser.getRole())
                && !UserRoleEnum.SUPER_ADMIN.equals(currentUser.getRole())) {
            throw new RuntimeException("无管理员权限，无法执行该操作");
        }
    }
}