package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端评价查询条件
 */
@Data
@Schema(description = "评价查询条件")
public class CommentQueryDTO {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "评价用户ID")
    private Long userId;

    @Schema(description = "评分 1-5")
    private Integer star;

    @Schema(description = "状态 1显示 0隐藏")
    private Integer status;

    @Schema(description = "关键字（评价内容 / 商品名称）")
    private String keyword;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 10;
}
