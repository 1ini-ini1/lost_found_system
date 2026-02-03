package com.zjut.lost_found.config; // 注意：此类应放在config包下，而非entity包（修正之前的包路径错误）

import com.zjut.lost_found.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器（补充，安全核心）
 * 作用：拦截所有请求，验证JWT令牌，通过则自动完成认证（无需重复登录）
 * OncePerRequestFilter：确保每个请求只被过滤一次（避免重复拦截，提升性能）
 */
@Component // 标识为Spring组件，交给Spring管理
@Slf4j // 打印日志
@RequiredArgsConstructor // Lombok注解，自动生成构造器，注入依赖（无需手动写@Autowired）
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 注入JWT工具类（用于解析、验证令牌）
    private final JwtUtil jwtUtil;
    // 注入UserDetailsService（用于根据用户名查询用户详情，Spring Security提供的接口）
    private final UserDetailsService userDetailsService;

    /**
     * 核心过滤逻辑（每个请求都会执行此方法）
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 前端发送的请求
            HttpServletResponse response, // 后端返回的响应
            FilterChain filterChain // 过滤器链，用于继续执行后续过滤器
    ) throws ServletException, IOException {
        try {
            // 1. 从请求头获取JWT令牌（调用JwtUtil的方法，获取Authorization头中的令牌）
            String token = jwtUtil.getTokenFromRequest(request);

            // 2. 验证令牌是否有效，且当前未完成认证（SecurityContext中无认证信息）
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 3. 从令牌中解析用户名
                String username = jwtUtil.getUsernameFromToken(token);
                if (username != null) {
                    // 4. 根据用户名查询用户详情（从数据库获取用户信息，用于验证）
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 5. 验证令牌与用户是否匹配（令牌未篡改、未过期，且对应此用户）
                    if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                        // 6. 构建认证令牌，存入Security上下文（完成自动认证）
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities() // 权限信息
                        );
                        // 设置请求详情（如IP地址）
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        // 存入SecurityContext，后续接口会认为用户已登录
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT认证通过，用户名：{}", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT认证失败：{}", e.getMessage());
        }

        // 继续执行过滤器链（无论认证是否成功，都放行请求，后续由Security判断是否允许访问）
        filterChain.doFilter(request, response);
    }
}