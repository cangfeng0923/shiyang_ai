package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.shiyangai.entity.HealthProfile;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfile> {

    @Select("SELECT * FROM health_profile WHERE user_id = #{userId}")
    HealthProfile selectByUserId(@Param("userId") String userId);

    @Update("UPDATE health_profile SET ${field} = #{value}, update_time = NOW() WHERE user_id = #{userId}")
    void updateField(@Param("userId") String userId,
                     @Param("field") String field,
                     @Param("value") String value);
}
