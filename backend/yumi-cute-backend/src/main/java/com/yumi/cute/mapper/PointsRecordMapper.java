package com.yumi.cute.mapper;

import com.yumi.cute.entity.PointsRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointsRecordMapper {

    @Insert("INSERT INTO points_record (user_id, change_amount, balance_after, type, biz_id, remark) " +
            "VALUES (#{userId}, #{changeAmount}, #{balanceAfter}, #{type}, #{bizId}, #{remark})")
    int insert(PointsRecord record);
}