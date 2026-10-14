package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置新增 / 修改参数
 */
@Data
@Schema(description = "系统配置新增/修改参数")
public class SysConfigSaveDTO {

    @Schema(description = "配置ID，按 configKey 更新时可不传")
    private Long id;

    @Schema(description = "配置键，唯一")
    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过 100 位")
    private String configKey;

    @Schema(description = "配置值")
    @Size(max = 500, message = "配置值长度不能超过 500 位")
    private String configValue;

    @Schema(description = "配置名称，新增时缺省取配置键")
    @Size(max = 100, message = "配置名称长度不能超过 100 位")
    private String configName;

    @Schema(description = "说明")
    @Size(max = 255, message = "说明长度不能超过 255 位")
    private String remark;
}
