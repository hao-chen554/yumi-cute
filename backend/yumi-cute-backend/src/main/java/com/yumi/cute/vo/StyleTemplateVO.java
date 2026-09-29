package com.yumi.cute.vo;

import lombok.Data;

@Data
public class StyleTemplateVO {
    private Long id;
    private String name;
    private String coverUrl;
    private String description;
    private Integer pointsCost;
}