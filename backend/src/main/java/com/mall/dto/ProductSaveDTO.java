package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品新增/编辑参数
 *
 * <p>说明：SPU 的 price/originalPrice/stock 由后端根据 SKU 自动计算，前端无需传入。</p>
 */
@Data
@Schema(description = "商品新增/编辑参数")
public class ProductSaveDTO {

    @Schema(description = "商品ID，新增时为空")
    private Long id;

    @Schema(description = "分类ID")
    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    @Schema(description = "商品名称")
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过 200 个字符")
    private String name;

    @Schema(description = "副标题/卖点")
    @Size(max = 255, message = "副标题不能超过 255 个字符")
    private String subtitle;

    @Schema(description = "品牌")
    @Size(max = 50, message = "品牌不能超过 50 个字符")
    private String brand;

    @Schema(description = "单位，默认 件")
    @Size(max = 20, message = "单位不能超过 20 个字符")
    private String unit;

    @Schema(description = "主图")
    @Size(max = 255, message = "主图地址不能超过 255 个字符")
    private String mainImage;

    @Schema(description = "轮播图列表")
    private List<String> images;

    @Schema(description = "图文详情(HTML)")
    private String detail;

    @Schema(description = "状态 1上架 0下架")
    private Integer status;

    @Schema(description = "是否推荐 1是 0否")
    private Integer isRecommend;

    @Schema(description = "是否新品 1是 0否")
    private Integer isNew;

    @Schema(description = "是否热销 1是 0否")
    private Integer isHot;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "库存预警阈值，为空时使用系统配置")
    private Integer warnStock;

    @Schema(description = "成本价")
    private BigDecimal costPrice;

    @Schema(description = "规格列表")
    @Valid
    private List<SpecSaveDTO> specs;

    @Schema(description = "SKU 列表，不能为空")
    @Valid
    private List<SkuSaveDTO> skus;
}
