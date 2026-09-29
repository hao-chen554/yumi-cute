package com.yumi.cute.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAvatarDTO {
    @NotBlank(message = "头像地址不能为空")
    private String avatarUrl;
}
