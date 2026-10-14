package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提交订单请求参数
 */
@Data
@Schema(description = "提交订单请求参数")
public class OrderSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "请选择收货地址")
    private Long addressId;

    @Schema(description = "购物车条目ID集合，购物车下单时使用")
    private List<Long> cartIds;

    @Schema(description = "立即购买商品，立即购买时使用")
    @Valid
    private OrderItemDTO buyNow;

    @Schema(description = "用户优惠券ID，不使用优惠券时为空")
    private Long userCouponId;

    @Schema(description = "买家备注", example = "工作日白天送货")
    private String remark;
}
