package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {

//    @Select("SELECT * FROM t_user WHERE username = #{username}")
//    User selectByUsername(String username);

    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM t_user WHERE id = #{userId}")
    User selectByUserId(String userId);

    // 在 UserMapper 中添加
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

}