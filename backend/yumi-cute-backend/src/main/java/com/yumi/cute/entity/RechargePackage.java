package com.yumi.cute.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
public class RechargePackage {
    private Integer id;
    private String name;
    private Integer points; // 基础算力
    private Integer bonusPoints; // 赠送算力
    private Integer amountCent; // 售价（分）
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}
