package com.mall.common;

import lombok.Getter;

/**
 * 统一响应状态码
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),
    /** 参数错误 */
    PARAM_ERROR(400, "请求参数错误"),
    /** 未登录 / 登录过期 */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    /** 无权限 */
    FORBIDDEN(403, "抱歉，您没有该操作权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),
    /** 系统异常 */
    ERROR(500, "系统繁忙，请稍后重试"),
    /** 业务异常 */
    BUSINESS_ERROR(600, "业务处理失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
