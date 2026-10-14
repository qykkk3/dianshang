package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/** OrderItem Mapper */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
