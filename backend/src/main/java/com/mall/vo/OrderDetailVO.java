package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单详情：在订单信息基础上补充物流轨迹与售后工单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单详情")
public class OrderDetailVO extends OrderVO {

    private static final long serialVersionUID = 1L;

    @Schema(description = "物流轨迹（按节点时间倒序）")
    private List<ExpressTrackVO> tracks = new ArrayList<>();

    @Schema(description = "售后工单")
    private List<RefundVO> refunds = new ArrayList<>();
}
