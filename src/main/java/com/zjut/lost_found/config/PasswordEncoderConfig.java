package com.zjut.lost_found.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置类
 * 核心：配置BCrypt密码编码器，供整个项目的密码加密/校验使用
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 配置BCrypt密码编码器Bean
     * BCrypt：不可逆加密算法，适合密码存储（Spring Security推荐）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}