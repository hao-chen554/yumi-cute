package com.yumi.cute.mapper;

import com.yumi.cute.entity.RechargeOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RechargeOrderMapper {
    @Insert("INSERT INTO recharge_order (recharge_no,user_id,package_id,points,amount_cent,status,client_token)"
            + ("VALUES (#{rechargeNo},#{userId},#{packageId},#{points},#{amountCent},#{status},#{clientToken})"))
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RechargeOrder order);

    @Select("SELECT * FROM recharge_order WHERE recharge_no = #{rechargeNo}")
    RechargeOrder selectByRechargeNo(String RechargeNo);

    @Select("SELECT * FROM recharge_order WHERE user_id = #{userId} AND client_token = #{clientToken}")
    RechargeOrder selectByClientToken(@Param("userId") Long userId, @Param("clientToken") String clientToken);

    /**
     * 状态流转闸门
     * 返回 1 = 将订单状态由from 改为 to
     * 返回 0 = 订单已被修改，不进行操作
     */
    @Update("UPDATE recharge_order SET status = #{to}, pay_time = NOW()" +
            "WHERE recharge_no = #{no} AND status = #{from}")
    int markPaid(@Param("no") String rechargeNo, @Param("from") int from, @Param("to") int to);

    @Update("UPDATE recharge_order SET status = #{to}, finish_time = NOW()" +
            "WHERE recharge_no = #{no} AND status = #{from}")
    int markCredited(@Param("no") String rechargeNo, @Param("from") int from, @Param("to") int to);

    @Select("SELECT * FROM recharge_order WHERE user_id = #{userId} ORDER BY id DESC LIMIT 50")
    List<RechargeOrder> selectByUser(Long userId);

    /**
     * 超时未支付的充值单
     */
    @Select("SELECT * FROM recharge_order WHERE status = 0 AND create_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)" +
    "ORDER BY id ASC LIMIT #{limit}")
    List<RechargeOrder> selectExpiredUnpaid(@Param("minutes") int minutes, @Param("limit") int limit);

    /**
     * 关闭闸门
     */
    @Update("UPDATE recharge_order SET status = #{to} WHERE id = #{id} AND status = #{from}")
    int transit(@Param("id") Long id, @Param("from") int from, @Param("to") int to);
}
