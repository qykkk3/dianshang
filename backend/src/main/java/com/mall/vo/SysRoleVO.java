package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色信息（含权限ID集合与关联管理员数量）
 */
@Data
@Schema(description = "角色信息")
public class SysRoleVO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色编码 ADMIN/OPERATOR/WAREHOUSE")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "该角色拥有的权限ID集合")
    private List<Long> permissionIds = new ArrayList<>();

    @Schema(description = "使用该角色的管理员数量")
    private Long userCount = 0L;
}
