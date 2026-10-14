package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券表
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户券ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 优惠券ID */
    private Long couponId;

    /** 券名称(快照) */
    private String couponName;

    /** 状态 0未使用 1已使用 2已过期 */
    private Integer status;

    /** 使用的订单ID */
    private Long orderId;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 过期时间 */
    private LocalDateTime expireTime;
}
