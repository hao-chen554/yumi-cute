package com.yumi.cute.mapper;

import com.yumi.cute.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM `user`")
    List<User> selectAll();

    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User selectByUsername(String username);

    @Insert("INSERT INTO `user` (username, password, nickname, points) VALUES (#{username}, #{password}, #{nickname}, #{points})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}