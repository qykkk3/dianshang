package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存预警阈值配置
 */
@Data
@Schema(description = "库存预警阈值配置")
public class StockConfigDTO {

    @Schema(description = "库存预警阈值：SKU 库存小于等于该值时进入预警")
    @NotNull(message = "预警阈值不能为空")
    @Min(value = 0, message = "预警阈值不能小于 0")
    private Integer threshold;
}
