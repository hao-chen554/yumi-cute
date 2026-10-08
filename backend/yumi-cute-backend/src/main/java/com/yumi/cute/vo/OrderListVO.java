package com.yumi.cute.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderListVO {
    private Long id;
    private String orderNo;
    private String styleName;
    private Integer orderType;
    private Integer totalCount;
    private Integer successCount;
    private Integer pointsCost;
    private Integer status;
    private LocalDateTime createTime;
}