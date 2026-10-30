package com.yumi.cute.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 下载文件名规则。
 *
 * <p>这些函数出错不会抛异常，只会让用户下载到「顺序错乱」或「没有扩展名」的文件 ——
 * 属于静默故障，只能靠测试兜住。
 */
class DownloadNamingTest {

    @Test
    @DisplayName("序号补零到两位，保证解压后顺序正确")
    void padSeq_padsToTwoDigits() {
        assertEquals("01", DownloadNaming.padSeq(1));
        assertEquals("09", DownloadNaming.padSeq(9));
        assertEquals("10", DownloadNaming.padSeq(10));
        assertEquals("06", DownloadNaming.padSeq(6));
    }

    @Test
    @DisplayName("序号为 null 时给 00，不抛 NPE")
    void padSeq_nullBecomesZeroZero() {
        assertEquals("00", DownloadNaming.padSeq(null));
    }

    @Test
    @DisplayName("从 URL 取扩展名")
    void extensionOf_readsFromUrl() {
        assertEquals(".png", DownloadNaming.extensionOf("http://x/files/a.png"));
        assertEquals(".webp", DownloadNaming.extensionOf("http://x/files/a.webp"));
        assertEquals(".jpg", DownloadNaming.extensionOf("http://x/files/a.jpg"));
    }

    @Test
    @DisplayName("URL 带查询串时扩展名不被污染")
    void extensionOf_ignoresQuery() {
        assertEquals(".png", DownloadNaming.extensionOf("http://x/files/a.png?exp=1&sig=abc"));
    }

    @Test
    @DisplayName("OSS 签名 URL（query 很长）也能正确取到扩展名")
    void extensionOf_handlesOssSignedUrl() {
        String oss = "https://b.oss-cn-beijing.aliyuncs.com/upload/2026/10/a.webp"
                + "?Expires=1761000000&OSSAccessKeyId=LTAI5tXXXX&Signature=abcdefghijk%3D";
        assertEquals(".webp", DownloadNaming.extensionOf(oss));
    }

    @Test
    @DisplayName("没有扩展名 / 域名里的点 / 扩展名过长 → 一律回落 .jpg")
    void extensionOf_fallsBackToJpg() {
        assertEquals(".jpg", DownloadNaming.extensionOf("http://x/files/abcdef"));
        // 域名里的 .com 在斜杠之前，不能当成扩展名
        assertEquals(".jpg", DownloadNaming.extensionOf("http://x.com/files/abcdef"));
        // 超过 6 个字符不像扩展名
        assertEquals(".jpg", DownloadNaming.extensionOf("http://x/files/a.verylongext"));
        assertEquals(".jpg", DownloadNaming.extensionOf(null));
        assertEquals(".jpg", DownloadNaming.extensionOf(""));
    }

    @Test
    @DisplayName("压缩包用订单号后 6 位，区分同风格多次购买")
    void tailOf_takesLastSixChars() {
        assertEquals("539163", DownloadNaming.tailOf("YM202610211511539163"));
    }

    @Test
    @DisplayName("订单号太短 / 为 null → 回落到 download")
    void tailOf_fallsBackToDownload() {
        assertEquals("download", DownloadNaming.tailOf("YM1"));
        assertEquals("download", DownloadNaming.tailOf("123456"));
        assertEquals("download", DownloadNaming.tailOf(null));
    }

    @Test
    @DisplayName("由文件名推 content-type")
    void contentTypeOf_mapsByExtension() {
        assertEquals("image/png", DownloadNaming.contentTypeOf("a.png"));
        assertEquals("image/webp", DownloadNaming.contentTypeOf("a.WEBP"));
        assertEquals("image/jpeg", DownloadNaming.contentTypeOf("a.jpg"));
        assertEquals("image/jpeg", DownloadNaming.contentTypeOf("a.unknown"));
        assertEquals("image/jpeg", DownloadNaming.contentTypeOf(null));
    }

    @Test
    @DisplayName("拼出来的文件名形如「日系清新-03.png」")
    void composedFileName_looksRight() {
        String style = "日系清新";
        String name = style + "-" + DownloadNaming.padSeq(3)
                + DownloadNaming.extensionOf("http://x/a.png");
        assertEquals("日系清新-03.png", name);
    }
}
