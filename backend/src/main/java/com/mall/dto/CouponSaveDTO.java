package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端优惠券新增 / 修改参数
 *
 * <p>时间字段统一使用字符串 "yyyy-MM-dd HH:mm:ss" 接收，由 Service 解析。</p>
 */
@Data
@Schema(description = "优惠券保存参数")
public class CouponSaveDTO {

    @Schema(description = "优惠券ID，为空表示新增")
    private Long id;

    @Schema(description = "券名称")
    @NotBlank(message = "请填写券名称")
    @Size(max = 100, message = "券名称最长 100 个字符")
    private String name;

    @Schema(description = "类型 1满减 2折扣")
    @NotNull(message = "请选择优惠券类型")
    @Min(value = 1, message = "优惠券类型不合法")
    @Max(value = 2, message = "优惠券类型不合法")
    private Integer type;

    @Schema(description = "满减金额(type=1)")
    @Min(value = 0, message = "满减金额不能为负数")
    private BigDecimal amount;

    @Schema(description = "折扣率(type=2，如 8.5 表示 8.5 折)")
    @Min(value = 0, message = "折扣不合法")
    private BigDecimal discount;

    @Schema(description = "使用门槛金额")
    @Min(value = 0, message = "使用门槛金额不能为负数")
    private BigDecimal minAmount;

    @Schema(description = "发行总量")
    @NotNull(message = "请填写发行总量")
    @Min(value = 1, message = "发行总量至少为 1")
    private Integer totalCount;

    @Schema(description = "每人限领数量")
    @Min(value = 1, message = "每人限领至少为 1")
    private Integer perLimit;

    @Schema(description = "适用范围 1全场 2指定分类 3指定商品")
    @NotNull(message = "请选择适用范围")
    @Min(value = 1, message = "适用范围不合法")
    @Max(value = 3, message = "适用范围不合法")
    private Integer scope;

    @Schema(description = "范围ID集合，scope=2 为分类ID，scope=3 为商品ID")
    private List<Long> scopeIds;

    @Schema(description = "生效时间 yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "请选择生效时间")
    private String startTime;

    @Schema(description = "失效时间 yyyy-MM-dd HH:mm:ss")
    @NotBlank(message = "请选择失效时间")
    private String endTime;

    @Schema(description = "状态 1启用 0停用")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;

    @Schema(description = "使用说明")
    @Size(max = 255, message = "使用说明最长 255 个字符")
    private String description;
}
