package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单信息（列表项）
 */
@Data
@Schema(description = "订单信息")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "买家ID")
    private Long userId;

    @Schema(description = "买家昵称")
    private String buyerNickname;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount;

    @Schema(description = "运费")
    private BigDecimal freightAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "订单状态 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中")
    private Integer status;

    @Schema(description = "订单状态名称")
    private String statusName;

    @Schema(description = "支付方式 1模拟支付")
    private Integer payType;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime shipTime;

    @Schema(description = "收货时间")
    private LocalDateTime receiveTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "关闭（取消）时间")
    private LocalDateTime closeTime;

    @Schema(description = "收货人（快照）")
    private String receiverName;

    @Schema(description = "收货电话（快照）")
    private String receiverPhone;

    @Schema(description = "收货地址（快照）")
    private String receiverAddress;

    @Schema(description = "快递公司")
    private String expressCompany;

    @Schema(description = "快递单号")
    private String expressNo;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "商家备注")
    private String adminRemark;

    @Schema(description = "商品件数")
    private Integer itemCount;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细")
    private List<OrderItemVO> items = new ArrayList<>();

    @Schema(description = "是否存在售后单")
    private Boolean hasRefund = false;
}
