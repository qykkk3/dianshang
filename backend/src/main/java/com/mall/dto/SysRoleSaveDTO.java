package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色新增 / 编辑参数
 */
@Data
@Schema(description = "角色新增/编辑参数")
public class SysRoleSaveDTO {

    @Schema(description = "角色ID，新增时为空，编辑时必传")
    private Long id;

    @Schema(description = "角色编码，仅允许大写字母与下划线，如 OPERATOR")
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z_]+$", message = "角色编码只能由大写字母和下划线组成")
    @Size(max = 50, message = "角色编码长度不能超过 50 位")
    private String roleCode;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过 50 位")
    private String roleName;

    @Schema(description = "角色描述")
    @Size(max = 255, message = "角色描述长度不能超过 255 位")
    private String description;

    @Schema(description = "状态 1启用 0停用，新增时缺省为 1")
    private Integer status;
}
