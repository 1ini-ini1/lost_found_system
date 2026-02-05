package com.zjut.lost_found.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类（优化版）
 * 核心优化：新增日志排查、空值校验、异常细化，解决「当前登录用户不存在」问题
 */
@Component
@Slf4j
public class JwtUtil {

    // 从配置文件读取密钥（适配你的application.yml）
    @Value("${jwt.secret:zjutLostFoundSystem20240510SecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.issuer:zjut}")
    private String issuer;

    /**
     * 生成符合HS512要求的JWT令牌
     */
    public String generateToken(String username) {
        // 前置校验：用户名不能为空
        if (!StringUtils.hasText(username)) {
            log.error("生成令牌失败：用户名不能为空");
            throw new IllegalArgumentException("用户名不能为空");
        }

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        // 生成符合HS512要求的安全密钥
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .setSubject(username)          // 用户名作为唯一标识
                .setIssuedAt(now)              // 签发时间
                .setExpiration(expirationDate) // 过期时间（24小时）
                .setIssuer(issuer)             // 签发者
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.info("为用户[{}]生成JWT令牌：{}", username, token);
        return token;
    }

    /**
     * 解析令牌中的用户名（优化版：新增日志+空值校验+异常细化）
     */
    public String getUsernameFromToken(String token) {
        // 前置校验：令牌不能为空
        if (!StringUtils.hasText(token)) {
            log.error("令牌解析失败：令牌为空");
            return null;
        }

        try {
            // 生成安全密钥
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            // 解析令牌
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();

            // 获取用户名并校验
            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                log.error("令牌解析失败：令牌中用户名为空");
                return null;
            }

            log.info("令牌解析成功，用户名：{}", username);
            return username;

        } catch (ExpiredJwtException e) {
            log.error("JWT令牌已过期 | 过期时间：{} | 错误信息：{}",
                    e.getClaims().getExpiration(), e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.error("JWT令牌格式错误（如重复Bearer、令牌篡改） | 错误信息：{}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.error("JWT令牌签名错误（密钥不匹配） | 错误信息：{}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.error("JWT令牌无效 | 错误信息：{}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("JWT令牌解析失败 | 异常类型：{} | 错误信息：{}",
                    e.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 验证令牌有效性
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        boolean isValid = StringUtils.hasText(tokenUsername)
                && tokenUsername.equals(username)
                && !isTokenExpired(token);

        log.info("验证用户[{}]的令牌有效性：{}", username, isValid);
        return isValid;
    }

    /**
     * 判断令牌是否过期
     */
    private boolean isTokenExpired(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
            boolean isExpired = claims.getExpiration().before(new Date());
            if (isExpired) {
                log.warn("令牌已过期 | 过期时间：{}", claims.getExpiration());
            }
            return isExpired;
        } catch (Exception e) {
            log.error("判断令牌过期失败：{}", e.getMessage());
            return true; // 解析失败默认视为过期
        }
    }

    /**
     * 从请求头中提取令牌（自动去除Bearer前缀）
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            // 自动去除Bearer前缀（兼容大小写/多余空格）
            if (authHeader.toLowerCase().startsWith("bearer ")) {
                String token = authHeader.substring(7).trim();
                log.info("从请求头提取令牌：{}", token);
                return token;
            }
            log.warn("请求头Authorization格式错误，未以Bearer开头：{}", authHeader);
        }
        log.error("请求头Authorization为空");
        return null;
    }
}