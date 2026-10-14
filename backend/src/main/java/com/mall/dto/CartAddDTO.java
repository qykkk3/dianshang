package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 加入购物车请求参数
 */
@Data
@Schema(description = "加入购物车请求参数")
public class CartAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @Schema(description = "购买数量，至少 1", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;
}
