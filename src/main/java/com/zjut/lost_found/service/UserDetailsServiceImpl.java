package com.zjut.lost_found.service;

import com.zjut.lost_found.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security用户详情服务（适配JWT认证过滤器）
 * 核心作用：让JWT过滤器能复用UserService的查询逻辑，确保用户查询结果一致
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("加载用户详情，用户名：{}", username);

            // 核心修改：调用带密码的查询方法
            User user = userService.getUserByUsernameWithPassword(username);

            // 构建Spring Security标准的UserDetails对象
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword()) // 此时密码不为null
                    .authorities(user.getRole().name()) // 角色作为权限标识
                    .accountExpired(false) // 账号未过期
                    .accountLocked(false) // 账号未锁定
                    .credentialsExpired(false) // 凭证未过期
                    .disabled(user.getIsEnabled() != 1) // 1=启用，0=禁用
                    .build();
        } catch (Exception e) {
            log.error("加载用户详情失败 | 用户名：{} | 错误信息：{}", username, e.getMessage());
            throw new UsernameNotFoundException("用户不存在或已禁用：" + username);
        }
    }
}