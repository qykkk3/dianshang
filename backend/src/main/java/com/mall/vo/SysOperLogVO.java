package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@Schema(description = "操作日志")
public class SysOperLogVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人账号")
    private String username;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求URI")
    private String requestUri;

    @Schema(description = "请求参数")
    private String params;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "耗时(ms)")
    private Long costTime;

    @Schema(description = "状态 1成功 0失败")
    private Integer status;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
