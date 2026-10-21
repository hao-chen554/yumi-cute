package com.yumi.cute.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文件，每上传一张图就记录一条，用于追溯：
 * 1、这张图是谁的
 * 2、该在磁盘上留多久
 */

@Data
public class UserFile {
    private Long id;
    private Long userId;
    private String url;
    private Integer bizType;  //1头像 2人物参考图 3宠物参考图 4生成结果 0未区分
    private Long sizeBytes;
    private LocalDateTime expireAt;  //Null = 不过期
    private LocalDateTime deletedAt;
    private LocalDateTime createTime;
}
