package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.service.ShareService;
import com.yumi.cute.vo.ShareViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开的分享查看接口 -- 无需登录
 * 放行靠 JwtInterceptor的前缀白名单 "/share/"
 */
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @GetMapping("/{token}")
    public Result<ShareViewVO> view(@PathVariable String token) {
        return Result.success(shareService.viewByToken(token));
    }
}
