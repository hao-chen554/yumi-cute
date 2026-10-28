package com.yumi.cute.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;

/**
 * 登陆失败计数
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    /**
     * 计数 + 首次设TTL，原子完成
     */
    private static final RedisScript<Long> HIT_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR',KEYS[1]) " +
                    "if c == 1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]) end " +
                    "return c",
            Long.class
    );

    private static final String USER_KEY = "login:fail:user:";
    private static final String IP_KEY = "login:fail:ip:";

    private final StringRedisTemplate redis;

    @Value("${security.login-limit.max-fail:5}")
    private int maxFailUser;

    @Value("${security.login-limit.window-minutes:10}")
    private long windowMinutes;

    @Value("${security.ip-limit.max-fail:20}")
    private int maxFailIp;

    @Value("${security.ip-limit.enable}")
    private boolean ipLimitEnabled;

    /**
     * 用户统一小写：数据库的 utf8mb4_general_ci 本身大小写不敏感，这里跟着归一，防止用 Yumi / YUMI 绕开计数
     */
    private String userKey(String username) {
        return USER_KEY + username.toLowerCase();
    }

    private String ipKey(String ip) {
        return IP_KEY + ip;
    }

    /**
     * 记一次失败，返回该用户名累计失败次数
     */
    public long hitUser(String username) {
        return hit(userKey(username), windowMinutes);
    }

    /**
     * 记一次 IP 失败
     */
    public long hitIp(String ip) {
        return hit(ipKey(ip), windowMinutes);
    }

    private long hit(String key, long minutes) {
        Long c = redis.execute(HIT_SCRIPT, List.of(key), String.valueOf(Duration.ofMinutes(minutes).toMillis()));
        return c == null ? 1L : c;
    }

    public boolean isUserLocked(String username) {
        return count(userKey(username)) >= maxFailUser;
    }

    private long count(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? 0L : Long.parseLong(v);
    }

    /**
     * 登陆成功：清掉这个用户名的失败计数
     */
    public void resetAll(String username) {
        redis.delete(userKey(username));
    }

    /**
     * 登陆成功：用户名和IP的失败计数一起清掉
     */
    public void resetAll(String username, String ip) {
        redis.delete(List.of(userKey(username), ipKey(ip)));
    }

    /**
     * 取客户端IP
     */
    public String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("x-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    public long userFails(String username) {
        return count(userKey(username));
    }

    public long ipFails(String ip) {
        return count(ipKey(ip));
    }

    public boolean isIpLocked(String ip){
        return ipLimitEnabled && count(ipKey(ip)) >= maxFailIp;
    }
}
