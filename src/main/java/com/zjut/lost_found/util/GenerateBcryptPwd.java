package com.zjut.lost_found.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 临时工具类：生成BCrypt加密密码（解决密码不匹配问题）
 * 运行此类，复制输出的加密值到数据库更新user1/admin的密码
 */
public class GenerateBcryptPwd {
    public static void main(String[] args) {
        // 初始化BCrypt编码器（和项目中使用的一致）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456"; // 原始密码
        String encodedPassword = encoder.encode(rawPassword); // 加密

        // 打印结果（复制encodedPassword到数据库）
        System.out.println("原始密码：" + rawPassword);
        System.out.println("BCrypt加密值：" + encodedPassword);
        // 示例输出：$2a$10$e8V9s8Z7d6F5g4H3j2K1L0M9N8B7V6C5X4Z3A2S1D0F9G8H7J6K5L4M3N2B1V0
    }
}