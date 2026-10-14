package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端轮播图新增 / 修改参数
 */
@Data
@Schema(description = "轮播图保存参数")
public class BannerSaveDTO {

    @Schema(description = "轮播图ID，为空表示新增")
    private Long id;

    @Schema(description = "标题")
    @NotBlank(message = "请填写轮播图标题")
    @Size(max = 100, message = "标题最长 100 个字符")
    private String title;

    @Schema(description = "图片地址")
    @NotBlank(message = "请上传轮播图片")
    @Size(max = 255, message = "图片地址过长")
    private String image;

    @Schema(description = "跳转链接")
    @Size(max = 255, message = "跳转链接过长")
    private String linkUrl;

    @Schema(description = "位置 1商城首页 2分类页")
    @Min(value = 1, message = "位置取值不合法")
    @Max(value = 2, message = "位置取值不合法")
    private Integer position;

    @Schema(description = "排序值，越小越前")
    private Integer sort;

    @Schema(description = "状态 1启用 0停用")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
