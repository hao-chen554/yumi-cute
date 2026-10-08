package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhotoTask {
    private Long id;
    private Long orderId;
    private Integer seqNo;
    private Integer status;
    private String imageUrl;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}