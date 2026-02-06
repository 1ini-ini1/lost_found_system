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
import java.util.Map;

/**
 * JWT工具类（最终优化版）
 * 核心功能：生成JWT令牌（单用户名/Map载荷）、解析令牌用户名、验证令牌有效性、从请求头提取令牌
 * 优化点：日志规范、空值严格校验、异常细化捕获、安全密钥生成、自动兼容Bearer前缀、新增Map载荷重载方法
 * 适配性：同时兼容单用户名令牌生成（基础服务）和多信息Map载荷生成（UserService核心业务）
 */
@Component
@Slf4j
public class JwtUtil {

    // 从配置文件读取JWT配置，添加默认值避免配置缺失报错，适配application.yml/properties
    @Value("${jwt.secret:zjutLostFoundSystem20240510SecretKey}")
    private String secret;

    // 令牌过期时间（毫秒），默认24小时
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    // 令牌签发者
    @Value("${jwt.issuer:zjut}")
    private String issuer;

    /**
     * 生成符合HS512算法的JWT令牌（单用户名，基础版，兼容所有服务）
     * @param username 用户名（唯一标识，建议为用户账号）
     * @return 有效JWT令牌字符串
     * @throws IllegalArgumentException 用户名空时抛出非法参数异常
     */
    public String generateToken(String username) {
        // 前置严格校验：用户名非空且非空白字符
        if (!StringUtils.hasText(username)) {
            log.error("JWT令牌生成失败【单用户名】：用户名不能为空或仅含空白字符");
            throw new IllegalArgumentException("用户名不能为空");
        }

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        SecretKey secretKey = getSecretKey();

        // 构建JWT令牌，设置核心载荷和签名
        String token = Jwts.builder()
                .setSubject(username)          // 核心载荷：用户名（唯一标识）
                .setIssuedAt(now)              // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .setIssuer(issuer)             // 签发者
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.info("JWT令牌生成成功【单用户名】 | 用户名：{} | 签发者：{} | 过期时长：{}小时",
                username, issuer, expiration / (60 * 60 * 1000));
        return token;
    }

    /**
     * 生成符合HS512算法的JWT令牌（Map载荷，增强版，适配UserService核心业务）
     * @param claims 自定义载荷（可存放userId、username、role等多维度信息）
     * @return 有效JWT令牌字符串
     * @throws IllegalArgumentException 载荷为空/无有效数据时抛出非法参数异常
     */
    public String generateToken(Map<String, Object> claims) {
        // 前置严格校验：载荷非空且包含有效数据
        if (claims == null || claims.isEmpty()) {
            log.error("JWT令牌生成失败【Map载荷】：自定义载荷不能为空或无有效数据");
            throw new IllegalArgumentException("JWT自定义载荷不能为空");
        }

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        SecretKey secretKey = getSecretKey();

        // 构建JWT令牌，装载自定义载荷+基础元信息
        String token = Jwts.builder()
                .setClaims(claims)             // 装载自定义多维度载荷
                .setIssuedAt(now)              // 签发时间
                .setExpiration(expirationDate) // 过期时间
                .setIssuer(issuer)             // 签发者
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        log.info("JWT令牌生成成功【Map载荷】 | 签发者：{} | 过期时长：{}小时 | 载荷维度：{}",
                issuer, expiration / (60 * 60 * 1000), claims.size());
        log.debug("JWT令牌自定义载荷详情：{}", claims);
        return token;
    }

