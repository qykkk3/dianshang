package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 售后审核请求参数（管理端）
 */
@Data
@Schema(description = "售后审核请求参数")
public class RefundAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "审核结果 1同意 2拒绝 4退款完成", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请选择审核结果")
    private Integer status;

    @Schema(description = "审核意见", example = "已与买家协商一致")
    private String auditRemark;

    @Schema(description = "实际退款金额，为空时保持原申请金额")
    private BigDecimal refundAmount;
}
