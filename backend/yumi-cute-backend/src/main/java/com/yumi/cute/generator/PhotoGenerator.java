package com.yumi.cute.generator;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.StyleTemplate;

import java.util.List;

/**
 * 图片生成器接口。
 * 现在两个实现：MockPhotoGenerator（模拟）和 AiPhotoGenerator（真AI）
 * 用哪个由配置ai.provider决定
 */
public interface PhotoGenerator {

    /**
     * 一次性生成整套写真的所有图片。
     *
     * 数量由 order.getTotalCount() 决定（试看单 1 张、整套单 6 张）。
     *
     *
     * @return 成功生成的图片地址，按顺序对应第 1..N 张。
     *         可能少于 totalCount（模型少给了几张），剩下的会被标成失败。
     *         全部失败时抛异常。
     */
    List<String> generate(PhotoOrder order, StyleTemplate style);
}