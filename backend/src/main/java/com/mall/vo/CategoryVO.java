package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类树节点
 */
@Data
@Schema(description = "商品分类树节点")
public class CategoryVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID，0 表示一级分类")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类图标")
    private String icon;

    @Schema(description = "层级 1一级 2二级")
    private Integer level;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "状态 1启用 0停用")
    private Integer status;

    @Schema(description = "商品数量：一级分类为其自身与子分类商品数之和")
    private Long productCount = 0L;

    @Schema(description = "子分类")
    private List<CategoryVO> children = new ArrayList<>();
}
