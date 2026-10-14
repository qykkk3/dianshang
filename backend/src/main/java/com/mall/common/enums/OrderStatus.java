package com.mall.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态
 */
@Getter
public enum OrderStatus {

    /** 待付款 */
    UNPAID(0, "待付款"),
    /** 待发货（已付款） */
    PAID(1, "待发货"),
    /** 待收货（已发货） */
    SHIPPED(2, "待收货"),
    /** 已完成 */
    FINISHED(3, "已完成"),
    /** 已取消 */
    CANCELLED(4, "已取消"),
    /** 售后中 */
    AFTER_SALE(5, "售后中");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .map(OrderStatus::getDesc)
                .findFirst()
                .orElse("未知");
    }

    /** 已付款（含后续所有状态），用于销售额统计 */
    public static boolean isPaid(Integer code) {
        return code != null && (code == 1 || code == 2 || code == 3 || code == 5);
    }

    /** 买家是否可以申请售后 */
    public static boolean canApplyAfterSale(Integer code) {
        return code != null && (code == 1 || code == 2 || code == 3);
    }
}
