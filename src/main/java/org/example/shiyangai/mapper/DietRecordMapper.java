package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.DietRecord;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_date = #{date} ORDER BY create_time DESC")
    List<DietRecord> selectByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate}")
    List<DietRecord> selectByDateRange(@Param("userId") String userId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}