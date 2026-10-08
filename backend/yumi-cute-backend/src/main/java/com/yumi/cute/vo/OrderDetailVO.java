package com.yumi.cute.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {
    private Long id;
    private String orderNo;
    private String styleName;
    private String personPhotoUrl;
    private String petPhotoUrl;
    private Integer orderType;
    private Integer totalCount;
    private Integer successCount;
    private Integer pointsCost;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private List<PhotoTaskVO> tasks;
}