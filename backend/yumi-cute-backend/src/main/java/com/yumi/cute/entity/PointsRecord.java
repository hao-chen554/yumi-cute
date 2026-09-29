package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsRecord {
    private Long id;
    private Long userId;
    private Integer changeAmount;
    private Integer balanceAfter;
    private Integer type;
    private Long bizId = 0L;
    private String remark;
    private LocalDateTime createTime;
}
