package com.yumi.cute.storage;

import org.springframework.web.multipart.MultipartFile;

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
}