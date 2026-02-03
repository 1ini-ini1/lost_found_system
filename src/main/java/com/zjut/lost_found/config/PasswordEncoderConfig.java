package com.zjut.lost_found.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置（0基础必懂）
 * 作用：提供BCrypt密码加密组件，用于用户密码的安全存储
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt算法：不可逆加密，自动生成盐值，安全性高
        return new BCryptPasswordEncoder();
    }
}
