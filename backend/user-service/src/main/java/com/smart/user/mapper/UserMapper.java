package com.smart.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT id, username, password, real_name, phone, role, status, deleted, created_at, updated_at FROM users WHERE username = #{username} LIMIT 1")
    User selectAnyByUsername(@Param("username") String username);
}
