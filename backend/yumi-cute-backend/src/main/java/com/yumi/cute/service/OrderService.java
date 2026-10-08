package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.common.PointsType;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.dto.CreateOrderDTO;
import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.entity.PointsRecord;
import com.yumi.cute.entity.StyleTemplate;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PhotoTaskMapper;
import com.yumi.cute.mapper.PointsRecordMapper;
import com.yumi.cute.mapper.StyleTemplateMapper;
import com.yumi.cute.mapper.UserMapper;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final PhotoOrderMapper photoOrderMapper;
    private final PhotoTaskMapper photoTaskMapper;
    private final StyleTemplateMapper styleTemplateMapper;
    private final UserMapper userMapper;
    private final PointsRecordMapper pointsRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, CreateOrderDTO dto) {

        // ① 风格必须存在且在上架状态
        StyleTemplate style = styleTemplateMapper.selectById(dto.getStyleId());
        if (style == null || style.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "这个风格不存在或已下架");
        }

        // ② 确定张数和消耗
        boolean isTrial = OrderConstants.TYPE_TRIAL.equals(dto.getOrderType());
        if (!isTrial && !OrderConstants.TYPE_FULL.equals(dto.getOrderType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "下单类型不正确");
        }

        int totalCount = isTrial ? OrderConstants.TRIAL_PHOTO_COUNT : OrderConstants.FULL_PHOTO_COUNT;
        // 试看按整套价的 1/4 收费，最少 1 算力
        int cost = isTrial ? Math.max(1, style.getPointsCost() / 4) : style.getPointsCost();

        // ③ 扣算力：一条 SQL 同时完成"校验"和"扣减"，天然防超扣
        int affected = userMapper.deductPoints(userId, cost);
        if (affected == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "算力不足，请先充值");
        }

        // ④ 创建订单
        PhotoOrder order = new PhotoOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPetId(null);
        order.setStyleId(style.getId());
        order.setPersonPhotoUrl(dto.getPersonPhotoUrl());
        order.setPetPhotoUrl(dto.getPetPhotoUrl());
        order.setOrderType(dto.getOrderType());
        order.setTotalCount(totalCount);
        order.setSuccessCount(0);
        order.setPointsCost(cost);
        order.setStatus(OrderConstants.STATUS_WAITING);
        photoOrderMapper.insert(order);

        // ⑤ 创建 N 条生图任务
        List<PhotoTask> tasks = new ArrayList<>();
        for (int i = 1; i <= totalCount; i++) {
            PhotoTask task = new PhotoTask();
            task.setOrderId(order.getId());
            task.setSeqNo(i);
            task.setStatus(OrderConstants.TASK_WAITING);
            tasks.add(task);
        }
        photoTaskMapper.insertBatch(tasks);

        // ⑥ 记一笔算力流水
        User user = userMapper.selectById(userId);
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeAmount(-cost);
        record.setBalanceAfter(user.getPoints());
        record.setType(PointsType.GENERATE_COST);
        record.setBizId(order.getId());
        record.setRemark("生成" + (isTrial ? "试看" : "整套") + "写真：" + style.getName());
        pointsRecordMapper.insert(record);

        return order.getOrderNo();
    }

    /**
     * 生成订单号：YM + 时间 + 4 位随机数
     */
    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(10000);
        return "YM" + time + String.format("%04d", rand);
    }

    /** 我的订单列表 */
    public List<OrderListVO> listMyOrders(Long userId) {
        return photoOrderMapper.selectByUserId(userId);
    }

    /** 订单详情（含每一张图的生成任务） */
    public OrderDetailVO getOrderDetail(Long userId, Long orderId) {

        OrderDetailVO detail = photoOrderMapper.selectDetailById(orderId, userId);
        if (detail == null) {
            // 不存在、或者不属于当前用户，统一提示，不给攻击者任何线索
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }

        detail.setTasks(photoTaskMapper.selectByOrderId(orderId));
        return detail;
    }
}