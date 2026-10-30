package com.yumi.cute.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件访问签名 —— 这是唯一挡住「拿到链接就能看别人照片」的机制。
 *
 * <p>核心是三条不可绕过的约束：
 * <ol>
 *   <li>换路径就失效（签名里绑了路径）</li>
 *   <li>改过期时间就失效（exp 参与了签名）</li>
 *   <li>过期就失效</li>
 * </ol>
 */
class FileSignerTest {

    private static final String SECRET = "unit-test-secret-key-must-be-long-enough-32";
    private static final String URL = "http://localhost:8080/files/upload/2026-10-21/a.jpg";
    private static final String PATH = "/files/upload/2026-10-21/a.jpg";

    /** 按 TTL 建一个新的 signer（TTL 传负数就能造出「已过期」的场景） */
    private static FileSigner signerWithTtl(long ttlSeconds) {
        return new FileSigner(SECRET, ttlSeconds);
    }

    private static String paramOf(String signedUrl, String name) {
        Matcher m = Pattern.compile("[?&]" + name + "=([^&]+)").matcher(signedUrl);
        assertTrue(m.find(), "签名后的 URL 里应该有 " + name);
        return m.group(1);
    }

    @Test
    @DisplayName("签出来的地址同时带 exp 和 sig")
    void sign_appendsBothParam() {
        String signed = signerWithTtl(1800).sign(URL);
        assertNotNull(paramOf(signed, "exp"));
        assertNotNull(paramOf(signed, "sig"));
        assertTrue(signed.startsWith(URL), "原地址不能被改动");
    }

    @Test
    @DisplayName("URL 本来带 query 时用 & 拼接")
    void sign_usesAmpersandWhenQueryExists() {
        String signed = signerWithTtl(1800).sign(URL + "?v=1");
        assertTrue(signed.contains("?v=1&exp="), "应该是 & 而不是第二个 ?");
    }

    @Test
    @DisplayName("自己签的，自己能验过")
    void verify_acceptsOwnSignature() {
        String signed = signerWithTtl(1800).sign(URL);
        assertTrue(signerWithTtl(1800).verify(PATH, paramOf(signed, "exp"), paramOf(signed, "sig")));
    }

    @Test
    @DisplayName("换一个路径 → 验不过（签名与路径绑定）")
    void verify_rejectsDifferentPath() {
        String signed = signerWithTtl(1800).sign(URL);
        assertFalse(signerWithTtl(1800)
                .verify("/files/upload/2026-10-21/OTHER.jpg", paramOf(signed, "exp"), paramOf(signed, "sig")));
    }

    @Test
    @DisplayName("把 exp 改大想续命 → 验不过（exp 参与了签名）")
    void verify_rejectsTamperedExp() {
        String signed = signerWithTtl(1800).sign(URL);
        long exp = Long.parseLong(paramOf(signed, "exp"));
        assertFalse(signerWithTtl(1800)
                .verify(PATH, String.valueOf(exp + 86400), paramOf(signed, "sig")));
    }

    @Test
    @DisplayName("篡改 sig → 验不过")
    void verify_rejectsTamperedSignature() {
        String signed = signerWithTtl(1800).sign(URL);
        String sig = paramOf(signed, "sig");
        String tampered = (sig.charAt(0) == 'A' ? "B" : "A") + sig.substring(1);
        assertFalse(signerWithTtl(1800).verify(PATH, paramOf(signed, "exp"), tampered));
    }

    @Test
    @DisplayName("已过期 → 验不过")
    void verify_rejectsExpired() {
        String signed = signerWithTtl(-10).sign(URL);
        assertFalse(signerWithTtl(1800).verify(PATH, paramOf(signed, "exp"), paramOf(signed, "sig")));
    }

    @Test
    @DisplayName("换一个密钥签的 → 验不过")
    void verify_rejectsOtherSecret() {
        String signed = signerWithTtl(1800).sign(URL);
        FileSigner other = new FileSigner("a-completely-different-secret-key-32b!", 1800);
        assertFalse(other.verify(PATH, paramOf(signed, "exp"), paramOf(signed, "sig")));
    }

    @Test
    @DisplayName("任一参数为 null → 直接 false，不抛异常")
    void verify_nullArgs_returnFalse() {
        FileSigner signer = signerWithTtl(1800);
        assertFalse(signer.verify(null, "1", "x"));
        assertFalse(signer.verify(PATH, null, "x"));
        assertFalse(signer.verify(PATH, "1", null));
    }

    @Test
    @DisplayName("exp 不是数字 → false，不抛 NumberFormatException")
    void verify_nonNumericExp_returnFalse() {
        assertFalse(signerWithTtl(1800).verify(PATH, "not-a-number", "x"));
    }

    @Test
    @DisplayName("空地址签名原样返回，不报错")
    void sign_blankUrl_returnsAsIs() {
        assertNull(signerWithTtl(1800).sign(null));
        assertEquals("  ", signerWithTtl(1800).sign("  "));
    }

    @Test
    @DisplayName("stripQuery 去掉查询串，保留裸地址")
    void stripQuery_removesQueryOnly() {
        assertEquals(URL, FileSigner.stripQuery(URL + "?exp=1&sig=abc"));
        assertEquals(URL, FileSigner.stripQuery(URL));
        assertNull(FileSigner.stripQuery(null));
    }
}
