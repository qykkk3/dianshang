package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单状态分布
 */
@Data
@Schema(description = "订单状态分布")
public class OrderStatusCountVO {

    @Schema(description = "状态码 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "金额")
    private BigDecimal totalAmount;
}
