package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品查询条件（买家端 / 管理端共用）
 */
@Data
@Schema(description = "商品查询条件")
public class ProductQueryDTO {

    @Schema(description = "关键词，模糊匹配商品名称/副标题/品牌")
    private String keyword;

    @Schema(description = "分类ID，传一级分类时自动包含其子分类")
    private Long categoryId;

    @Schema(description = "状态 1上架 0下架（管理端过滤条件，买家端固定只查上架）")
    private Integer status;

    @Schema(description = "是否推荐 1是 0否")
    private Integer isRecommend;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    @Min(value = 1, message = "每页条数不能小于 1")
    private Integer size = 10;

    @Schema(description = "最低价格")
    @DecimalMin(value = "0", message = "最低价格不能小于 0")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    @Schema(description = "排序方式：default(推荐+排序值)、sales(销量倒序)、price_asc、price_desc、new(创建时间倒序)")
    private String sort;
}
