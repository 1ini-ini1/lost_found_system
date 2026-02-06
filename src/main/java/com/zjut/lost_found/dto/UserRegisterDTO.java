package com.zjut.lost_found.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求DTO
 * 作用：接收前端注册表单参数，通过JSR-380注解做参数合法性校验，校验失败直接返回前端
 * 注：仅保留注册必要字段，与User实体解耦，避免实体字段变更影响前端请求
 */
@Data
public class UserRegisterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 登录账号（非空）
     * 校验规则：仅字母/数字，长度5-20字符，前端+后端双重校验
     */
    @NotBlank(message = "登录账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,20}$", message = "登录账号仅支持字母和数字，长度5-20个字符")
    private String username;

    /**
     * 登录密码（非空）
     * 校验规则：必须包含字母+数字，长度8-20字符，保证密码复杂度
     */
    @NotBlank(message = "登录密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$", message = "密码必须包含字母和数字，长度8-20个字符")
    private String password;

    /**
     * 真实姓名（非空）
     * 校验规则：仅2-10位中文，适配国内姓名规范，避免特殊字符
     */
    @NotBlank(message = "真实姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "真实姓名仅支持2-10位中文")
    private String name;

    /**
     * 手机号码（可选）
     * 校验规则：11位有效手机号（13/14/15/16/17/18/19开头），非空时才校验格式
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号码格式不正确（请输入11位有效手机号）")
    private String phone;

    /**
     * 电子邮箱（可选）
     * 校验规则：符合RFC标准邮箱格式，非空时才校验格式
     * 注：@Email自带非空校验，配合@Pattern空值正则实现「可选但非空则校验」
     */
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "电子邮箱格式不正确")
    private String email;
}