package com.yumi.cute.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储的统一接口。
 * 具体存到哪（本地磁盘 / MinIO / 阿里云 OSS），由实现类决定。
 */
public interface FileStorage {

    /**
     * 保存用户上传的文件，返回可以直接访问的 URL
     */
    String save(MultipartFile file);

    /**
     * 保存一段字节数据（比如AI生成返回的图片），返回可访问的URL
     */
    String save(byte[] data, String extension);

    /**
     * 删除一个文件（按访问地址），文件若不存在，静默返回
     */
    void delete(String url);

    /**
     * 打开一个文件用于读取
     * 文件不存在时返回null，由调用方决定”跳过“还是”报错“
     * （zip 下载跳过缺失的单张，单张下载要报错）
     */
    InputStream openStream(String url);

    /**
     * 文件字节数；不存在时返回-1
     */
    long size(String url);
}