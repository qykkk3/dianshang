package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单明细请求参数（立即购买）
 */
@Data
@Schema(description = "订单明细请求参数")
public class OrderItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @Schema(description = "购买数量，至少 1", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;
}
