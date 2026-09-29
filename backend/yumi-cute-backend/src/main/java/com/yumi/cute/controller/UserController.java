package com.yumi.cute.controller;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.Result;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.UserLoginDTO;
import com.yumi.cute.dto.UserRegisterDTO;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.UserMapper;
import com.yumi.cute.service.UserService;
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

    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userMapper.selectAll());
    }

    @GetMapping("/detail/{id}")
    public Result<User> detail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success(user);
    }

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
    public Result<User> me(){
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        return Result.success(user);
    }
}