package com.yumi.cute.task;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.service.OrderRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensationTask {

    /** 超过多少分钟还没跑完，就判定为卡住 */
    private static final int STUCK_MINUTES = 10;

    private final PhotoOrderMapper photoOrderMapper;
    private final OrderRefundService orderRefundService;

    /**
     * fixedDelay：上一次执行【结束】之后，再等 60 秒执行下一次。
     * 对比 fixedRate：不管上次有没有跑完，每 60 秒都触发一次。
     * 补偿任务必须用 fixedDelay —— 避免自己的两次执行重叠。
     */
    @Scheduled(fixedDelay = 60_000)
    public void compensate() {

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(STUCK_MINUTES);
        List<PhotoOrder> stuckOrders = photoOrderMapper.selectStuckOrders(deadline);

        if (stuckOrders.isEmpty()) {
            return;
        }

        log.warn("发现 {} 个超时未完成的订单，开始补偿", stuckOrders.size());

        for (PhotoOrder order : stuckOrders) {
            try {
                boolean refunded = orderRefundService.refundStuckOrder(order);
                if (refunded) {
                    log.info("订单 {} 已判定失败，退还 {} 算力", order.getOrderNo(), order.getPointsCost());
                } else {
                    log.info("订单 {} 已被其他流程处理，跳过", order.getOrderNo());
                }
            } catch (Exception e) {
                // 一个订单处理失败，不能影响后面的订单
                log.error("补偿订单 {} 时出错", order.getOrderNo(), e);
            }
        }
    }
}