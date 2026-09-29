package com.yumi.cute.service;

import com.yumi.cute.common.BizException;
import com.yumi.cute.common.PointsType;
import com.yumi.cute.common.ResultCode;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.dto.UserLoginDTO;
import com.yumi.cute.dto.UserRegisterDTO;
import com.yumi.cute.entity.PointsRecord;
import com.yumi.cute.entity.User;
import com.yumi.cute.mapper.PointsRecordMapper;
import com.yumi.cute.mapper.UserMapper;
import com.yumi.cute.util.JwtUtil;
import com.yumi.cute.vo.UserInfoVO;
import com.yumi.cute.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int REGISTER_GIFT_POINTS = 100;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 用于密码加密
    private final PointsRecordMapper pointsRecordMapper;
    private final JwtUtil jwtUtil;

    @Transactional(rollbackFor = Exception.class) // 遇到任何异常都回滚
    public void register(UserRegisterDTO dto) {
        User exist = userMapper.selectByUsername(dto.getUsername()); // 查询是否已注册
        if (exist != null) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPoints(REGISTER_GIFT_POINTS);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户名已存在");
        }

        // 插入算力流水（user.getId()已被回填）
        PointsRecord record = new PointsRecord();
        record.setUserId(user.getId());
        record.setChangeAmount(REGISTER_GIFT_POINTS);
        record.setBalanceAfter(REGISTER_GIFT_POINTS);
        record.setType(PointsType.REGISTER_GIFT);
        record.setRemark("注册赠送！");

        pointsRecordMapper.insert(record);
    }

    public UserLoginVO login(UserLoginDTO dto) {
        User user = userMapper.selectByUsername(dto.getUsername());

        // 用户不存在 和 密码错误，返回同一异常信息
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误！");
        }

        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用！");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getNickname());

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(token);
        userLoginVO.setUserId(user.getId());
        userLoginVO.setNickname(user.getNickname());
        userLoginVO.setPoints(user.getPoints());
        userLoginVO.setUsername(user.getUsername());
        return userLoginVO;
    }

    /** 获取当前登录用户的信息 */
    public UserInfoVO getCurrentUser() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setPoints(user.getPoints());
        vo.setVipLevel(user.getVipLevel());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /** 更新头像 */
    public void updateAvatar(Long userId, String avatarUrl) {
        userMapper.updateAvatar(userId, avatarUrl);
    }

}
