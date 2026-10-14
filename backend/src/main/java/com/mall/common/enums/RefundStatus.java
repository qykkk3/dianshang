package com.mall.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 售后工单状态
 */
@Getter
public enum RefundStatus {

    /** 待审核 */
    PENDING(0, "待审核"),
    /** 已同意 */
    AGREED(1, "已同意"),
    /** 已拒绝 */
    REJECTED(2, "已拒绝"),
    /** 待买家退货 */
    WAIT_RETURN(3, "待买家退货"),
    /** 退款完成 */
    FINISHED(4, "退款完成"),
    /** 已撤销 */
    CANCELLED(5, "已撤销");

    private final Integer code;
    private final String desc;

    RefundStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .map(RefundStatus::getDesc)
                .findFirst()
                .orElse("未知");
    }
}
