package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.CreateRechargeOrderDTO;
import com.yumi.cute.entity.RechargeOrder;
import com.yumi.cute.entity.RechargePackage;
import com.yumi.cute.service.RechargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/recharge")
@RequiredArgsConstructor
public class RechargeController {
    private final RechargeService rechargeService;

    /**
     * 获取套餐列表
     */
    @GetMapping("/packages")
    public Result<List<RechargePackage>> packages(){
        return Result.success(rechargeService.listPackage());
    }

    /**
     * 创建充值单
     */
    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid CreateRechargeOrderDTO dto){
        Long userId = UserContext.getUserId();
        String rechargeNo;
        try{
            rechargeNo = rechargeService.createOrder(userId, dto);
        }catch (DuplicateKeyException e){
            String exist = rechargeService.findRechargeNoByClientToken(userId, dto.getClientToken());
            if(exist == null){
                throw e; // 撞得不是幂等键
            }
            log.info("重复提交命中充值幂等键，返回原单号 {}", exist);
            return Result.success(exist);
        }
        return Result.success(rechargeNo);
    }

    /**
     * 模拟支付回调，返回本次到账算力
     */
    @PostMapping("/{rechargeNo}/pay")
    public Result<Integer> pay(@PathVariable String rechargeNo){
        return Result.success(rechargeService.pay(UserContext.getUserId(), rechargeNo));
    }

    /**
     * 我的充值记录
     */
    @GetMapping("/orders")
    public Result<List<RechargeOrder>> orders(){
        return Result.success(rechargeService.listMyRecharge(UserContext.getUserId()));
    }
}
