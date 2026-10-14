package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品分类新增/编辑参数
 */
@Data
@Schema(description = "商品分类新增/编辑参数")
public class CategorySaveDTO {

    @Schema(description = "分类ID，新增时为空")
    private Long id;

    @Schema(description = "父分类ID，0 表示一级分类", defaultValue = "0")
    private Long parentId = 0L;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String name;

    @Schema(description = "分类图标")
    @Size(max = 255, message = "图标地址不能超过 255 个字符")
    private String icon;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "状态 1启用 0停用")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}