    /**
     * 从JWT令牌中解析出用户名
     * @param token JWT令牌字符串
     * @return 解析成功返回用户名，失败返回null
     */
    public String getUsernameFromToken(String token) {
        // 前置校验：令牌非空
        if (!StringUtils.hasText(token)) {
            log.error("JWT令牌解析失败：令牌字符串为空或仅含空白字符");
            return null;
        }

        try {
            // 解析令牌并获取载荷信息，自动校验签名有效性
            Claims claims = parseTokenToClaims(token);
            // 提取用户名并二次校验（兼容单用户名/Map载荷，均从subject提取）
            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                log.error("JWT令牌解析失败：令牌载荷中用户名为空");
                return null;
            }

            log.info("JWT令牌解析成功 | 用户名：{} | 签发者：{}", username, claims.getIssuer());
            return username;

        } catch (ExpiredJwtException e) {
            log.error("JWT令牌解析失败：令牌已过期 | 过期时间：{}", e.getClaims().getExpiration());
        } catch (MalformedJwtException e) {
            log.error("JWT令牌解析失败：令牌格式错误（可能被篡改/拼接错误）");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("JWT令牌解析失败：签名验证不通过（密钥不匹配/令牌被篡改）");
        } catch (UnsupportedJwtException e) {
            log.error("JWT令牌解析失败：不支持的令牌算法/格式");
        } catch (IllegalArgumentException e) {
            log.error("JWT令牌解析失败：令牌载荷为空/格式非法");
        } catch (JwtException e) {
            log.error("JWT令牌解析失败：令牌无效 | 具体原因：{}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT令牌解析失败：未知异常 | 异常类型：{}", e.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * 验证JWT令牌是否对指定用户有效
     * 验证规则：令牌非空 + 用户名匹配 + 令牌未过期
     * @param token 待验证令牌
     * @param username 待验证用户名
     * @return 有效返回true，无效返回false
     */
    public boolean validateToken(String token, String username) {
        // 前置校验：用户名和令牌均非空
        if (!StringUtils.hasText(username) || !StringUtils.hasText(token)) {
            log.error("JWT令牌验证失败：用户名或令牌为空");
            return false;
        }

        String tokenUsername = getUsernameFromToken(token);
        boolean isValid = StringUtils.hasText(tokenUsername)
                && tokenUsername.equals(username)
                && !isTokenExpired(token);

        log.info("JWT令牌验证结果 | 待验证用户名：{} | 令牌内用户名：{} | 是否有效：{}",
                username, tokenUsername, isValid);
        return isValid;
    }

    /**
     * 从Http请求头中提取JWT令牌
     * 自动兼容Authorization头的Bearer格式：Bearer {token}，自动去除前缀并trim
     * @param request HttpServletRequest请求对象
     * @return 提取成功返回纯令牌字符串，失败返回null
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        // 从请求头获取Authorization字段（JWT令牌标准传递方式）
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            String trimHeader = authHeader.trim();
            // 兼容Bearer前缀的大小写，统一转小写判断
            if (trimHeader.toLowerCase().startsWith("bearer ")) {
                String token = trimHeader.substring(7).trim();
                log.debug("从请求头提取JWT令牌成功：已去除Bearer前缀");
                return token;
            }
            log.warn("请求头Authorization格式警告：未以「Bearer 」为前缀（注意后接空格），直接返回原始令牌");
            // 兼容直接传递令牌（无Bearer前缀）的场景
            return trimHeader;
        }
        log.debug("从请求头提取JWT令牌失败：Authorization请求头为空");
        return null;
    }

    // ========== 私有工具方法（抽离复用，简化代码） ==========
    /**
     * 生成HS512算法安全密钥（指定UTF-8编码，避免中文密钥乱码）
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析令牌到Claims载荷（抽离复用，避免重复代码）
     */
    private Claims parseTokenToClaims(String token) {
        SecretKey secretKey = getSecretKey();
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 判断JWT令牌是否已过期
     * @param token JWT令牌
     * @return 过期返回true，未过期返回false；解析失败默认视为过期
     */
    private boolean isTokenExpired(String token) {
        try {
            Claims claims = parseTokenToClaims(token);
            boolean isExpired = claims.getExpiration().before(new Date());
            if (isExpired) {
                log.warn("JWT令牌已过期 | 过期时间：{} | 当前时间：{}",
                        claims.getExpiration(), new Date());
            }
            return isExpired;
        } catch (Exception e) {
            log.error("判断JWT令牌是否过期失败，默认视为过期 | 原因：{}", e.getMessage());
            return true; // 解析失败/令牌异常，默认视为过期，拒绝访问
        }
    }
}