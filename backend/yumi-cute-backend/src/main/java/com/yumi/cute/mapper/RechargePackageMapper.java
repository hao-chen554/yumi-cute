package com.yumi.cute.mapper;

import com.yumi.cute.entity.RechargePackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RechargePackageMapper {
    @Select("SELECT * FROM recharge_package WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<RechargePackage> selectOnSale();

    @Select("SELECT * FROM recharge_package WHERE id = #{id}")
    RechargePackage selectById(Integer id);
}
