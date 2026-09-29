package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StyleTemplate {
    private Long id;
    private String name;
    private String coverUrl;
    private String description;
    private String prompt;
    private Integer pointsCost;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}