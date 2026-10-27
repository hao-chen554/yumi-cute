package com.yumi.cute.common;

import org.springframework.stereotype.Component;

/**
 * 充值单状态
 * 1、未支付 2、已支付 3、已到账 4、已关闭
 */
public class RechargeStatus {
    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int CREDITED = 2;
    public static final int CLOSED = 3;
}

