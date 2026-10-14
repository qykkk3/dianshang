package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单结算预览请求参数
 */
@Data
@Schema(description = "订单结算预览请求参数")
public class OrderPreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车条目ID集合，购物车结算时使用")
    private List<Long> cartIds;

    @Schema(description = "立即购买商品，立即购买时使用")
    @Valid
    private OrderItemDTO buyNow;

    @Schema(description = "用户优惠券ID，用于试算优惠金额")
    private Long userCouponId;
}
