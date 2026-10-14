package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品详情：在列表字段的基础上补充商品全字段、规格、SKU、收藏与评价信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品详情")
public class ProductDetailVO extends ProductVO {

    @Schema(description = "轮播图，逗号分隔")
    private String images;

    @Schema(description = "图文详情(HTML)")
    private String detail;

    @Schema(description = "成本价（仅管理端使用）")
    private BigDecimal costPrice;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "规格列表")
    private List<SpecVO> specs = new ArrayList<>();

    @Schema(description = "SKU 列表（买家端仅返回启用状态的 SKU）")
    private List<SkuVO> skus = new ArrayList<>();

    @Schema(description = "当前登录用户是否已收藏，未登录恒为 false")
    private Boolean favorited = false;

    @Schema(description = "评价数量")
    private Long commentCount = 0L;

    @Schema(description = "平均评分，无评价时为 null")
    private Double avgStar;
}
