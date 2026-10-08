package com.yumi.cute.mapper;

import com.yumi.cute.entity.PhotoTask;
import com.yumi.cute.vo.PhotoTaskVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PhotoTaskMapper {

    @Insert("<script>" +
            "INSERT INTO photo_task (order_id, seq_no, status) VALUES " +
            "<foreach collection='tasks' item='t' separator=','>" +
            "(#{t.orderId}, #{t.seqNo}, #{t.status})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("tasks") List<PhotoTask> tasks);

    @Select("SELECT seq_no, status, image_url, error_msg " +
            "FROM photo_task WHERE order_id = #{orderId} ORDER BY seq_no ASC")
    List<PhotoTaskVO> selectByOrderId(Long orderId);

    /**
     * 查实体列表（生成器处理时用，需要 id）
     */
    @Select("SELECT * FROM photo_task WHERE order_id = #{orderId} ORDER BY seq_no ASC")
    List<PhotoTask> selectEntitiesByOrderId(Long orderId);

    @Update("UPDATE photo_task SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 标记成功：只更新状态和结果图。
     * 注意：不写 error_msg 这一列 —— 不写它，它的默认值 '' 就会生效
     */
    @Update("UPDATE photo_task SET status = #{status}, image_url = #{imageUrl}, " +
            "update_time = NOW() WHERE id = #{id}")
    int markSuccess(@Param("id") Long id, @Param("status") Integer status,
                    @Param("imageUrl") String imageUrl);

    /**
     * 标记失败：只更新状态和失败原因。
     * 不写 image_url 这一列，保留它原本的值
     */
    @Update("UPDATE photo_task SET status = #{status}, error_msg = #{errorMsg}, " +
            "update_time = NOW() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("status") Integer status,
                   @Param("errorMsg") String errorMsg);
}