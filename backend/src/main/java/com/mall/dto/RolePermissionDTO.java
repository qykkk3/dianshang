package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配参数
 */
@Data
@Schema(description = "角色权限分配参数")
public class RolePermissionDTO {

    @Schema(description = "权限ID集合，全量覆盖该角色的权限；空集合表示清空权限")
    private List<Long> permissionIds;
}
