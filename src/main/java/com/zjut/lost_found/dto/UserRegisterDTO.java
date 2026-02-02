package com.zjut.lost_found.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求DTO（0基础必懂）
 * 作用：接收前端注册参数，校验格式合法性
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "账号不能为空")  // 非空校验
    @Pattern(regexp = "^[a-zA-Z0-9]{5,20}$", message = "账号仅支持字母和数字，长度5-20字符")
    private String username;  // 登录账号

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$", message = "密码需包含字母和数字，长度8-20字符")
    private String password;  // 登录密码

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "姓名需为2-10位中文")
    private String name;  // 真实姓名

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;  // 联系方式（可选）

    @Email(message = "邮箱格式不正确")
    private String email;  // 电子邮箱（可选）
}