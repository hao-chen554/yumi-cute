package com.yumi.cute.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储的统一接口。
 * 具体存到哪（本地磁盘 / MinIO / 阿里云 OSS），由实现类决定。
 */
public interface FileStorage {

    /**
     * 保存文件，返回可以直接访问的 URL
     */
    String save(MultipartFile file);
}