package com.yumi.cute.task;

import com.yumi.cute.entity.UserFile;
import com.yumi.cute.mapper.UserFileMapper;
import com.yumi.cute.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件生命周期清理
 * 1、到期的参考图
 * 2、孤儿文件
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FileCleanupTask {
    private final UserFileMapper userFileMapper;
    private final FileStorage fileStorage;

    @Value("${file.lifecycle.batch-limit:200}")
    private int batchlimit;

    @Value("${file.lifecycle.orphan-ttl-days:30}")
    private int orphanTtlDays;

    @Scheduled(cron = "${file.lifecycle.cleanup-cron:0 0 3 * * ?}")
    public void cleanup() {
        int expired = purgeExpired();
        int orphans = purgeOrphans();
        if (expired + orphans > 0) {
            log.info("文件清理完成：到期{}个，孤儿{}个", expired, orphans);
        }
    }

    private int purgeExpired() {
        return deleteEach(userFileMapper.selectExpired(batchlimit), "到期");
    }

    private int purgeOrphans() {
        return deleteEach(userFileMapper.selectOrphans(orphanTtlDays, batchlimit), "孤儿");
    }

    /**
     * 先删磁盘再标记
     */
    private int deleteEach(List<UserFile> userFiles, String kind) {
        int done = 0;
        for(UserFile file : userFiles){
            try{
                fileStorage.delete(file.getUrl()); // 先删磁盘
                userFileMapper.markDeleted(file.getId()); // 标记已删除
                done++;
                log.info("已删除{}文件：{}",kind,file.getUrl());
            }catch (Exception e){
                // 不标记delete_at，下一轮继续被捞出重试
                log.error("删除{}文件失败：{}",kind,file.getUrl(),e);
            }
        }
        return done;
    }
}
