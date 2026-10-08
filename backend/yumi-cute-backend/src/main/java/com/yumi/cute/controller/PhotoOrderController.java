package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.CreateOrderDTO;
import com.yumi.cute.service.GenerationService;
import com.yumi.cute.service.OrderService;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.vo.OrderListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class PhotoOrderController {

    private final OrderService orderService;
    private final GenerationService generationService;

    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid CreateOrderDTO dto) {
        Long userId = UserContext.getUserId();

        //1. 同步：建订单、建任务、扣算力
        String orderNo = orderService.createOrder(userId, dto);
        //2. 异步：提交后台生成任务，立刻返回
        generationService.generate(orderNo);

        return Result.success(orderNo);
    }

    @GetMapping("/list")
    public Result<List<OrderListVO>> list() {
        return Result.success(orderService.listMyOrders(UserContext.getUserId()));
    }

    @GetMapping("/detail/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(UserContext.getUserId(), id));
    }
}