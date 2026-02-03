package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.enums.UserRoleEnum;
import com.zjut.lost_found.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器（0基础必懂）
 * 暴露用户注册、登录、角色修改等接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理接口", description = "普通用户/管理员的用户相关操作")
public class UserController extends BaseController {

    private final UserService userService;

    /**
     * 用户注册接口（公开）
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "普通用户注册，账号需唯一，密码需符合规则")
    public ApiResponse<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        User user = userService.register(dto);
        return ApiResponse.success("注册成功", user);
    }

    /**
     * 用户登录接口（公开，由Spring Security接管，此处仅作接口说明）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "登录成功返回令牌，用于后续接口授权")
    public ApiResponse<String> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.login(username, password);
        // 实际开发中需生成JWT令牌返回，此处简化处理
        return ApiResponse.success("登录成功", "JWT_TOKEN_" + user.getId());
    }

    /**
     * 查询当前登录用户信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前登录用户信息", description = "需登录后调用，返回用户基本信息（隐藏密码）")
    public ApiResponse<User> getCurrentUserInfo() {
        User currentUser = getCurrentUser();
        // 隐藏敏感信息（密码置空）
        currentUser.setPassword(null);
        return ApiResponse.success("查询成功", currentUser);
    }

    /**
     * 修改用户角色（超级管理员操作）
     */
    @PutMapping("/{userId}/role")
    @Operation(summary = "修改用户角色", description = "仅超级管理员可调用，修改用户的角色类型")
    public ApiResponse<User> updateUserRole(
            @PathVariable Long userId,
            @RequestParam UserRoleEnum newRole
    ) {
        checkSuperAdmin(); // 校验超级管理员权限
        User operator = getCurrentUser();
        User updatedUser = userService.updateUserRole(userId, newRole, operator);
        updatedUser.setPassword(null); // 隐藏密码
        return ApiResponse.success("角色修改成功", updatedUser);
    }
}
