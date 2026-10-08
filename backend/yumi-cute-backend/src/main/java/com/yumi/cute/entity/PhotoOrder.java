package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long petId;
    private Long styleId;
    private String personPhotoUrl;
    private String petPhotoUrl;
    private Integer orderType;
    private Integer totalCount;
    private Integer successCount;
    private Integer pointsCost;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}