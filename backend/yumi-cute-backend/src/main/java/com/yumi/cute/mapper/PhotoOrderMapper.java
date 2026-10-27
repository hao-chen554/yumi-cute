package com.yumi.cute.mapper;

import com.yumi.cute.entity.PhotoOrder;
import com.yumi.cute.vo.OrderDetailVO;
import com.yumi.cute.vo.OrderListVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PhotoOrderMapper {

    @Insert("INSERT INTO photo_order (order_no, client_token, user_id, pet_id, style_id, person_photo_url, pet_photo_url, " +
            "order_type, total_count, success_count, points_cost, status) " +
            "VALUES (#{orderNo}, #{clientToken}, #{userId}, #{petId}, #{styleId}, #{personPhotoUrl}, #{petPhotoUrl}, " +
            "#{orderType}, #{totalCount}, #{successCount}, #{pointsCost}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PhotoOrder order);

    /**
     * 查某个用户的订单列表
     * 用 LEFT JOIN 把风格名一起查出来，避免"先查订单、再逐个查风格"的 N+1 问题
     */
    @Select("""
        SELECT o.id, o.order_no, o.order_type, o.total_count, o.success_count,
               o.points_cost, o.status, o.create_time, s.name AS style_name
        FROM photo_order o
        LEFT JOIN style_template s ON s.id = o.style_id
        WHERE o.user_id = #{userId} AND o.deleted_at IS NULL
        ORDER BY o.id DESC
        """)
    List<OrderListVO> selectByUserId(Long userId);

    /**
     * 查订单详情
     * 注意 WHERE 里的 user_id —— 防水平越权，只能查自己的订单
     */
    @Select("SELECT o.*, s.name AS style_name " +
            "FROM photo_order o " +
            "LEFT JOIN style_template s ON s.id = o.style_id " +
            "WHERE o.id = #{id} AND o.user_id = #{userId} AND o.deleted_at IS NULL")
    OrderDetailVO selectDetailById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM photo_order WHERE order_no = #{orderNo}")
    PhotoOrder selectByOrderNo(String orderNo);

    @Update("UPDATE photo_order SET status = #{status} WHERE id = #{id} AND status IN (0, 1)")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE photo_order SET status = #{status}, success_count = #{successCount}, finish_time = NOW() WHERE id = #{id} AND status IN (0, 1)")
    int finish(@Param("id") Long id, @Param("status") Integer status,
               @Param("successCount") Integer successCount);

    /**
     * 两边都用数据库的时钟
     */
    @Select("SELECT * FROM photo_order " +
            "WHERE status IN (0, 1) AND create_time < DATE_SUB(NOW(), INTERVAL #{stuckMinutes} MINUTE) " +
            "ORDER BY id ASC LIMIT 50")
    List<PhotoOrder> selectStuckOrders(@Param("stuckMinutes") int stuckMinutes);

    /**
     * 条件更新：只有订单还停在中间态时，才改成失败。
     * 返回 1 表示"这次是我改的"，返回 0 表示"已经被处理过了"
     */
    @Update("UPDATE photo_order SET status = #{status}, finish_time = NOW() " +
            "WHERE id = #{id} AND status IN (0, 1)")
    int markFailedIfStuck(@Param("id") Long id, @Param("status") Integer status);

    @Select("SELECT * FROM photo_order WHERE user_id = #{userId} AND client_token = #{clientToken}")
    PhotoOrder selectByClientToken(@Param("userId") Long userId, @Param("clientToken") String clientToken);

    /**
     * 查自己的订单 (不带分享有效性判断，仅用于归属校验)
     */
    @Select("SELECT * FROM photo_order WHERE user_id = #{userId} AND id = #{id}")
    PhotoOrder selectMineById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 启用分享：同时完成【归属校验 + 状态校验 + 写入token】
     * 旧token会被覆盖 -> 旧链接立刻失效
     */
    @Update("UPDATE photo_order SET share_token = #{token}, share_expire_at = DATE_ADD(NOW(), INTERVAL #{days} DAY) WHERE id = #{id} AND user_id = #{userId} AND status IN (2,3) AND deleted_at IS NULL")
    int enableShare(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("token") String token,
            @Param("days") int days
    );

    /**
     * 撤销分享（幂等，已撤销再调不会报错）
     */
    @Update("UPDATE photo_order SET share_token = NULL,share_expire_at = NULL WHERE id = #{id} AND user_id = #{userId}")
    int cancelShare(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 当前有效的分享
     */
    @Select("SELECT * FROM photo_order WHERE id = #{id} AND user_id = #{userId} AND share_token IS NOT NULL AND share_expire_at > NOW() AND deleted_at IS NULL")
    PhotoOrder selectMyValidShare(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 按token查询有效分享（用于公共接口）
     */
    @Select("SELECT * FROM photo_order WHERE share_token = #{token} AND share_expire_at > NOW() AND deleted_at IS NULL")
    PhotoOrder selectByValidShareToken(String token);

    /**
     * 标记删除，返回1表示删除成功
     */
    @Update("UPDATE photo_order SET deleted_at = NOW(),share_token = NULL,share_expire_at = NULL WHERE id = #{id} AND user_id = #{userId} AND deleted_at IS NULL AND status IN (2,3,4,5)")
    int markDeleted(@Param("id") Long id, @Param("userId") Long userId);
}
