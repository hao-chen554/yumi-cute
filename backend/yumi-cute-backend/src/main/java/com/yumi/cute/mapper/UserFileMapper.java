package com.yumi.cute.mapper;

import com.yumi.cute.entity.UserFile;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    /**
     * 将一个文件的过期时间设为N天后，时间采用同一个时钟
     */
    @Update("UPDATE user_file SET expire_at = DATE_ADD(NOW(), INTERVAL #{days} DAY) WHERE url = #{url} AND expire_at IS NULL")
    int markExpireAfterDays(@Param("url") String url, @Param("days") int days);

    /**
     * 找出已过期，但暂未删除的文件
     */
    @Select("SELECT * FROM user_file WHERE deleted_at IS NULL AND expire_at IS NOT NULL AND expire_at < NOW() AND biz_type <> 4 ORDER BY expire_at ASC LIMIT #{limit}")
    List<UserFile> selectExpired(@Param("limit") int limit);

    /**
     * 孤儿文件 -> 上传了但从未被使用，并且已存放很久
     */
    @Select("SELECT * FROM user_file WHERE deleted_at IS NULL AND expire_at IS NULL AND biz_type = 0 AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY) ORDER BY id ASC LIMIT #{limit}")
    List<UserFile> selectOrphans(@Param("days") int days, @Param("limit") int limit);

    @Update("UPDATE user_file SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int markDeleted(@Param("id") Long id);
}
