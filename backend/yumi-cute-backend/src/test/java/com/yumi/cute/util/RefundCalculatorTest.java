package com.yumi.cute.util;

import com.yumi.cute.common.OrderConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退款金额计算 —— 全项目最不能算错的一段逻辑。
 *
 * <p>覆盖点：三种终态的退款、向下取整「永远退不超」、以及 null / 除零的容错。
 */
class RefundCalculatorTest {

    private static final int COST = 30;
    private static final int TOTAL = 6;

    @Test
    @DisplayName("全部失败 → 全额退")
    void allFailed_refundsEverything() {
        assertEquals(COST, RefundCalculator.calc(OrderConstants.STATUS_FAILED, COST, TOTAL, 0));
    }

    @Test
    @DisplayName("用户取消 → 全额退")
    void canceled_refundsEverything() {
        assertEquals(COST, RefundCalculator.calc(OrderConstants.STATUS_CANCELED, COST, TOTAL, 0));
    }

    @Test
    @DisplayName("全部成功 → 一分不退")
    void allSucceeded_refundsNothing() {
        assertEquals(0, RefundCalculator.calc(OrderConstants.STATUS_SUCCESS, COST, TOTAL, TOTAL));
    }

    @Test
    @DisplayName("部分失败 1/6 成功 → 退 5/6")
    void partial_oneOfSix() {
        // 30 * 5 / 6 = 25
        assertEquals(25, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, 30, 6, 1));
    }

    @Test
    @DisplayName("部分失败 3/6 成功 → 正好退一半")
    void partial_half() {
        assertEquals(15, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, 30, 6, 3));
    }

    @Test
    @DisplayName("向下取整：25 算力 5/6 成功 → 退 4 而不是 4.16")
    void partial_roundsDown() {
        // 25 * 1 / 6 = 4.166… → int 除法得 4
        assertEquals(4, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, 25, 6, 5));
    }

    @Test
    @DisplayName("任何输入下退款都不超过原价（钱不能多退）")
    void refundNeverExceedsCost() {
        for (int total = 1; total <= 10; total++) {
            for (int success = 0; success <= total; success++) {
                for (int cost : new int[] { 0, 1, 7, 25, 30, 99 }) {
                    int refund = RefundCalculator.calc(
                            OrderConstants.STATUS_PARTIAL_FAILED, cost, total, success);
                    assertTrue(refund >= 0, "退款不能为负");
                    assertTrue(refund <= cost, "退款不能超过原价：cost=" + cost + " refund=" + refund);
                }
            }
        }
    }

    @Test
    @DisplayName("部分失败但成功数为 0 → 等价全额退")
    void partialWithZeroSuccess_refundsEverything() {
        assertEquals(COST, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, COST, TOTAL, 0));
    }

    @Test
    @DisplayName("总张数为 null 或 0 → 不抛除零异常")
    void nullOrZeroTotal_doesNotDivideByZero() {
        assertEquals(COST, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, COST, null, 0));
        assertEquals(COST, RefundCalculator.calc(OrderConstants.STATUS_PARTIAL_FAILED, COST, 0, 0));
    }

    @Test
    @DisplayName("算力为 null → 退 0，不抛 NPE")
    void nullCost_returnsZero() {
        assertEquals(0, RefundCalculator.calc(OrderConstants.STATUS_FAILED, null, TOTAL, 0));
    }

    @Test
    @DisplayName("待生成 / 生成中这类非终态 → 不退")
    void nonFinalStatus_refundsNothing() {
        assertEquals(0, RefundCalculator.calc(OrderConstants.STATUS_WAITING, COST, TOTAL, 0));
        assertEquals(0, RefundCalculator.calc(OrderConstants.STATUS_GENERATING, COST, TOTAL, 0));
    }
}
