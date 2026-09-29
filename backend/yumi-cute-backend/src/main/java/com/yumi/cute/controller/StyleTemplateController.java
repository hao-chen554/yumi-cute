package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.service.StyleTemplateService;
import com.yumi.cute.vo.StyleTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/style")
@RequiredArgsConstructor
public class StyleTemplateController {

    private final StyleTemplateService styleTemplateService;

    @GetMapping("/list")
    public Result<List<StyleTemplateVO>> list() {
        return Result.success(styleTemplateService.listEnabled());
    }
}