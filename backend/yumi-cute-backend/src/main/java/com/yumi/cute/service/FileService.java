package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.storage.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class FileService {

    /** 允许上传的图片类型白名单 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final FileStorage fileStorage;

    public FileService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public String upload(MultipartFile file) {

        // 业务校验（和"存在哪"无关，所以留在 Service 里）
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片不能超过 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "只支持 jpg、png、webp 格式的图片");
        }

        // 存文件这件事，交给存储实现去做
        return fileStorage.save(file);
    }
}