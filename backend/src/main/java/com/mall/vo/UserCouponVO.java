package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券（我的优惠券 / 领券记录）
 */
@Data
@Schema(description = "用户优惠券")
public class UserCouponVO {

    @Schema(description = "用户券ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "券名称")
    private String couponName;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "满减金额")
    private BigDecimal amount;

    @Schema(description = "折扣率")
    private BigDecimal discount;

    @Schema(description = "使用门槛金额")
    private BigDecimal minAmount;

    @Schema(description = "状态 0未使用 1已使用 2已过期")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "使用的订单ID")
    private Long orderId;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "使用说明")
    private String description;
}
