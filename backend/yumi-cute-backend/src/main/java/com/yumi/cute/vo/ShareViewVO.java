package com.yumi.cute.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 给外部分享页看的数据
 */
public record ShareViewVO(String styleName, List<String> images, LocalDateTime expireAt) {
}
