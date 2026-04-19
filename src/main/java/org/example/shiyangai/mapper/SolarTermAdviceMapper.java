// mapper/SolarTermAdviceMapper.java
package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.SolarTermAdvice;

@Mapper
public interface SolarTermAdviceMapper extends BaseMapper<SolarTermAdvice> {

    @Select("SELECT * FROM solar_term_advice WHERE solar_term_name = #{termName} AND constitution = #{constitution} AND year = YEAR(NOW()) LIMIT 1")
    SolarTermAdvice findByTermAndConstitution(@Param("termName") String termName,
                                              @Param("constitution") String constitution);
}