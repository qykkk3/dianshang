package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券
 */
@Data
@Schema(description = "优惠券")
public class CouponVO {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "券名称")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    private Integer type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "满减金额")
    private BigDecimal amount;

    @Schema(description = "折扣率")
    private BigDecimal discount;

    @Schema(description = "使用门槛金额")
    private BigDecimal minAmount;

    @Schema(description = "发行总量")
    private Integer totalCount;

    @Schema(description = "已领取数量")
    private Integer receivedCount;

    @Schema(description = "已使用数量")
    private Integer usedCount;

    @Schema(description = "每人限领")
    private Integer perLimit;

    @Schema(description = "适用范围 1全场 2指定分类 3指定商品")
    private Integer scope;

    @Schema(description = "适用范围名称")
    private String scopeNames;

    @Schema(description = "生效时间")
    private LocalDateTime startTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "使用说明")
    private String description;

    @Schema(description = "当前登录用户是否已领取，未登录时为 false")
    private Boolean received = Boolean.FALSE;
}
