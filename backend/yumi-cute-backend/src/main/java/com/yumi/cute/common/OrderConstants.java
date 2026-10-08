package com.yumi.cute.common;

public class OrderConstants {

    /** 订单类型 */
    public static final Integer TYPE_TRIAL = 1;   // 试看单
    public static final Integer TYPE_FULL = 2;    // 整套单

    /** 订单状态 */
    public static final Integer STATUS_WAITING = 0;         // 待处理
    public static final Integer STATUS_GENERATING = 1;      // 生成中
    public static final Integer STATUS_SUCCESS = 2;         // 已完成
    public static final Integer STATUS_PARTIAL_FAILED = 3;  // 部分失败
    public static final Integer STATUS_FAILED = 4;          // 全部失败
    public static final Integer STATUS_CANCELED = 5;        // 已取消

    /** 生图任务状态 */
    public static final Integer TASK_WAITING = 0;
    public static final Integer TASK_GENERATING = 1;
    public static final Integer TASK_SUCCESS = 2;
    public static final Integer TASK_FAILED = 3;

    /** 一套写真生成几张 */
    public static final int FULL_PHOTO_COUNT = 6;
    public static final int TRIAL_PHOTO_COUNT = 1;
}