package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分类销售统计
 */
@Data
@Schema(description = "分类销售统计")
public class CategorySalesVO {

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "销量")
    private Long saleCount;

    @Schema(description = "销售额")
    private BigDecimal totalAmount;
}
