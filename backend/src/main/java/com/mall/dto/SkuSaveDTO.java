package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 保存参数
 */
@Data
@Schema(description = "商品SKU保存参数")
public class SkuSaveDTO {

    @Schema(description = "SKU ID，新增时为空")
    private Long id;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "规格键值对 JSON，如 {\"颜色\":\"曜石黑\"}")
    private String specData;

    @Schema(description = "规格展示文本，如 曜石黑 / 256G（为空时由 specData 自动拼接）")
    private String specInfo;

    @Schema(description = "售价")
    @NotNull(message = "SKU 售价不能为空")
    @DecimalMin(value = "0", message = "SKU 售价不能小于 0")
    private BigDecimal price;

    @Schema(description = "原价（为空时默认与售价一致）")
    @DecimalMin(value = "0", message = "SKU 原价不能小于 0")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    @NotNull(message = "SKU 库存不能为空")
    @Min(value = 0, message = "SKU 库存不能小于 0")
    private Integer stock;

    @Schema(description = "SKU 图片")
    private String image;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;
}
