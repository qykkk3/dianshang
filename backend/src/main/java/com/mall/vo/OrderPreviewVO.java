package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单结算预览结果
 */
@Data
@Schema(description = "订单结算预览结果")
public class OrderPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "待结算商品明细（含小计金额）")
    private List<CartVO> items = new ArrayList<>();

    @Schema(description = "收货地址（默认地址）")
    private AddressVO address;

    @Schema(description = "商品总额")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Schema(description = "运费")
    private BigDecimal freightAmount = BigDecimal.ZERO;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Schema(description = "应付金额")
    private BigDecimal payAmount = BigDecimal.ZERO;

    @Schema(description = "当前订单可用的优惠券")
    private List<UserCouponVO> availableCoupons = new ArrayList<>();

    @Schema(description = "当前试算使用的用户优惠券ID")
    private Long userCouponId;
}
