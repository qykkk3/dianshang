package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限菜单（树形结构）
 */
@Data
@Schema(description = "权限菜单（树形结构）")
public class SysPermissionVO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父级ID，0 为顶级")
    private Long parentId;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限标识，如 sys:user:list")
    private String perms;

    @Schema(description = "类型 1目录 2菜单 3按钮")
    private Integer type;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "子权限")
    private List<SysPermissionVO> children = new ArrayList<>();
}
