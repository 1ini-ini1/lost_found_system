package com.zjut.lost_found.controller;

import com.zjut.lost_found.dto.ApiResponse;
import com.zjut.lost_found.dto.UserRegisterDTO;
import com.zjut.lost_found.entity.User;
import com.zjut.lost_found.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口层（0基础必懂）
 * 前端调用入口：用户注册、登录、信息查询
 */
@RestController  // 标识为REST接口，返回JSON数据
@RequestMapping("/api/user")  // 接口统一前缀
@RequiredArgsConstructor
@Validated  // 开启参数校验
public class UserController {

    private final UserService userService;  // 注入用户业务服务

    /**
     * 用户注册接口（POST请求）
     * 访问地址：http://localhost:8080/api/user/register
     */
    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody UserRegisterDTO dto) {
        User savedUser = userService.register(dto);
        return ApiResponse.success("用户注册成功", savedUser);
    }

    /**
     * 用户登录接口（POST请求）
     * 访问地址：http://localhost:8080/api/user/login
     */
    @PostMapping("/login")
    public ApiResponse<User> login(
            @RequestParam String username,  // URL参数：账号
            @RequestParam String password   // URL参数：密码
    ) {
        User loginUser = userService.login(username, password);
        return ApiResponse.success("登录成功", loginUser);
    }

    /**
     * 按ID查询用户信息（GET请求）
     * 访问地址：http://localhost:8080/api/user/{userId}
     */
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ApiResponse.success("查询成功", user);
    }
}