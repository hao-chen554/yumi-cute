package com.yumi.cute.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * 文件访问签名。
 *
 * 签名内容只有「路径 + 过期时间」，不含用户身份 —— 所以签出来的是一张
 * 「单据」而不是一张「通行证」：泄露了最多让人看这一张图，动不了别的接口。
 * 这是云存储预签名 URL 的标准做法。
 */
@Component
public class FileSigner {

    private final SecretKeySpec key;
    private final long ttlSeconds;

    public FileSigner(@Value("${file.sign.secret}") String secret,
                      @Value("${file.sign.ttl-seconds:1800}") long ttlSeconds) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.ttlSeconds = ttlSeconds;
    }

    /** 给一个文件地址签名，返回「原地址 + exp + sig」 */
    public String sign(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String path = pathOf(url);
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String sig = hmac(path + "|" + exp);

        return url + (url.contains("?") ? "&" : "?") + "exp=" + exp + "&sig=" + sig;
    }

    /** 校验：签名对得上，且没过期 */
    public boolean verify(String path, String exp, String sig) {
        if (path == null || exp == null || sig == null) {
            return false;
        }

        long expAt;
        try {
            expAt = Long.parseLong(exp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expAt) {
            return false;
        }

        String expected = hmac(path + "|" + exp);

        // ★ 常量时间比较：避免攻击者通过响应时间差逐字节猜出签名
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8));
    }

    /** 去掉查询串，拿回裸地址（提交、存库、查属主都用它） */
    public static String stripQuery(String url) {
        if (url == null) {
            return null;
        }
        int i = url.indexOf('?');
        return i < 0 ? url : url.substring(0, i);
    }

    /** 取 URL 的路径部分，和拦截器看到的 requestURI 对齐 */
    private static String pathOf(String url) {
        try {
            return URI.create(url).getPath();
        } catch (Exception e) {
            return stripQuery(url);
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("文件签名失败", e);
        }
    }
}