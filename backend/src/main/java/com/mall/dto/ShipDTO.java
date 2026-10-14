package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单发货请求参数
 */
@Data
@Schema(description = "订单发货请求参数")
public class ShipDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "快递公司", requiredMode = Schema.RequiredMode.REQUIRED, example = "顺丰速运")
    @NotBlank(message = "快递公司不能为空")
    private String expressCompany;

    @Schema(description = "快递单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SF330568540791")
    @NotBlank(message = "快递单号不能为空")
    private String expressNo;
}
