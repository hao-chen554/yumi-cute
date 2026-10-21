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
    private final FileService fileService;

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

        creditAndRecord(order, order.getPointsCost(), "订单超时未完成，自动返还算力：" + order.getOrderNo());

        return true;
    }

    /**
     * 写订单终态并返还算力
     */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeOrder(PhotoOrder order, int finalStatus, int successCount) {
        // 若登记清理时间失败只记录日志不影响订单终态，后续文件会被【孤儿规则】清理
        markReferenceExpireQuietly(order.getPersonPhotoUrl());
        markReferenceExpireQuietly(order.getPetPhotoUrl());

        //1、抢终态的写权限
        if (photoOrderMapper.finish(order.getId(), finalStatus, successCount) == 0) {
            log.error("订单{}已被其他流程处理，跳过终态写入与退款", order.getOrderNo());
            return;
        }

        //2、按结果算退款
        int refund = calcRefund(order, finalStatus, successCount);
        if (refund <= 0) {
            log.info("订单{}终态{}，成功{}/{}，无需退款", order.getOrderNo(), finalStatus, successCount, order.getTotalCount());
            return;
        }

        //3、退钱＋记流水
        creditAndRecord(order, refund, "生成失败退还算力（成功" + successCount + "/" + order.getTotalCount() + ") : " + order.getOrderNo());
        log.info("订单{}终态{}，成功{}/{}，退还{}算力", order.getOrderNo(), finalStatus, successCount, order.getTotalCount(), refund);
    }

    private void markReferenceExpireQuietly(String url) {
        try {
            fileService.markReferenceExpire(url);
        } catch (Exception e) {
            log.error("登记参考图清理时间失败：{}", url, e);
        }
    }

    /**
     * 退款规则：
     * 全部失败 -> 全额退
     * 部分失败 -> 按失败张数比例退（整数除法向下取整，永远不超退）
     * 成功 -> 不退
     */
    private int calcRefund(PhotoOrder order, int finalStatus, int successCount) {
        int cost = order.getPointsCost() == null ? 0 : order.getPointsCost();
        int total = (order.getTotalCount() == null || order.getTotalCount() <= 0) ? 1 : order.getTotalCount();

        if (finalStatus == OrderConstants.STATUS_FAILED || finalStatus == OrderConstants.STATUS_CANCELED) {
            return cost;
        }

        if (finalStatus == OrderConstants.STATUS_PARTIAL_FAILED) {
            return cost * (total - successCount) / total;
        }

        return 0;
    }

    /**
     * 加算力 + 记一笔流水，所有退款走此接口
     */
    private void creditAndRecord(PhotoOrder order, int refund, String remark) {
        userMapper.addPoints(order.getUserId(), refund);

        User user = userMapper.selectById(order.getUserId());
        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeAmount(refund);
        record.setBalanceAfter(user.getPoints());
        record.setType(PointsType.REFUND);
        record.setBizId(order.getId());
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }
}