package com.yumi.cute.vo;

import lombok.Data;

@Data
public class PhotoTaskVO {
    private Integer seqNo;
    private Integer status;
    private String imageUrl;
    private String errorMsg;
}