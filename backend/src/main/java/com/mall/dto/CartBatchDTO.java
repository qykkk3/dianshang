package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 购物车批量操作请求参数
 */
@Data
@Schema(description = "购物车批量操作请求参数")
public class CartBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车条目ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一条购物车记录")
    private List<Long> ids;
}
