package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 售后申请请求参数
 */
@Data
@Schema(description = "售后申请请求参数")
public class RefundApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单项ID，为空表示整单售后")
    private Long orderItemId;

    @Schema(description = "售后类型 1仅退款 2退货退款 3换货", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请选择售后类型")
    private Integer type;

    @Schema(description = "申请原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量问题")
    @NotBlank(message = "申请原因不能为空")
    private String reason;

    @Schema(description = "问题描述", example = "商品存在瑕疵，申请售后处理")
    private String description;

    @Schema(description = "凭证图片地址集合")
    private List<String> images;

    @Schema(description = "申请退款金额，为空时按订单/明细金额计算", example = "178.00")
    private BigDecimal refundAmount;
}
