package org.example.shiyangai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.shiyangai.entity.ChatHistory;
import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    @Select("SELECT * FROM t_chat_history WHERE user_id = #{userId} ORDER BY created_at ASC LIMIT #{limit}")
    List<ChatHistory> getByUserId(String userId, int limit);
}