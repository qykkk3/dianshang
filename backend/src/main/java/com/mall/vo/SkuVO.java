package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU
 */
@Data
@Schema(description = "商品SKU")
public class SkuVO {

    @Schema(description = "SKU ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "规格键值对 JSON")
    private String specData;

    @Schema(description = "规格展示文本")
    private String specInfo;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "SKU 图片")
    private String image;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;
}
