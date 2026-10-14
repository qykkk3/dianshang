package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 买家订单状态数量统计
 */
@Data
@Schema(description = "订单状态数量统计")
public class OrderCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "待付款数量")
    private Long unpaid = 0L;

    @Schema(description = "待发货数量")
    private Long paid = 0L;

    @Schema(description = "待收货数量")
    private Long shipped = 0L;

    @Schema(description = "已完成数量")
    private Long finished = 0L;

    @Schema(description = "售后中数量")
    private Long afterSale = 0L;

    @Schema(description = "待评价明细数量")
    private Long toComment = 0L;
}
