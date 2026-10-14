package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单查询请求参数（买家/管理端通用）
 */
@Data
@Schema(description = "订单查询请求参数")
public class OrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号，支持模糊查询")
    private String orderNo;

    @Schema(description = "订单状态 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中")
    private Integer status;

    @Schema(description = "买家ID（管理端筛选）")
    private Long userId;

    @Schema(description = "下单开始时间，格式 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "下单结束时间，格式 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "关键词：订单号或商品名称")
    private String keyword;

    @Schema(description = "页码，从 1 开始", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;
}
