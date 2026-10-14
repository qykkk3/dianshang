package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 操作日志查询条件
 */
@Data
@Schema(description = "操作日志查询条件")
public class SysOperLogQueryDTO {

    @Schema(description = "关键字：操作描述 / 操作人 / 请求URI，模糊匹配")
    private String keyword;

    @Schema(description = "模块，如：系统管理")
    private String module;

    @Schema(description = "状态 1成功 0失败")
    private Integer status;

    @Schema(description = "开始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间，支持 yyyy-MM-dd（含当天）或 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "页码，从 1 开始", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;
}
