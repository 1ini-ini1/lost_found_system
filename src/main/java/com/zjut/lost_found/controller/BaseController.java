package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 所有控制器的基类（优化版）
 * 核心优化：多层校验+日志排查+匿名用户识别，解决「当前登录用户不存在」问题
 */
@Slf4j // 新增日志注解，便于排查问题
public class BaseController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户（优化版：全链路校验+日志排查）
     */
    protected User getCurrentUser() {
        // 1. 获取SecurityContext中的认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("从SecurityContext获取的认证信息：{}", authentication == null ? "空" : authentication);

        // 2. 第一层校验：认证信息是否为空
        if (authentication == null) {
            log.error("获取当前用户失败：SecurityContext中无认证信息");
            throw new RuntimeException("用户未登录");
        }

        // 3. 第二层校验：是否为匿名用户（关键修复点）
        if ("anonymousUser".equals(authentication.getName())) {
            log.error("获取当前用户失败：当前为匿名用户，未携带有效令牌");
            throw new RuntimeException("用户未登录");
        }

        // 4. 第三层校验：认证是否有效
        if (!authentication.isAuthenticated()) {
            log.error("获取当前用户失败：认证信息无效，未通过验证");
            throw new RuntimeException("用户未登录");
        }

        // 5. 提取用户名（兼容不同Principal类型）
        String username;
        if (authentication.getPrincipal() instanceof UserDetails) {
            // 正常认证：Principal是UserDetails对象
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            // 特殊情况：Principal是用户名字符串
            username = (String) authentication.getPrincipal();
        } else {
            username = null;
        }
        log.info("从认证信息中提取的用户名：{}", username == null ? "空" : username);

        // 6. 第四层校验：用户名是否为空
        if (username == null || username.isEmpty()) {
            log.error("获取当前用户失败：从认证信息中提取的用户名为空");
            throw new EntityNotFoundException("当前登录用户不存在");
        }

        // 7. 查询用户并返回（带日志）
        User currentUser = userService.getUserRepository().findByUsername(username)
                .orElseThrow(() -> {
                    log.error("获取当前用户失败：用户名[{}]在数据库中不存在", username);
                    return new EntityNotFoundException("当前登录用户不存在");
                });
        log.info("成功获取当前登录用户：{}（ID：{}，角色：{}）",
                currentUser.getUsername(), currentUser.getId(), currentUser.getRole());

        return currentUser;
    }

    /**
     * 校验当前用户是否为超级管理员（优化版：带日志）
     */
    protected void checkSuperAdmin() {
        User currentUser = getCurrentUser();
        if (!UserRoleEnum.SUPER_ADMIN.equals(currentUser.getRole())) {
            log.error("权限校验失败：用户[{}]不是超级管理员（当前角色：{}）",
                    currentUser.getUsername(), currentUser.getRole());
            throw new RuntimeException("无超级管理员权限，无法执行该操作");
        }
        log.info("权限校验通过：用户[{}]是超级管理员", currentUser.getUsername());
    }

    /**
     * 校验当前用户是否为管理员（含超级管理员）（优化版：带日志）
     */
    protected void checkAdmin() {
        User currentUser = getCurrentUser();
        boolean isAdmin = UserRoleEnum.ADMIN.equals(currentUser.getRole())
                || UserRoleEnum.SUPER_ADMIN.equals(currentUser.getRole());

        if (!isAdmin) {
            log.error("权限校验失败：用户[{}]不是管理员（当前角色：{}）",
                    currentUser.getUsername(), currentUser.getRole());
            throw new RuntimeException("无管理员权限，无法执行该操作");
        }
        log.info("权限校验通过：用户[{}]是管理员（角色：{}）",
                currentUser.getUsername(), currentUser.getRole());
    }
}