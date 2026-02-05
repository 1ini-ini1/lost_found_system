package com.zjut.lost_found.controller;

import com.zjut.lost_found.exception.BusinessException;
import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.dto.LoginRequestDTO;
import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器（0基础必懂）
 * 暴露用户注册、登录、角色修改等接口
 * 优化点：
 * 1. 登录接口改为JSON请求体接收参数（解决Swagger无法输入JSON问题）
 * 2. 新增登录请求DTO，规范参数校验
 * 3. 增强注释和Swagger文档
 * 4. 统一返回格式，隐藏敏感信息
 * 5. 治本优化：登录/注册时校验用户存在性，从源头避免无效令牌
 * 6. 抛出自定义业务异常，替代模糊的RuntimeException
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理接口", description = "普通用户/管理员的用户相关操作")
@Slf4j // 新增：添加日志注解
public class UserController extends BaseController {

    private final UserService userService;

    /**
     * 用户注册接口（公开）
     * 优化：注册前校验用户名是否存在，避免重复注册，抛出自定义异常
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "普通用户注册，账号需唯一，密码需符合规则（长度≥6，包含字母+数字）")
    public ApiResponse<User> register(
            @Valid @RequestBody UserRegisterDTO dto
    ) {
        // 核心优化1：注册前校验用户名是否已存在
        if (userService.existsByUsername(dto.getUsername())) {
            log.warn("注册失败：用户名[{}]已存在", dto.getUsername());
            throw BusinessException.usernameExists(); // 抛出自定义异常
        }

        User user = userService.register(dto);
        user.setPassword(null); // 隐藏密码，避免敏感信息泄露
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 用户登录接口（公开）
     * 优化：改为JSON请求体接收参数，符合RESTful规范，提升安全性
     * 治本优化：登录前校验用户存在性，避免生成无效令牌
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回JWT令牌，用于后续接口授权（令牌有效期2小时）")
    public ApiResponse<String> login(
            @Valid @RequestBody LoginRequestDTO loginRequest // 核心修改：JSON请求体接收参数
    ) {
        // 核心优化2：登录前校验用户是否存在（从源头避免无效令牌）
        if (!userService.existsByUsername(loginRequest.getUsername())) {
            log.warn("登录失败：用户名[{}]不存在", loginRequest.getUsername());
            // 统一提示为"用户名或密码错误"，避免信息泄露
            throw BusinessException.passwordError();
        }

        // 调用服务层登录逻辑（用户名+密码校验）
        String jwtToken = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ApiResponse.success("登录成功", jwtToken);
    }

    /**
     * 查询当前登录用户信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户信息", description = "需登录后调用，返回用户基本信息（隐藏密码/盐值等敏感字段）")
    public ApiResponse<User> getCurrentUserInfo() {
        User currentUser = getCurrentUser();
        // 隐藏所有敏感信息
        currentUser.setPassword(null);
        return ApiResponse.success("查询成功", currentUser);
    }

    /**
     * 修改用户角色（超级管理员操作）
     */
    @PutMapping("/{userId}/role")
    @Operation(summary = "修改用户角色", description = "仅超级管理员可调用，支持修改为USER/ADMIN/SUPER_ADMIN")
    public ApiResponse<User> updateUserRole(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "新角色类型", required = true) @RequestParam UserRoleEnum newRole
    ) {
        checkSuperAdmin(); // 校验超级管理员权限
        User operator = getCurrentUser();
        User updatedUser = userService.updateUserRole(userId, newRole, operator);
        updatedUser.setPassword(null); // 隐藏密码
        return ApiResponse.success("角色修改成功", updatedUser);
    }
}