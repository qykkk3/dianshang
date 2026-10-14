package com.mall.common;

import lombok.Getter;

/**
 * 业务异常：由业务代码主动抛出，交给全局异常处理器转换为统一响应
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /** 条件成立时抛出业务异常 */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }

    /** 对象为空时抛出业务异常 */
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new BusinessException(message);
        }
        return obj;
    }
}
