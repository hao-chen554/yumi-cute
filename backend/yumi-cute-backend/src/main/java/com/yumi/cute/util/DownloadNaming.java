package com.yumi.cute.util;

/**
 * 下载文件名规则。
 *
 * <p>抽成无状态工具类是为了可单测 —— 文件名错了不会报错，只会让用户下载到
 * 顺序混乱或没有扩展名的文件，属于「静默出错」，必须靠测试兜住。
 */
public final class DownloadNaming {

    private DownloadNaming() {
    }

    /** 序号补零到两位：否则解压后 10.jpg 会排在 2.jpg 前面（字典序） */
    public static String padSeq(Integer seqNo) {
        return String.format("%02d", seqNo == null ? 0 : seqNo);
    }

    /**
     * 从 URL 里取扩展名；取不到、或像 .com 这种太长的一律回落到 .jpg。
     *
     * <p><b>必须先剥掉查询串再算长度</b>：带签名的 URL（如 {@code a.png?exp=..&sig=..}）
     * 里 {@code length - dot} 会把 query 也算进去，导致所有带参数的 URL 都被误判成 .jpg。
     * 接 OSS 之后返回的就是带签名的 URL，这个坑一定会踩。
     */
    public static String extensionOf(String url) {
        if (url == null || url.isBlank()) {
            return ".jpg";
        }
        int q = url.indexOf('?');
        String bare = q < 0 ? url : url.substring(0, q);

        int dot = bare.lastIndexOf('.');
        int slash = bare.lastIndexOf('/');
        if (dot > slash && bare.length() - dot <= 6) {
            return bare.substring(dot);
        }
        return ".jpg";
    }

    /** 订单号后 6 位，用来区分「同一风格买了多次」的压缩包 */
    public static String tailOf(String orderNo) {
        if (orderNo == null || orderNo.length() <= 6) {
            return "download";
        }
        return orderNo.substring(orderNo.length() - 6);
    }

    /** 由文件名推 content-type */
    public static String contentTypeOf(String fileName) {
        String f = fileName == null ? "" : fileName.toLowerCase();
        if (f.endsWith(".png")) {
            return "image/png";
        }
        if (f.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
