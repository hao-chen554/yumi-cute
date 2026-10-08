package com.yumi.cute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderDTO {

    @NotNull(message = "请选择风格")
    private Long styleId;

    @NotBlank(message = "请上传本人照片")
    private String personPhotoUrl;

    @NotBlank(message = "请上传宠物照片")
    private String petPhotoUrl;

    @NotNull(message = "请选择下单类型")
    private Integer orderType;
}