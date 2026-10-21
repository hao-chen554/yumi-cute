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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class PhotoOrderController {

    private final OrderService orderService;
    private final GenerationService generationService;

    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid CreateOrderDTO dto) {
        Long userId = UserContext.getUserId();
        String orderNo;

        try{
            orderNo = orderService.createOrder(userId, dto);
        }catch (DuplicateKeyException e){
            // 幂等命中，表示当前clientToken已经成功下过单
            String exist = orderService.findOrderNoByClientToken(userId, dto.getClientToken());
            if(exist == null){
                throw e; // 不是幂等键冲突，是别的约束
            }
            log.info("重复提交命中幂等键，返回原订单{}",exist);
            // 原订单已存在，返回原订单号，不重复生成
            return Result.success(exist);
        }

        // 若未命中幂等键，异步新建订单
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