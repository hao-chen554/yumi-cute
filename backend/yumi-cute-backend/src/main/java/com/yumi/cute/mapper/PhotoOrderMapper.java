package com.yumi.cute.mapper;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.vo.OrderListVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PhotoOrderMapper {

    @Insert("INSERT INTO photo_order (order_no, user_id, pet_id, style_id, person_photo_url, pet_photo_url, " +
            "order_type, total_count, success_count, points_cost, status) " +
            "VALUES (#{orderNo}, #{userId}, #{petId}, #{styleId}, #{personPhotoUrl}, #{petPhotoUrl}, " +
            "#{orderType}, #{totalCount}, #{successCount}, #{pointsCost}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PhotoOrder order);

    /**
     * 查某个用户的订单列表
     * 用 LEFT JOIN 把风格名一起查出来，避免"先查订单、再逐个查风格"的 N+1 问题
     */
    @Select("SELECT o.id, o.order_no, o.order_type, o.total_count, o.success_count, " +
            "       o.points_cost, o.status, o.create_time, s.name AS style_name " +
            "FROM photo_order o " +
            "LEFT JOIN style_template s ON s.id = o.style_id " +
            "WHERE o.user_id = #{userId} " +
            "ORDER BY o.id DESC")
    List<OrderListVO> selectByUserId(Long userId);

    /**
     * 查订单详情
     * 注意 WHERE 里的 user_id —— 防水平越权，只能查自己的订单
     */
    @Select("SELECT o.*, s.name AS style_name " +
            "FROM photo_order o " +
            "LEFT JOIN style_template s ON s.id = o.style_id " +
            "WHERE o.id = #{id} AND o.user_id = #{userId}")
    OrderDetailVO selectDetailById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM photo_order WHERE order_no = #{orderNo}")
    PhotoOrder selectByOrderNo(String orderNo);

    @Update("UPDATE photo_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE photo_order SET status = #{status}, success_count = #{successCount}, finish_time = NOW() WHERE id = #{id}")
    int finish(@Param("id") Long id, @Param("status") Integer status,
               @Param("successCount") Integer successCount);

    /**
     * 查"卡住"的订单：还停在中间态，且创建时间早于 deadline
     * 注意：deadline 是 Java 侧算好传进来的，SQL 里不做时间运算
     */
    @Select("SELECT * FROM photo_order " +
            "WHERE status IN (0, 1) AND create_time < #{deadline} " +
            "ORDER BY id ASC LIMIT 50")
    List<PhotoOrder> selectStuckOrders(@Param("deadline") LocalDateTime deadline);

    /**
     * 条件更新：只有订单还停在中间态时，才改成失败。
     * 返回 1 表示"这次是我改的"，返回 0 表示"已经被处理过了"
     */
    @Update("UPDATE photo_order SET status = #{status}, finish_time = NOW() " +
            "WHERE id = #{id} AND status IN (0, 1)")
    int markFailedIfStuck(@Param("id") Long id, @Param("status") Integer status);
}