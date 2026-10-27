package com.yumi.cute.vo;

import java.util.List;

/**
 * 一次下载的【计划】：叫什么名字、包含哪些文件
 */
public record DownloadPlan(String fileName, String contentType, List<Entry> entries) {
    /**
     * 压缩包里的一个条目
     */
    public record Entry(String name, String url){}
}
