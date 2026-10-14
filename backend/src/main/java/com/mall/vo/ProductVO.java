package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项
 */
@Data
@Schema(description = "商品列表项")
public class ProductVO {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题/卖点")
    private String subtitle;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "主图")
    private String mainImage;

    @Schema(description = "最低售价")
    private BigDecimal price;

    @Schema(description = "最低原价")
    private BigDecimal originalPrice;

    @Schema(description = "总库存")
    private Integer stock;

    @Schema(description = "累计销量")
    private Integer sales;

    @Schema(description = "库存预警阈值")
    private Integer warnStock;

    @Schema(description = "状态 1上架 0下架")
    private Integer status;

    @Schema(description = "是否推荐 1是 0否")
    private Integer isRecommend;

    @Schema(description = "是否新品 1是 0否")
    private Integer isNew;

    @Schema(description = "是否热销 1是 0否")
    private Integer isHot;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "SKU 数量")
    private Integer skuCount;
}
