package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.CreateOrderDTO;
import com.yumi.cute.service.DownloadService;
import com.yumi.cute.service.GenerationService;
import com.yumi.cute.service.OrderService;
import com.yumi.cute.service.ShareService;
import com.yumi.cute.storage.FileStorage;
import com.yumi.cute.util.DownloadWriter;
import com.yumi.cute.vo.DownloadPlan;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.vo.OrderListVO;
import com.yumi.cute.vo.ShareInfoVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class PhotoOrderController {

    private final OrderService orderService;
    private final GenerationService generationService;
    private final DownloadService downloadService;
    private final FileStorage fileStorage;
    private final ShareService shareService;

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

    /** 整套下载（zip） */
    @GetMapping("/{id}/download")
    public void downloadZip(@PathVariable Long id, HttpServletResponse response)
            throws IOException {
        DownloadPlan plan = downloadService.planZip(UserContext.getUserId(), id);
        DownloadWriter.writeZip(response, fileStorage, plan);
    }

    /** 单张下载 */
    @GetMapping("/{id}/download/{taskId}")
    public void downloadOne(@PathVariable Long id,
                            @PathVariable Long taskId,
                            HttpServletResponse response) throws IOException {
        DownloadPlan plan = downloadService.planSingle(UserContext.getUserId(), id, taskId);
        DownloadWriter.writeSingle(response, fileStorage, plan);
    }

    /**
     * 生成分享链接（覆盖旧链接）
     */
    @PostMapping("/{id}/share")
    public Result<ShareInfoVO> createShare(@PathVariable Long id){
        return Result.success(shareService.createShare(UserContext.getUserId(), id));
    }

    /**
     * 查询当前分享状态，没有则 data 为 null
     */
    @GetMapping("/{id}/share")
    public Result<ShareInfoVO> getShare(@PathVariable Long id){
        return Result.success(shareService.getShare(UserContext.getUserId(), id));
    }

    /**
     * 撤销分享
     */
    @DeleteMapping("/{id}/share")
    public Result<Void> cancelShare(@PathVariable Long id){
        shareService.cancelShare(UserContext.getUserId(), id);
        return Result.success();
    }

    /**
     * 删除订单（软删）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        orderService.deleteOrder(UserContext.getUserId(), id);
        return Result.success();
    }
}