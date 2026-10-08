package com.yumi.cute.service;

import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.generator.PhotoGenerator;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PhotoTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationService {

    private final PhotoOrderMapper photoOrderMapper;
    private final PhotoTaskMapper photoTaskMapper;
    private final PhotoGenerator photoGenerator;

    @Async("photoTaskExecutor")
    public void generate(String orderNo) {

        PhotoOrder order = photoOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("异步任务收到不存在的订单：{}", orderNo);
            return;
        }

        List<PhotoTask> tasks = photoTaskMapper.selectEntitiesByOrderId(order.getId());
        int total = tasks.size();
        int success = 0;

        try {
            photoOrderMapper.updateStatus(order.getId(), OrderConstants.STATUS_GENERATING);

            for (PhotoTask task : tasks) {
                photoTaskMapper.updateStatus(task.getId(), OrderConstants.TASK_GENERATING);

                try {
                    String imageUrl = photoGenerator.generate(order, task);
                    photoTaskMapper.markSuccess(task.getId(), OrderConstants.TASK_SUCCESS, imageUrl);
                    success++;
                    log.info("订单 {} 第 {}/{} 张生成成功", orderNo, task.getSeqNo(), total);

                } catch (Exception e) {
                    // 单张失败：只标记这一张，继续做下一张
                    log.error("订单 {} 第 {}/{} 张生成失败", orderNo, task.getSeqNo(), total, e);
                    try {
                        photoTaskMapper.markFailed(task.getId(), OrderConstants.TASK_FAILED, shortMsg(e));
                    } catch (Exception inner) {
                        // 连"写失败状态"都失败了，只能记日志，但绝不能让它带崩整个循环
                        log.error("订单 {} 第 {} 张的失败状态写入失败", orderNo, task.getSeqNo(), inner);
                    }
                }
            }
        } catch (Exception e) {
            log.error("订单 {} 生成过程异常中断", orderNo, e);
        } finally {
            // ★ 这个方法最重要的部分：无论如何，订单都必须走到终态
            try {
                int finalStatus;
                if (success == total) {
                    finalStatus = OrderConstants.STATUS_SUCCESS;
                } else if (success == 0) {
                    finalStatus = OrderConstants.STATUS_FAILED;
                } else {
                    finalStatus = OrderConstants.STATUS_PARTIAL_FAILED;
                }
                photoOrderMapper.finish(order.getId(), finalStatus, success);
                log.info("订单 {} 生成结束，成功 {}/{}", orderNo, success, total);
            } catch (Exception e) {
                log.error("订单 {} 最终状态写入失败", orderNo, e);
            }
        }
    }

    private String shortMsg(Exception e) {
        String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}