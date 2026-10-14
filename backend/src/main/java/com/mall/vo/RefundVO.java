package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后工单
 */
@Data
@Schema(description = "售后工单")
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "售后ID")
    private Long id;

    @Schema(description = "售后单号")
    private String refundNo;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单项ID（单品售后）")
    private Long orderItemId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @Schema(description = "买家ID")
    private Long userId;

    @Schema(description = "买家昵称")
    private String buyerNickname;

    @Schema(description = "类型 1仅退款 2退货退款 3换货")
    private Integer type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "申请原因")
    private String reason;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "凭证图片地址集合")
    private List<String> images;

    @Schema(description = "申请退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "状态 0待审核 1已同意 2已拒绝 3待买家退货 4退款完成 5已撤销")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "审核人")
    private String auditUser;

    @Schema(description = "审核意见")
    private String auditRemark;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
