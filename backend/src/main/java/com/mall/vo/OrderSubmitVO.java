package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单提交结果
 */
@Data
@Schema(description = "订单提交结果")
public class OrderSubmitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "应付金额")
    private BigDecimal payAmount;
}
