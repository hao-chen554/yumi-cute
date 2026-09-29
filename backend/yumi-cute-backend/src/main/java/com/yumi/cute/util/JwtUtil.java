package com.yumi.cute.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.expire-hours}")
    private Long expireHours;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发token
     **/
    public String generateToken(Long userId, String nickname) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireHours * 60 * 60 * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))    // 标准字段：主体，这里放userId
                .claim("nickname", nickname)         // 自定义字段
                .setIssuedAt(now)                       // 签发时间
                .setExpiration(expireDate)              // 过期时间
                .signWith(getKey())                    // 用密钥签名
                .compact();
    }

    /**
     * 解析并校验token，校验失败会抛异常
     **/
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
