package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单节点（管理端左侧菜单树）
 */
@Data
@Schema(description = "菜单节点")
public class MenuVO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "父级ID，0 为顶级")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "类型 1目录 2菜单 3按钮")
    private Integer type;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "子菜单")
    private List<MenuVO> children = new ArrayList<>();
}
