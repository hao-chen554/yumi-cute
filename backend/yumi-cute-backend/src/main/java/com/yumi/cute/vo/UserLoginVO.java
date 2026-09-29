package com.yumi.cute.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private Integer points;
}
