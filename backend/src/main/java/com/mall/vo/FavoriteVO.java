package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的收藏
 */
@Data
@Schema(description = "我的收藏")
public class FavoriteVO {

    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品主图")
    private String productImage;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "商品状态 1上架 0下架")
    private Integer status;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;
}
