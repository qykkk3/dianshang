package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售趋势（按天）
 */
@Data
@Schema(description = "销售趋势")
public class SalesTrendVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String saleDate;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "销售额")
    private BigDecimal totalAmount;
}
