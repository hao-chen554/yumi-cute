package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.OrderConstants;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.mapper.PhotoOrderMapper;
import com.yumi.cute.mapper.PhotoTaskMapper;
import com.yumi.cute.vo.DownloadPlan;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.util.DownloadNaming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class DownloadService {
    private static final String DEFAULT_STYLE = "作品";

    private final PhotoOrderMapper photoOrderMapper;
    private final PhotoTaskMapper photoTaskMapper;

    /**
     * 单张下载
     */
    public DownloadPlan planSingle(Long userId, Long orderId, Long taskId) {
        OrderDetailVO order = requireMyOrder(userId, orderId);

        PhotoTask target = photoTaskMapper.selectEntitiesByOrderId(orderId).stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "图片不存在"));

        // 校验前置：若还没生成成功，下载便失败
        if (target.getStatus() == null || target.getStatus() != OrderConstants.TASK_SUCCESS
                || target.getImageUrl() == null || target.getImageUrl().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "这张图还没生成成功！");
        }

        String fileName = styleName(order) + "-" + DownloadNaming.padSeq(target.getSeqNo())
                + DownloadNaming.extensionOf(target.getImageUrl());

        return new DownloadPlan(fileName, DownloadNaming.contentTypeOf(fileName),
                List.of(new DownloadPlan.Entry(fileName, target.getImageUrl())));
    }

    /**
     * 整套下载
     */
    public DownloadPlan planZip(Long userId, Long orderId){
        OrderDetailVO order = requireMyOrder(userId, orderId);

        String style = styleName(order);
        List<DownloadPlan.Entry> entries = new ArrayList<>();

        for(PhotoTask task : photoTaskMapper.selectEntitiesByOrderId(orderId)){
            // 部分失败也是允许下载的，将成功的那一部分打包
            if(task.getStatus() == null || task.getStatus() != OrderConstants.TASK_SUCCESS){
                continue;
            }
            if(task.getImageUrl() == null || task.getImageUrl().isBlank()){
                continue;
            }
            String name = style + "-" + DownloadNaming.padSeq(task.getSeqNo())
                    + DownloadNaming.extensionOf(task.getImageUrl());
            entries.add(new DownloadPlan.Entry(name, task.getImageUrl()));
        }
        if(entries.isEmpty()){
            throw new BizException(ResultCode.PARAM_ERROR,"这套写真没有可下载的图片");
        }
        String zipName = style + "-" + DownloadNaming.tailOf(order.getOrderNo()) + ".zip";
        return new DownloadPlan(zipName, "application/zip", entries);
    }

    /**
     * 取订单并校验归属权
     */
    private OrderDetailVO requireMyOrder(Long userId, Long orderId) {
        OrderDetailVO order = photoOrderMapper.selectDetailById(orderId, userId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private String styleName(OrderDetailVO order) {
        String name = order.getStyleName();
        return (name == null || name.isBlank()) ? DEFAULT_STYLE : name;
    }

    /**
     * 序号补零、扩展名、压缩包命名都在 {@link DownloadNaming} —— 纯函数已抽出，便于单测
     */
}
