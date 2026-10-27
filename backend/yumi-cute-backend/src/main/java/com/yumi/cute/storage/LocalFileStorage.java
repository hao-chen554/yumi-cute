package com.yumi.cute.storage;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    @Value("${storage.local.path}")
    private String basePath;

    @Value("${storage.local.url-prefix}")
    private String urlPrefix;


    @Override
    public String save(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            return saveStream(in, ext);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "图片保存失败，请稍后再试");
        }
    }

    @Override
    public String save(byte[] data, String extension) {
        return saveStream(new ByteArrayInputStream(data), extension);
    }

    /**
     * URL -> 磁盘路径。不是本存储产生的地址返回null
     */
    private Path toPath(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return null;
        }
        String relative = url.substring(urlPrefix.length()).replaceFirst("^/+", "");
        return Paths.get(basePath, relative);
    }

    @Override
    public void delete(String url) {
        Path path = toPath(url);
        if (path == null) {
            return;
        }

        try {
            // 清理文件，若文件已不存在不报错
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("删除文件失败：{}", path, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "删除文件失败：" + path);
        }
    }

    /**
     * 两种 来源（用户上传 / AI生成）最终都走这里
     */
    private String saveStream(InputStream in, String extension) {
        String ext = normalizedValidator(extension);
        String relativePath = "upload/" + LocalDate.now() + "/" + UUID.randomUUID() + ext;
        Path target = Paths.get(basePath, relativePath);

        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存文件失败：{}", target, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }

        return urlPrefix + "/" + relativePath;
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return ".jpg";
        }

        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return ".jpg";
        }

        return normalizedValidator(fileName.substring(dot));
    }

    private String normalizedValidator(String ext) {
        if (ext == null) {
            return ".jpg";
        }

        String lower = ext.toLowerCase();
        return lower.matches("\\.[a-z0-9]{1,5}") ? lower : ".jpg";
    }

    @Override
    public InputStream openStream(String url) {
        Path path = toPath(url);
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e){
            throw new BizException(ResultCode.SYSTEM_ERROR,"读取文件失败：" + path);
        }
    }

    @Override
    public long size(String url){
        Path path = toPath(url);
        if (path == null || !Files.exists(path)) {
            return -1;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }
}