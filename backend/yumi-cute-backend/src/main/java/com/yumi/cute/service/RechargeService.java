package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.PointsType;
import com.yumi.cute.common.RechargeStatus;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.dto.CreateRechargeOrderDTO;
import com.yumi.cute.entity.PointsRecord;
import com.yumi.cute.entity.RechargeOrder;
import com.yumi.cute.entity.RechargePackage;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.PointsRecordMapper;
import com.yumi.cute.mapper.RechargeOrderMapper;
import com.yumi.cute.mapper.RechargePackageMapper;
import com.yumi.cute.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class RechargeService {
    private final RechargePackageMapper rechargePackageMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;

    public List<RechargePackage> listPackage() {
        return rechargePackageMapper.selectOnSale();
    }

    /**
     * 创建充值单
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, CreateRechargeOrderDTO rechargeOrderDTO) {
        // 先判断购买的套餐状态是否正常
        RechargePackage pck = rechargePackageMapper.selectById(rechargeOrderDTO.getPackageId());
        if (pck == null || pck.getStatus() == null || pck.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "套餐不存在或已下架");
        }

        // 组装订单
        RechargeOrder rechargeOrder = new RechargeOrder();
        rechargeOrder.setUserId(userId);
        rechargeOrder.setPackageId(rechargeOrderDTO.getPackageId());
        rechargeOrder.setRechargeNo(generateRechargeNo());
        // 金额和算力快照，避免因套餐变动影响本单
        rechargeOrder.setPoints(pck.getPoints() + pck.getBonusPoints());
        rechargeOrder.setAmountCent(pck.getAmountCent());
        rechargeOrder.setStatus(RechargeStatus.UNPAID);
        rechargeOrder.setClientToken(rechargeOrderDTO.getClientToken());

        rechargeOrderMapper.insert(rechargeOrder);
        log.info("创建充值单 {} 用户 {} 套餐 {} 金额 {}分 到账 {}算力", rechargeOrder.getRechargeNo(), userId, pck.getName(), pck.getAmountCent(), rechargeOrder.getPoints());
        return rechargeOrder.getRechargeNo();
    }

    private String generateRechargeNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(10000);
        return "RC" + time + String.format("%04d", rand);
    }

    /**
     * 支付成功（模拟收银台回调）
     *
     * @return 本次到账的算力；0 表示此单已被处理，不进行任何操作
     */
    @Transactional(rollbackFor = Exception.class)
    public int pay(Long userId, String rechargeNo) {
        RechargeOrder order = rechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND, "充值订单不存在");
        }

        if (order.getStatus() == RechargeStatus.CLOSED) {
            throw new BizException(ResultCode.PARAM_ERROR, "充值订单已关闭");
        }

        // 1、状态机闸门：0 -> 1，抢到才处理
        if (rechargeOrderMapper.markPaid(rechargeNo, RechargeStatus.UNPAID, RechargeStatus.PAID) == 0) {
            log.info("充值单 {} 已处理过，重复回调忽略", rechargeNo);
            return 0;
        }

        // 2、加算力+记流水
        userMapper.addPoints(order.getUserId(), order.getPoints());

        User user = userMapper.selectById(order.getUserId());
        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeAmount(order.getPoints());
        record.setBalanceAfter(user.getPoints());
        record.setType(PointsType.RECHARGE);
        record.setBizId(order.getId());
        record.setRemark("充值" + order.getPoints() + "算力: " + rechargeNo);
        pointsRecordMapper.insert(record);

        // 3、订单状态改为已到账
        rechargeOrderMapper.markCredited(rechargeNo, RechargeStatus.PAID, RechargeStatus.CREDITED);
        log.info("充值到账：{} 用户 {} + {}算力（余额 {}）", rechargeNo, order.getUserId(), order.getPoints(), user.getPoints());
        return order.getPoints();
    }

    public List<RechargeOrder> listMyRecharge(Long userId){
        return rechargeOrderMapper.selectByUser(userId);
    }

    // 幂等键回查（Controller 捕获唯一键冲突后用）
    public String findRechargeNoByClientToken(Long userId, String clientToken) {
        RechargeOrder order = rechargeOrderMapper.selectByClientToken(userId, clientToken);
        return order == null ? null : order.getRechargeNo();
    }
}
