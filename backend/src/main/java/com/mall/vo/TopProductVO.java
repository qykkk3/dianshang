package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销商品排行
 */
@Data
@Schema(description = "热销商品排行")
public class TopProductVO {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "销量")
    private Long saleCount;

    @Schema(description = "销售额")
    private BigDecimal totalAmount;
}
