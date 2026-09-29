package com.yumi.cute.storage;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
        String relativePath = "upload/" + LocalDate.now() + "/" + UUID.randomUUID() + ext;

        Path target = Paths.get(basePath, relativePath);

        try {
            // 自动创建日期目录（不存在才创建，存在就算了）
            Files.createDirectories(target.getParent());

            // 流式写入，避免把整个文件读进内存
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存文件失败: {}", target, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "图片保存失败，请稍后再试");
        }

        return urlPrefix + "/" + relativePath;
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return ".jpg";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return ".jpg";
        }
        String ext = filename.substring(dot).toLowerCase();
        return ext.matches("\\.[a-z0-9]{1,5}") ? ext : ".jpg";
    }
}