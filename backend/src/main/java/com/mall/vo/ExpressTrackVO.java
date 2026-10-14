package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流轨迹节点
 */
@Data
@Schema(description = "物流轨迹节点")
public class ExpressTrackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "轨迹ID")
    private Long id;

    @Schema(description = "节点状态：已揽收/运输中/派送中/已签收")
    private String status;

    @Schema(description = "轨迹描述")
    private String context;

    @Schema(description = "所在城市")
    private String location;

    @Schema(description = "节点时间")
    private LocalDateTime acceptTime;
}
