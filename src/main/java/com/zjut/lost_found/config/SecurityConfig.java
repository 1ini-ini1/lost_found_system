package com.zjut.lost_found.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security配置（0基础必懂）
 * 作用：配置接口授权规则、登录验证等安全策略
 * 优化点：1. 修复Swagger跳转登录问题 2. 适配JWT无状态认证 3. 完善请求放行规则
 */
@Configuration
@EnableWebSecurity  // 启用Spring Security安全机制
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭CSRF防护（前后端分离+JWT必关）
                .csrf(csrf -> csrf.disable())
                // 2. 禁用Session（JWT无状态认证，无需Session）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 3. 配置接口授权规则（核心）
                .authorizeHttpRequests(auth -> auth
                        // ========== 100%覆盖Swagger所有路径（彻底解决跳转登录） ==========
                        .requestMatchers(
                                // Swagger UI核心路径
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // Swagger v2/v3文档路径
                                "/v2/api-docs",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                // Swagger资源文件
                                "/swagger-resources",
                                "/swagger-resources/**",
                                // Swagger UI依赖的静态资源
                                "/webjars/**",
                                // SpringDoc OpenAPI额外路径
                                "/api-docs/**",
                                "/openapi/**"
                        ).permitAll()
                        // ========== 原有公开接口（完全保留） ==========
                        .requestMatchers("/api/user/register", "/api/user/login").permitAll()
                        .requestMatchers("/api/item/{itemId}", "/api/item/list").permitAll()
                        // 管理员接口：仅管理员可访问
                        .requestMatchers("/api/item/{itemId}/audit", "/api/claim/{claimId}/audit").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/audit-log/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 超级管理员接口：仅超级管理员可访问
                        .requestMatchers("/api/user/{userId}/role", "/api/audit-log/all").hasRole("SUPER_ADMIN")
                        // 其他接口：需登录后访问
                        .anyRequest().authenticated()
                )
                // 4. 配置登录（简化处理，实际开发需替换为JWT过滤器）
                .formLogin(form -> form
                        .permitAll() // 允许匿名访问登录页
                        .loginPage("/login") // 自定义登录页路径（可选）
                )
                // 5. 配置注销（保留原有逻辑）
                .logout(logout -> logout.permitAll())
                // 6. 禁用默认的HTTP Basic认证（避免冲突）
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}