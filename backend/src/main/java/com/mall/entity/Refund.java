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
 * 售后工单表
 */
@Data
@TableName("refund")
public class Refund implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 售后ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 售后单号 */
    private String refundNo;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 订单项ID(单品售后) */
    private Long orderItemId;

    /** 买家ID */
    private Long userId;

    /** 类型 1仅退款 2退货退款 3换货 */
    private Integer type;

    /** 申请原因 */
    private String reason;

    /** 问题描述 */
    private String description;

    /** 凭证图片,逗号分隔 */
    private String images;

    /** 申请退款金额 */
    private BigDecimal refundAmount;

    /** 状态 0待审核 1已同意 2已拒绝 3待买家退货 4退款完成 5已撤销 */
    private Integer status;

    /** 审核人ID */
    private Long auditUserId;

    /** 审核人 */
    private String auditUser;

    /** 审核意见 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
