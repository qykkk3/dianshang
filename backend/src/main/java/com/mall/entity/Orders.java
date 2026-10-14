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
 * 订单主表
 */
@Data
@TableName("orders")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 买家ID */
    private Long userId;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 使用的用户优惠券ID */
    private Long userCouponId;

    /** 状态 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中 */
    private Integer status;

    /** 支付方式 1模拟支付 */
    private Integer payType;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 关闭(取消)时间 */
    private LocalDateTime closeTime;

    /** 收货人(快照) */
    private String receiverName;

    /** 收货电话(快照) */
    private String receiverPhone;

    /** 收货地址(快照) */
    private String receiverAddress;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 买家备注 */
    private String remark;

    /** 商家备注 */
    private String adminRemark;

    /** 商品件数 */
    private Integer itemCount;

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
