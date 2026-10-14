package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端首页数据总览
 */
@Data
@Schema(description = "数据看板总览")
public class StatsOverviewVO {

    @Schema(description = "今日销售额")
    private BigDecimal todaySales = BigDecimal.ZERO;

    @Schema(description = "今日订单数")
    private Long todayOrders = 0L;

    @Schema(description = "今日新增用户")
    private Long todayNewUsers = 0L;

    @Schema(description = "累计销售额")
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Schema(description = "累计订单数")
    private Long totalOrders = 0L;

    @Schema(description = "会员总数")
    private Long totalUsers = 0L;

    @Schema(description = "在售商品数")
    private Long totalProducts = 0L;

    @Schema(description = "待发货订单数")
    private Long pendingShipOrders = 0L;

    @Schema(description = "待处理售后数")
    private Long pendingRefunds = 0L;

    @Schema(description = "库存预警数量")
    private Long stockWarnings = 0L;

    @Schema(description = "在线用户数")
    private Long onlineUsers = 0L;

    @Schema(description = "商品评价总数")
    private Long totalComments = 0L;
}
