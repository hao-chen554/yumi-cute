package com.yumi.cute.mapper;

import com.yumi.cute.entity.UserFile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFileMapper {

    @Insert("Insert into user_file (user_id,url,biz_type,size_bytes,expire_at)" +
            "values (#{userId},#{url},#{bizType},#{sizeBytes},#{expireAt,jdbcType=TIMESTAMP})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserFile file);

    /**
     * 按URL查文件的属主
     */
    @Select("Select * from user_file where url = #{url}")
    UserFile selectByUrl(String url);
}
