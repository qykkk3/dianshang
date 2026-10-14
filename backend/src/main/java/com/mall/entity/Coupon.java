package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券表
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 优惠券ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 券名称 */
    private String name;

    /** 类型 1满减 2折扣 */
    private Integer type;

    /** 满减金额(type=1) */
    private BigDecimal amount;

    /** 折扣率(type=2,如8.5表示8.5折) */
    private BigDecimal discount;

    /** 使用门槛金额 */
    private BigDecimal minAmount;

    /** 发行总量 */
    private Integer totalCount;

    /** 已领取数量 */
    private Integer receivedCount;

    /** 已使用数量 */
    private Integer usedCount;

    /** 每人限领 */
    private Integer perLimit;

    /** 适用范围 1全场 2指定分类 3指定商品 */
    private Integer scope;

    /** 范围ID,逗号分隔 */
    private String scopeIds;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 状态 1启用 0停用 */
    private Integer status;

    /** 使用说明 */
    private String description;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @TableLogic
    private Integer deleted;
}
