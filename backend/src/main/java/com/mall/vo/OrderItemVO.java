package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细项
 */
@Data
@Schema(description = "订单明细项")
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品名称（快照）")
    private String productName;

    @Schema(description = "商品图片（快照）")
    private String productImage;

    @Schema(description = "规格（快照）")
    private String specInfo;

    @Schema(description = "成交单价")
    private BigDecimal price;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "小计金额")
    private BigDecimal totalAmount;

    @Schema(description = "是否已评价 1是 0否")
    private Integer isCommented;
}
