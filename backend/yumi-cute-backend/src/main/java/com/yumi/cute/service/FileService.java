package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.entity.UserFile;
import com.yumi.cute.mapper.UserFileMapper;
import com.yumi.cute.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

    /** 文件用途 **/
    public static final int BIZ_TYPE_UNKNOWN = 0;
    public static final int BIZ_TYPE_AVATAR = 1;
    public static final int BIZ_TYPE_PERSON = 2;
    public static final int BIZ_TYPE_PET = 3;
    public static final int BIZ_TYPE_RESULT = 4;

    /**
     * 允许上传的图片类型白名单
     */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private final FileStorage fileStorage;
    private final UserFileMapper userFileMapper;

    public String upload(Long userId, MultipartFile file) {

        // 业务校验（和"存在哪"无关，所以留在 Service 里）
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片不能超过 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(ResultCode.PARAM_ERROR, "只支持 jpg、png、webp 格式的图片");
        }

        // 存文件这件事，交给存储实现去做，拿到url
        String url = fileStorage.save(file);

        try {
            register(userId,url,BIZ_TYPE_UNKNOWN,file.getSize());
        } catch (Exception e) {
            log.error("文件已落盘但登记属主失败，url={}，将成为孤儿文件", url, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }
        return url;
    }

    /**
     * 登记文件属主。
     * 用户上传和 AI 出图都走这里
     */
    public void register(Long userId, String url, int bizType, long sizeBytes) {
        UserFile record = new UserFile();
        record.setUserId(userId);
        record.setUrl(url);
        record.setBizType(bizType);
        record.setSizeBytes(sizeBytes);
        userFileMapper.insert(record);
    }

    /**
     * 校验一个url是不是这个用户上传的
     */
    public void assertOwned(Long userId, String url, String roleName) {
        if (url == null || url.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少" + roleName);
        }

        UserFile file = userFileMapper.selectByUrl(url);
        if (file == null || !file.getUserId().equals(userId)) {
            throw new BizException(ResultCode.PARAM_ERROR, roleName + "无效，请重新上传");
        }
    }
}