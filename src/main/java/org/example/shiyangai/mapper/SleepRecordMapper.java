package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.SleepRecord;
import java.time.LocalDate;
import java.util.List;

/**
 * 睡眠记录Mapper (Phase 2)
 */
@Mapper
public interface SleepRecordMapper extends BaseMapper<SleepRecord> {

    @Select("SELECT * FROM sleep_record WHERE user_id = #{userId} AND record_date = #{date} ORDER BY create_time DESC")
    List<SleepRecord> selectByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT * FROM sleep_record WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate}")
    List<SleepRecord> selectByDateRange(@Param("userId") String userId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}