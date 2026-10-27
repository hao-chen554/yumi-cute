package com.yumi.cute.util;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.storage.FileStorage;
import com.yumi.cute.vo.DownloadPlan;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 把下载计划真正写入HTTP响应
 */
public final class DownloadWriter {
    private DownloadWriter() {}

    /**
     * 单文件：先把校验做完，再动响应头
     */
    public static void writeSingle(HttpServletResponse response, FileStorage storage, DownloadPlan plan) throws IOException {
        DownloadPlan.Entry entry = plan.entries().get(0);

        // 前置校验
        InputStream in = storage.openStream(entry.url());
        if(in == null){
            throw new BizException(ResultCode.NOT_FOUND,"文件不存在（可能已被隐私清理）");
        }
        long size = storage.size(entry.url());

        response.setContentType(plan.contentType());
        response.setHeader("Content-Disposition",contentDisposition(plan.fileName()));
        if(size > 0){
            response.setContentLengthLong(size);
        }

        try(InputStream src = in){
            src.transferTo(response.getOutputStream());
        }
        response.flushBuffer();
    }

    /**
     * 整套：边读边写zip，内存占用与包大小无关
     */
    public static void writeZip(HttpServletResponse response, FileStorage storage, DownloadPlan plan) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",contentDisposition(plan.fileName()));

        try(ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())){
            for(DownloadPlan.Entry entry : plan.entries()){
                InputStream in = storage.openStream(entry.url());
                if(in == null){
                    // 单张缺失不该让整个包失败
                    continue;
                }
                zos.putNextEntry(new ZipEntry(entry.name()));
                try(InputStream src = in){
                    src.transferTo(zos);
                }
                zos.closeEntry();
            }
            zos.finish();
        }
        response.flushBuffer();
    }

    /**
     * HTTP 头只能是 ASCII，中文文件名必须走 RFC 5987：
     *   filename="____.jpg"            给老客户端兜底
     *   filename*=UTF-8''%E6%97%A5...  现代浏览器优先用这个
     *
     * URLEncoder 会把空格编成 '+'，但 RFC 5987 要求 %20 —— 必须手动替换。
     */
    private static String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String fallback = fileName.replaceAll("[^\\x20-\\x7E]", "_");
        return "attachment; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }
}
