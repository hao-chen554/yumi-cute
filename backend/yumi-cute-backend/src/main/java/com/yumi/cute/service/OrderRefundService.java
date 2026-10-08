package com.yumi.cute.service;

import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.common.PointsType;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PointsRecord;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PointsRecordMapper;
import com.yumi.cute.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单退款（目前只用于超时补偿）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundService {

    private final PhotoOrderMapper photoOrderMapper;
    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;

    /**
     * 把卡住的订单判定为失败并退还算力。
     *
     * @return true = 这次真的退了；false = 已经被处理过，什么都没做
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean refundStuckOrder(PhotoOrder order) {

        // ① 幂等闸门：只有还在中间态，才轮到我处理
        int affected = photoOrderMapper.markFailedIfStuck(order.getId(), OrderConstants.STATUS_FAILED);
        if (affected == 0) {
            return false;   // 已经被处理过了，直接返回
        }

        // ② 退算力
        userMapper.addPoints(order.getUserId(), order.getPointsCost());

        // ③ 记流水（和扣费一样，每一笔钱的变动都要留痕）
        User user = userMapper.selectById(order.getUserId());

        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeAmount(order.getPointsCost());     // 正数 = 增加
        record.setBalanceAfter(user.getPoints());
        record.setType(PointsType.REFUND);
        record.setBizId(order.getId());
        record.setRemark("订单超时未完成，自动退还算力：" + order.getOrderNo());
        pointsRecordMapper.insert(record);

        return true;
    }
}