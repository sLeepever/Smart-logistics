package com.smart.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.user.entity.CustomerProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerProfileMapper extends BaseMapper<CustomerProfile> {
}
