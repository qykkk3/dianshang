package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 库存预警明细
 */
@Data
@Schema(description = "库存预警明细")
public class StockWarningVO {

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "规格")
    private String specInfo;

    @Schema(description = "商品主图")
    private String mainImage;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "预警阈值")
    private Integer warnStock;

    @Schema(description = "商品状态 1上架 0下架")
    private Integer productStatus;
}
