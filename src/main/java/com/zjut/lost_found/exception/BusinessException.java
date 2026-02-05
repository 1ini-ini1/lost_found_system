package com.zjut.lost_found.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public static BusinessException userNotFound() {
        return new BusinessException(404, "用户不存在");
    }

    public static BusinessException tokenInvalid() {
        return new BusinessException(401, "令牌无效或已过期");
    }

    public static BusinessException userDisabled() {
        return new BusinessException(403, "用户已被禁用");
    }

    public static BusinessException passwordError() {
        return new BusinessException(400, "用户名或密码错误");
    }

    public static BusinessException usernameExists() {
        return new BusinessException(400, "用户名已存在");
    }
}