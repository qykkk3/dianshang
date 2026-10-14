package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端优惠券查询条件
 */
@Data
@Schema(description = "优惠券查询条件")
public class CouponQueryDTO {

    @Schema(description = "关键字（券名称）")
    private String keyword;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "页码，从 1 开始")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 10;
}
