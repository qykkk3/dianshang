package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/** Orders Mapper */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
}
