package com.zjut.lost_found.config;

import com.zjut.lost_found.exception.BusinessException;
import com.zjut.lost_found.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器（最终修复版）
 * 核心修复：
 * 1. 确保用户存在时认证信息正确存入SecurityContext
 * 2. 新增认证生效日志，精准定位认证状态
 * 3. 移除response.getWriter()写入，避免响应流重复使用导致500错误
 * 4. 抛出自定义业务异常，替代仅设置状态码，异常体系更规范
 * 5. 新增用户启用状态校验，避免禁用用户登录
 * 6. 新增SecurityContext调试日志，确认认证信息存入成功
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        log.info("开始拦截请求：{}", requestPath);

        try {
            String token = jwtUtil.getTokenFromRequest(request);
            log.info("从请求头提取的令牌：{}", token == null ? "空" : token);

            // ===== 核心修复：检查现有认证是否有效 =====
            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null) {
                // 如果是匿名认证/无效认证，清除并重新校验
                if (existingAuth instanceof AnonymousAuthenticationToken
                        || existingAuth.getPrincipal() == null
                        || "anonymousUser".equals(existingAuth.getPrincipal())) {
                    SecurityContextHolder.getContext().setAuthentication(null);
                    log.warn("清除无效的匿名认证信息，重新校验JWT令牌");
                } else {
                    log.info("当前有效认证用户：{}", existingAuth.getName());
                    // 提前放行，避免重复校验
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 重新检查认证状态，确保令牌校验逻辑执行
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("令牌非空且未认证，开始解析令牌");
                String username = jwtUtil.getUsernameFromToken(token);
                log.info("从令牌解析出的用户名：{}", username == null ? "空" : username);

                if (username != null) {
                    try {
                        // 1. 查询用户详情（核心：验证用户是否存在）
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        log.info("根据用户名[{}]查询到用户详情：{}", username, userDetails.getUsername());

                        // 新增：校验用户是否启用
                        if (!userDetails.isEnabled()) {
                            log.error("❌ JWT认证失败：用户名[{}]已被禁用", username);
                            throw BusinessException.userDisabled();
                        }

                        // 2. 验证令牌有效性
                        if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                            // 3. 构建认证令牌，存入SecurityContext
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            // 👇 新增调试日志：确认SecurityContext已正确设置认证信息
                            log.info("✅ JWT认证成功，用户[{}]已存入SecurityContext", username);
                            log.info("当前SecurityContext认证信息：{}", SecurityContextHolder.getContext().getAuthentication());
                            log.info("JWT认证通过，用户[{}]已自动登录，请求路径：{}", username, requestPath);
                        } else {
                            log.error("❌ 令牌验证失败：令牌与用户[{}]不匹配或已过期", username);
                            // 替换为自定义异常
                            throw BusinessException.tokenInvalid();
                        }
                    } catch (UsernameNotFoundException e) {
                        log.error("❌ JWT认证失败：用户名[{}]在数据库中不存在，请检查用户数据", username);
                        // 替换为自定义异常
                        throw BusinessException.userNotFound();
                    }
                } else {
                    log.error("❌ JWT认证失败：从令牌中解析出的用户名为空");
                    // 替换为自定义异常
                    throw new BusinessException(400, "令牌格式错误，无用户名信息");
                }
            } else {
                if (token == null) {
                    log.warn("⚠️ 请求[{}]未携带JWT令牌，跳过认证", requestPath);
                } else {
                    log.info("⚠️ 请求[{}]令牌解析失败，无有效认证", requestPath);
                }
            }
        } catch (BusinessException e) {
            // 捕获自定义业务异常，仅设置状态码，交给全局处理器返回响应
            log.error("❌ JWT认证业务异常 | 请求路径：{} | 状态码：{} | 错误信息：{}",
                    requestPath, e.getCode(), e.getMessage());
            response.setStatus(e.getCode());
            return;
        } catch (Exception e) {
            log.error("❌ JWT认证过滤器异常 | 请求路径：{} | 异常类型：{} | 错误信息：{}",
                    requestPath, e.getClass().getName(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } finally {
            // 仅当无异常终止时，放行请求
            if (!response.isCommitted()) {
                filterChain.doFilter(request, response);
                log.info("请求[{}]已放行，继续执行过滤器链", requestPath);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 排除无需认证的接口，提升性能
        return path.contains("/api/user/login")
                || path.contains("/api/user/register")
                || path.contains("/swagger-ui/")
                || path.contains("/v3/api-docs/")
                || path.contains("/actuator/");
    }
}