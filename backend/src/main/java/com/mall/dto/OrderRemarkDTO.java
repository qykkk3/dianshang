package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单备注请求参数（管理端）
 */
@Data
@Schema(description = "订单备注请求参数")
public class OrderRemarkDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商家备注", example = "已电话确认收货时间")
    private String adminRemark;
}
