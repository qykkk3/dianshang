package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改购物车条目请求参数
 */
@Data
@Schema(description = "修改购物车条目请求参数")
public class CartUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购买数量，至少 1", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于 0")
    private Integer quantity;

    @Schema(description = "是否选中 1是 0否", example = "1")
    private Integer checked;
}
