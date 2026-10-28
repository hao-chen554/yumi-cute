package com.yumi.cute.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * 已吊销 token 的黑名单
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private static final String PREFIX = "jwt:revoked";

    private final StringRedisTemplate redis;

    /**
     * 吊销：token已过期则无需记录
     */
    public void revoke(String jti, Date expireAt){
        if(jti == null || expireAt == null){
            return;
        }
        long ttlMs = expireAt.getTime() - System.currentTimeMillis();
        if(ttlMs <= 0){
            return;
        }
        redis.opsForValue().set(PREFIX + jti,"1", Duration.ofMillis(ttlMs));
    }

    public boolean isRevoked(String jti){
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
