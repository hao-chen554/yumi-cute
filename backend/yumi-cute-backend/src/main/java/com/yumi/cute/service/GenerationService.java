package com.yumi.cute.service;

import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.entity.StyleTemplate;
import com.yumi.cute.generator.PhotoGenerator;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PhotoTaskMapper;
import com.yumi.cute.mapper.StyleTemplateMapper;
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
    private final StyleTemplateMapper styleMapper;

    @Async("photoTaskExecutor")
    public void generate(String orderNo) {

        PhotoOrder order = photoOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("异步任务收到不存在的订单：{}", orderNo);
            return;
        }

        StyleTemplate style = styleMapper.selectById(order.getStyleId());
        if (style == null) {
            log.error("订单{}的风格{}不存在，无法生成", orderNo, order.getStyleId());
            photoOrderMapper.finish(order.getId(), OrderConstants.STATUS_FAILED, 0);
            return;
        }

        List<PhotoTask> tasks = photoTaskMapper.selectEntitiesByOrderId(order.getId());

        photoOrderMapper.updateStatus(order.getId(), OrderConstants.STATUS_GENERATING);
        for (PhotoTask task : tasks) {
            photoTaskMapper.updateStatus(task.getId(), OrderConstants.TASK_GENERATING);
        }

        // ★ 核心变化：只调用一次生成器，拿回这一套的全部图片
        List<String> urls = List.of();
        String failReason = "模型没有返回这一张";
        try {
            List<String> result = photoGenerator.generate(order, style);
            if (result != null) {
                urls = result;
            }
        } catch (Exception e) {
            failReason = shortMsg(e);
            log.error("订单 {} 生成失败", orderNo, e);
        }

        // 按顺序把结果分派给每一条任务：
        // 第 i 个 url 给第 i 条 task；url 不够的任务标记失败
        int success = 0;
        for (int i = 0; i < tasks.size(); i++) {
            PhotoTask task = tasks.get(i);
            String url = i < urls.size() ? urls.get(i) : null;

            try {
                if (url != null && !url.isBlank()) {
                    photoTaskMapper.markSuccess(task.getId(), OrderConstants.TASK_SUCCESS, url);
                    success++;
                } else {
                    photoTaskMapper.markFailed(task.getId(), OrderConstants.TASK_FAILED, failReason);
                }
            } catch (Exception e) {
                // 一条任务写失败，不能带崩整个循环
                log.error("订单 {} 第 {} 张的状态写入失败", orderNo, task.getSeqNo(), e);
            }
        }

        // 订单终态
        try {
            int finalStatus;
            if (success == 0) {
                finalStatus = OrderConstants.STATUS_FAILED;
            } else if (success == tasks.size()) {
                finalStatus = OrderConstants.STATUS_SUCCESS;
            } else {
                finalStatus = OrderConstants.STATUS_PARTIAL_FAILED;
            }
            photoOrderMapper.finish(order.getId(), finalStatus, success);
            log.info("订单 {} 生成结束，成功 {}/{}", orderNo, success, tasks.size());

        } catch (Exception e) {
            log.error("订单 {} 最终状态写入失败", orderNo, e);
        }
    }

    private String shortMsg(Exception e) {
        String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}