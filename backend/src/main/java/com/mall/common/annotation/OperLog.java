package com.mall.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，自动记录到 sys_oper_log
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 所属模块，如：商品管理 */
    String module() default "";

    /** 操作描述，如：新增商品 */
    String value() default "";

    /** 是否记录请求参数 */
    boolean saveParams() default true;
}
