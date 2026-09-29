package com.yumi.cute.service;

import com.yumi.cute.entity.StyleTemplate;
import com.yumi.cute.mapper.StyleTemplateMapper;
import com.yumi.cute.vo.StyleTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StyleTemplateService {
    private final StyleTemplateMapper styleTemplateMapper;

    public List<StyleTemplateVO> listEnabled(){
        return styleTemplateMapper.selectAllEnabled().stream().map(this::toVO).toList();
    }

    /** 实体 -> VO的转换，只保留允许暴露的字段 */
    private StyleTemplateVO toVO(StyleTemplate styleTemplate){
        StyleTemplateVO vo = new StyleTemplateVO();
        vo.setId(styleTemplate.getId());
        vo.setName(styleTemplate.getName());
        vo.setDescription(styleTemplate.getDescription());
        vo.setPointsCost(styleTemplate.getPointsCost());
        vo.setCoverUrl(styleTemplate.getCoverUrl());
        return vo;
    }
}
