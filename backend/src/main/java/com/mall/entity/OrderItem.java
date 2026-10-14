package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细表
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单项ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 买家ID */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** 商品名称(快照) */
    private String productName;

    /** 商品图片(快照) */
    private String productImage;

    /** 规格(快照) */
    private String specInfo;

    /** 成交单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal totalAmount;

    /** 是否已评价 */
    private Integer isCommented;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
