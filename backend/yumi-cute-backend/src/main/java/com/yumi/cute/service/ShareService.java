package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.entity.StyleTemplate;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PhotoTaskMapper;
import com.yumi.cute.mapper.StyleTemplateMapper;
import com.yumi.cute.util.FileSigner;
import com.yumi.cute.vo.ShareInfoVO;
import com.yumi.cute.vo.ShareViewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShareService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_STYLE = "人宠写真";

    private final PhotoOrderMapper photoOrderMapper;
    private final PhotoTaskMapper photoTaskMapper;
    private final StyleTemplateMapper styleTemplateMapper;
    private final FileSigner fileSigner;

    @Value("${share.ttl-days:7}")
    private int ttlDays;

    /**
     * 生成分享链接。若已有分享，旧链接会被覆盖失效 —— 前端应先 GET 查询现状
     */
    @Transactional(rollbackFor = Exception.class)
    public ShareInfoVO createShare(Long userId, Long orderId) {
        String token = newToken();

        if (photoOrderMapper.enableShare(orderId, userId, token, ttlDays) == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单不存在或还没有可分享的作品！");
        }

        PhotoOrder order = photoOrderMapper.selectMineById(orderId, userId);
        log.info("生成分享：订单 {} 用户 {} 有效期 {} 天", orderId, userId, ttlDays);
        return new ShareInfoVO(token, order.getShareExpireAt());
    }

    /**
     * 查询当前分享状态：没有有效分享返回NULL
     */
    public ShareInfoVO getShare(Long userId, Long orderId) {
        if (photoOrderMapper.selectMineById(orderId, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在！");
        }

        // 判断是否有有效分享
        PhotoOrder valid = photoOrderMapper.selectMyValidShare(orderId, userId);
        if (valid == null) {
            return null;
        }

        return new ShareInfoVO(valid.getShareToken(), valid.getShareExpireAt());
    }

    /**
     * 撤销分享，幂等
     */
    public void cancelShare(Long userId, Long orderId) {
        if (photoOrderMapper.selectMineById(orderId, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在！");
        }

        photoOrderMapper.cancelShare(orderId, userId);
        log.info("撤销分享：订单 {} 用户 {}", orderId, userId);
    }

    /**
     * 24 字节 = 192位随机 -> Base64 URL 安全编码 32 个字符
     */
    private String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 任何人凭token查看作品
     * 只返回作品图，不返回原图
     */
    public ShareViewVO viewByToken(String token) {
        PhotoOrder order = photoOrderMapper.selectByValidShareToken(token);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分享链接不存在或已过期");
        }

        List<String> images = new ArrayList<>();
        for (PhotoTask task : photoTaskMapper.selectEntitiesByOrderId(order.getId())) {
            if (task.getStatus() == null || task.getStatus() != OrderConstants.TASK_SUCCESS) {
                continue;
            }
            if (task.getImageUrl() == null || task.getImageUrl().isBlank()) {
                continue;
            }

            // 作品图仍要签名，避免被外部访问者拿到裸地址而绕过共享机制永久访问
            images.add(fileSigner.sign(task.getImageUrl()));
        }

        StyleTemplate style = styleTemplateMapper.selectById(order.getStyleId());
        String styleName = (style == null || style.getName() == null) ? DEFAULT_STYLE : style.getName();

        return new ShareViewVO(styleName, images, order.getShareExpireAt());
    }
}
