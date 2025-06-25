package com.tmd.ai.mapper;

import com.tmd.ai.entity.po.Sessions;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataProcessMapper {


    @Insert("insert into sessions(session_id, user_id, question, content, create_time) values (#{sessionId},#{userId},#{question},#{content},#{createTime})")
    void addSession(Sessions sessions);
}
