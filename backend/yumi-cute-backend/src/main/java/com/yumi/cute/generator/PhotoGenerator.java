package com.yumi.cute.generator;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;

/**
 * 图片生成器接口。
 * 现在用 Mock 实现，将来接真实 AI 时换成 AiPhotoGenerator 即可，业务代码不用改。
 */
public interface PhotoGenerator {

    /**
     * 生成一张图，返回图片地址；生成失败抛异常
     */
    String generate(PhotoOrder order, PhotoTask task);
}