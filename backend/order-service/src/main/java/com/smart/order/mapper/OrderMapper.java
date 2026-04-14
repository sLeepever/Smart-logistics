package com.smart.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT NEXTVAL('order_no_seq')")
    long nextOrderNoSeq();
}
