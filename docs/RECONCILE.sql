-- ============================================================
--  YuMi-cute 对账脚本
--
--  用法：mysql -uroot -p --default-character-set=utf8mb4 yumi_cute < docs/RECONCILE.sql
--  什么时候跑：每次改动与「钱」相关的代码之后
--
--  判定标准：标着「必须为空」「必须为 0」的结果不满足，就是有 bug。
-- ============================================================

-- ------------------------------------------------------------
--  ① 账实一致：每个用户的余额 vs 流水合计
--     差额必须为 0。非 0 说明有人绕过流水直接改了 user.points。
-- ------------------------------------------------------------
SELECT u.id, u.username,
       u.points                                   AS 账面余额,
       IFNULL(SUM(p.change_amount), 0)            AS 流水合计,
       u.points - IFNULL(SUM(p.change_amount), 0) AS 差额
FROM user u
LEFT JOIN points_record p ON p.user_id = u.id
GROUP BY u.id, u.username, u.points
HAVING 差额 <> 0
ORDER BY ABS(差额) DESC;

-- ------------------------------------------------------------
--  ② 同一订单被重复扣费（必须为空）
-- ------------------------------------------------------------
SELECT biz_id AS 订单ID, COUNT(*) AS 扣费条数
FROM points_record
WHERE type = 2
GROUP BY biz_id
HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
--  ③ 同一订单被重复退款（必须为空）
-- ------------------------------------------------------------
SELECT biz_id AS 订单ID, COUNT(*) AS 退款条数
FROM points_record
WHERE type = 4
GROUP BY biz_id
HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
--  ④ 同一充值单被重复到账（必须为空）
-- ------------------------------------------------------------
SELECT biz_id AS 充值单ID, COUNT(*) AS 到账条数
FROM points_record
WHERE type = 3
GROUP BY biz_id
HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
--  ⑤ 失败却没有退款的订单（历史遗留应逐渐清空）
--     应退 = 全部失败(4) 退全额；部分失败(3) 按失败张数比例退；成功(2) 不退
-- ------------------------------------------------------------
SELECT o.id, o.order_no, o.status, o.success_count, o.total_count, o.points_cost,
       IFNULL(SUM(p.change_amount), 0) AS 实际退款,
       CASE o.status
         WHEN 4 THEN o.points_cost
         WHEN 3 THEN o.points_cost * (o.total_count - o.success_count) DIV o.total_count
         ELSE 0
       END AS 应退
FROM photo_order o
LEFT JOIN points_record p ON p.biz_id = o.id AND p.type = 4
WHERE o.status IN (2, 3, 4) AND o.create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY o.id, o.order_no, o.status, o.success_count, o.total_count, o.points_cost
HAVING 实际退款 <> 应退;

-- ------------------------------------------------------------
--  ⑥ 卡在中间态的订单（应该有，但必须是「刚下的」）
--     超过 15 分钟还停在这里，说明补偿任务没在工作
-- ------------------------------------------------------------
SELECT id, order_no, status, create_time,
       TIMESTAMPDIFF(MINUTE, create_time, NOW()) AS 已卡分钟数
FROM photo_order
WHERE status IN (0, 1)
ORDER BY create_time ASC;

-- ------------------------------------------------------------
--  ⑦ 已支付但未到账的充值单（需要重放 pay 补单）
--     正常情况下必须为空
-- ------------------------------------------------------------
SELECT id, recharge_no, user_id, points, amount_cent, status, pay_time
FROM recharge_order
WHERE status = 1
ORDER BY pay_time ASC;

-- ------------------------------------------------------------
--  ⑧ 超时未支付的充值单（超过 30 分钟还 status=0，说明关闭任务没工作）
-- ------------------------------------------------------------
SELECT id, recharge_no, user_id, amount_cent, create_time,
       TIMESTAMPDIFF(MINUTE, create_time, NOW()) AS 已过分钟数
FROM recharge_order
WHERE status = 0 AND create_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
ORDER BY create_time ASC;

-- ------------------------------------------------------------
--  ⑨ 流水按类型汇总（核对总量用）
-- ------------------------------------------------------------
SELECT type,
       CASE type WHEN 1 THEN '注册赠送' WHEN 2 THEN '生成消费'
                 WHEN 3 THEN '充值' WHEN 4 THEN '退款'
                 WHEN 5 THEN '对账调整' ELSE '未知' END AS 类型名,
       COUNT(*) AS 条数,
       SUM(change_amount) AS 合计
FROM points_record
GROUP BY type
ORDER BY type;