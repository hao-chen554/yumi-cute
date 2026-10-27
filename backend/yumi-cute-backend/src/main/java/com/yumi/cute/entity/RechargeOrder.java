package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RechargeOrder {
    private Long id;
    private String rechargeNo;
    private Long userId;
    private Integer packageId;
    private Integer points; // 下单时快照
    private Integer amountCent; // 下单时快照（分）
    private Integer status;
    private String clientToken;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
}
