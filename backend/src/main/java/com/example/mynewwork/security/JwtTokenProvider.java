package com.example.mynewwork.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JWT 令牌提供者
 *
 * 负责生成、解析和验证 JWT 令牌
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * JWT 密钥最小长度（256位 = 32字符）
     */
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    /**
     * 初始化校验 JWT 密钥长度
     */
    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足，至少需要 " + MIN_SECRET_LENGTH + " 个字符");
        }
        log.info("JWT 配置初始化完成，令牌有效期: {}ms", jwtExpirationInMs);
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 生成 JWT 令牌
     *
     * @param authentication 认证信息
     * @return JWT 令牌
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("为用户 {} 生成 JWT 令牌", username);
        return token;
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * 验证 JWT 令牌有效性
     *
     * @param token JWT 令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("无效的 JWT 签名: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("无效的 JWT 令牌: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("JWT 令牌已过期: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的 JWT 令牌: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT 声明字符串为空: {}", ex.getMessage());
        }
        return false;
    }
}