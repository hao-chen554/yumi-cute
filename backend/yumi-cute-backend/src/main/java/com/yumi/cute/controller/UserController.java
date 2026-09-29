package com.yumi.cute.controller;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.Result;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.UpdateAvatarDTO;
import com.yumi.cute.dto.UserLoginDTO;
import com.yumi.cute.dto.UserRegisterDTO;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.UserMapper;
import com.yumi.cute.service.UserService;
import com.yumi.cute.vo.UserInfoVO;
import com.yumi.cute.vo.UserLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestBody @Valid UpdateAvatarDTO dto) {
        userService.updateAvatar(UserContext.getUserId(), dto.getAvatarUrl());
        return Result.success();
    }
}