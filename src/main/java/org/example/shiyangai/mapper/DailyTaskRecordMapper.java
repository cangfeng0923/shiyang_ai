package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.DailyTaskRecord;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyTaskRecordMapper extends BaseMapper<DailyTaskRecord> {

    @Select("SELECT * FROM daily_task_record WHERE user_id = #{userId} AND task_date = #{date}")
    List<DailyTaskRecord> selectByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM daily_task_record WHERE user_id = #{userId} AND task_date = #{date} AND completed = 1")
    Integer getCompletedCount(@Param("userId") String userId, @Param("date") LocalDate date);
}