package com.yumi.cute.vo;

import java.time.LocalDateTime;

/**
 * 给订单所有者看的分享信息
 */
public record ShareInfoVO(String token, LocalDateTime expireAt) {
}
