package com.yumi.cute.task;

import com.yumi.cute.common.RechargeStatus;
import com.yumi.cute.entity.RechargeOrder;
import com.yumi.cute.mapper.RechargeOrderMapper;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关闭超时未支付的充值单。
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class RechargeCleanupTask {
    private final RechargeOrderMapper rechargeOrderMapper;

    @Value("${recharge.unpaid-close-minutes:30}")
    private int closeMinutes;

    @Value("${recharge.close-batch-limit:200}")
    private int batchLimit;

    @Scheduled(cron = "${recharge.close-cron:0 */5 * * * ?}")
    public void closeExpiredUnpaid() {
        List<RechargeOrder> list = rechargeOrderMapper.selectExpiredUnpaid(closeMinutes, batchLimit);

        int closed = 0;
        for (RechargeOrder order : list) {
            if (rechargeOrderMapper.transit(order.getId(), RechargeStatus.UNPAID, RechargeStatus.CLOSED) == 1) {
                closed++;
                log.info("关闭超时未支付充值单 {} （创建于 {}）", order.getId(), order.getCreateTime());
            }
        }

        if (closed > 0) {
            log.info("本轮关闭 {} 个超时充值单", closed);
        }
    }
}
