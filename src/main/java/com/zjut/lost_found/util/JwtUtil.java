package com.zjut.lost_found.util;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * JWT工具类（0基础必懂）
 * 核心功能：生成令牌、验证令牌、解析令牌中的用户信息
 * JWT（JSON Web Token）：登录成功后返回给前端的“凭证”，前端后续请求需携带此令牌，证明用户已登录
 */
@Component // 标识为Spring组件，让Spring管理，后续可通过@Autowired注入使用
@Slf4j // Lombok注解，自动生成日志对象，用于打印日志（排查错误）
public class JwtUtil {

    // 从application.yml中读取JWT密钥（@Value注解用于读取配置文件中的值）
    @Value("${jwt.secret}")
    private String secret;

    // 从配置文件读取令牌有效期（毫秒）
    @Value("${jwt.expiration}")
    private long expiration;

    // 从配置文件读取签发者
    @Value("${jwt.issuer}")
    private String issuer;

    // 初始化密钥（编码处理，提升安全性）
    @PostConstruct // 该方法在对象创建后自动执行，用于初始化操作
    public void init() {
        secret = secret + "lost_found_salt"; // 加盐处理，避免密钥泄露后被破解（salt是自定义的随机字符串）
    }

    /**
     * 生成JWT令牌（登录成功后调用）
     * @param username 登录账号（作为令牌的subject，唯一标识用户）
     * @return 加密后的JWT令牌（字符串）
     */
    public String generateToken(String username) {
        Date now = new Date(); // 当前时间（令牌签发时间）
        Date expirationDate = new Date(now.getTime() + expiration); // 令牌过期时间

        // 构建令牌，链式调用，设置各项参数
        return Jwts.builder()
                .setSubject(username) // 设置用户唯一标识（此处用账号，确保唯一）
                .setIssuedAt(now) // 设置令牌签发时间
                .setExpiration(expirationDate) // 设置令牌过期时间
                .setIssuer(issuer) // 设置签发者
                .signWith(SignatureAlgorithm.HS512, secret) // 签名算法：HS512，密钥：secret（加密用）
                .compact(); // 生成令牌字符串
    }

    /**
     * 从令牌中解析出用户名（验证令牌时调用）
     * @param token JWT令牌
     * @return 用户名（解析失败返回null）
     */
    public String getUsernameFromToken(String token) {
        try {
            // 解析令牌：用密钥验证签名，获取令牌中的负载信息（claims）
            Claims claims = Jwts.parser()
                    .setSigningKey(secret) // 用密钥验证签名（确保令牌未被篡改）
                    .parseClaimsJws(token) // 解析令牌
                    .getBody(); // 获取令牌中的负载信息（存储了用户名等数据）
            return claims.getSubject(); // 返回subject（即用户名）
        } catch (Exception e) {
            // 解析失败（如令牌篡改、令牌过期），打印错误日志，返回null
            log.error("JWT令牌解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证令牌是否有效（请求接口时调用）
     * @param token JWT令牌
     * @param username 待验证的用户名
     * @return true：有效；false：无效（过期、签名错误、用户名不匹配）
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token); // 从令牌中解析用户名
        // 验证条件：令牌解析出的用户名不为空、与传入用户名一致、令牌未过期
        return StringUtils.hasText(tokenUsername)
                && tokenUsername.equals(username)
                && !isTokenExpired(token);
    }

    /**
     * 判断令牌是否过期（私有方法，仅当前类使用）
     * @param token JWT令牌
     * @return true：已过期；false：未过期
     */
    private boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            Date expirationDate = claims.getExpiration(); // 获取令牌过期时间
            return expirationDate.before(new Date()); // 过期时间早于当前时间，说明已过期
        } catch (Exception e) {
            log.error("JWT令牌过期判断失败：{}", e.getMessage());
            return true; // 解析失败默认视为过期（避免非法请求）
        }
    }

    /**
     * 从HTTP请求头中获取JWT令牌（请求接口时调用）
     * 约定：前端请求头格式为 Authorization: Bearer 令牌字符串（固定格式，前端需遵守）
     * @param request HTTP请求（前端发送的请求）
     * @return JWT令牌（无令牌返回null）
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization"); // 获取请求头中的Authorization字段
        // 判断请求头是否不为空，且以“Bearer ”开头（注意有空格）
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 截取Bearer后面的令牌部分（去掉前7个字符）
        }
        return null; // 无令牌返回null
    }
}