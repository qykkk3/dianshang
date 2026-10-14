package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置项
 */
@Data
@Schema(description = "系统配置项")
public class SysConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "说明")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
