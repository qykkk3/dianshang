package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SKU 库存调整参数
 */
@Data
@Schema(description = "SKU库存调整参数")
public class StockUpdateDTO {

    @Schema(description = "调整后的库存数量")
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能小于 0")
    private Integer stock;
}
