package com.yumi.cute.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer points;
    private Integer vipLevel;
    private LocalDateTime createTime;
}