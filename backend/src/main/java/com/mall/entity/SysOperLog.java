package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台操作日志表
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long userId;

    /** 操作人 */
    private String username;

    /** 模块 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 请求URI */
    private String requestUri;

    /** 请求参数 */
    private String params;

    /** 操作IP */
    private String ip;

    /** 耗时(ms) */
    private Long costTime;

    /** 状态 1成功 0失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
