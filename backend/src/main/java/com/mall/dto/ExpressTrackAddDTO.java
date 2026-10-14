package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 追加物流轨迹请求参数（管理端）
 */
@Data
@Schema(description = "追加物流轨迹请求参数")
public class ExpressTrackAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点状态：已揽收/运输中/派送中/已签收", example = "派送中")
    private String status;

    @Schema(description = "轨迹描述", example = "快递员正在派送，请保持电话畅通")
    private String context;

    @Schema(description = "所在城市", example = "南京市")
    private String location;
}
