package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单改价请求参数（管理端）
 */
@Data
@Schema(description = "订单改价请求参数")
public class OrderPriceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "修改后的实付金额，不能小于 0.01", requiredMode = Schema.RequiredMode.REQUIRED, example = "88.00")
    @NotNull(message = "实付金额不能为空")
    @DecimalMin(value = "0.01", message = "实付金额不能小于 0.01")
    private BigDecimal payAmount;
}
