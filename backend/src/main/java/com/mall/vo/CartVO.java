package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车条目
 */
@Data
@Schema(description = "购物车条目")
public class CartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车条目ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "规格展示文本")
    private String specInfo;

    @Schema(description = "成交单价（SKU 售价）")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "可售库存")
    private Integer stock;

    @Schema(description = "是否选中 1是 0否")
    private Integer checked;

    @Schema(description = "商品/SKU 是否有效 1有效 0失效")
    private Integer status;

    @Schema(description = "小计金额（单价 × 数量），下单预览时返回")
    private BigDecimal totalAmount;
}
