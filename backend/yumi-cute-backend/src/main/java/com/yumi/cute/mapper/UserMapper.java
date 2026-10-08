package com.yumi.cute.mapper;

import com.yumi.cute.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectByUsername(String username);

    @Insert("INSERT INTO `user` (username, password, nickname, points) VALUES (#{username}, #{password}, #{nickname}, #{points})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET avatar_url = #{avatarUrl},update_time=NOW() WHERE id = #{userId}")
    int updateAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);

    /**
     * 扣减算力。
     * 注意 WHERE 里的 points >= #{cost}，这是防超扣的关键
     */
    @Update("UPDATE `user` SET points = points - #{cost}, update_time = NOW() " +
            "WHERE id = #{userId} AND points >= #{cost}")
    int deductPoints(@Param("userId") Long userId, @Param("cost") Integer cost);

    @Update("UPDATE `user` SET points = points + #{amount}, update_time = NOW() WHERE id = #{id}")
    int addPoints(@Param("id") Long id, @Param("amount") Integer amount);
}