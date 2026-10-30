package com.yumi.cute.util;

import com.yumi.cute.common.OrderConstants;

/**
 * 退款金额计算。
 *
 * <p>刻意抽成无状态工具类、而不是留在 Service 的私有方法里 —— 因为这是全项目
 * 最不能算错的一段逻辑，而私有方法没法直接写单测。抽出来之后
 * {@code RefundCalculatorTest} 可以覆盖全部终态组合。
 */
public final class RefundCalculator {

    private RefundCalculator() {
    }

    /**
     * 规则：
     * <ul>
     *   <li>全部失败 / 用户取消 → 全额退</li>
     *   <li>部分失败 → 按失败张数比例退，整数除法向下取整（永远退不超）</li>
     *   <li>成功 / 其它 → 不退</li>
     * </ul>
     *
     * @param finalStatus  订单终态
     * @param cost         订单消耗的算力（可为 null，视为 0）
     * @param total        总张数（null 或 &lt;= 0 时按 1 处理，避免除零）
     * @param successCount 成功张数
     * @return 应退算力，永远 &gt;= 0 且 &lt;= cost
     */
    public static int calc(int finalStatus, Integer cost, Integer total, int successCount) {
        int c = cost == null ? 0 : cost;
        int t = (total == null || total <= 0) ? 1 : total;

        if (finalStatus == OrderConstants.STATUS_FAILED
                || finalStatus == OrderConstants.STATUS_CANCELED) {
            return c;
        }

        if (finalStatus == OrderConstants.STATUS_PARTIAL_FAILED) {
            // 先乘后除，且用 int 除法天然向下取整 —— 保证「退不超」
            return c * (t - successCount) / t;
        }

        return 0;
    }
}
