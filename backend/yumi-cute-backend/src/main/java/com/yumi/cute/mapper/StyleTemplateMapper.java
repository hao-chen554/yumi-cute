package com.yumi.cute.mapper;

import com.yumi.cute.entity.StyleTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StyleTemplateMapper {

    @Select("SELECT * FROM style_template WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<StyleTemplate> selectAllEnabled();
}