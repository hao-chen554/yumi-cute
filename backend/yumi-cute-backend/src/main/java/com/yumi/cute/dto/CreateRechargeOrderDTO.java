package com.yumi.cute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRechargeOrderDTO {
    @NotBlank(message = "缺少请求标识，请刷新页面重试")
    private String clientToken;

    @NotNull(message = "请选择充值套餐")
    private Integer packageId;
}
