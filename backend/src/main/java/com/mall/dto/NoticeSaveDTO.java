package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端公告新增 / 修改参数
 */
@Data
@Schema(description = "公告保存参数")
public class NoticeSaveDTO {

    @Schema(description = "公告ID，为空表示新增")
    private Long id;

    @Schema(description = "标题")
    @NotBlank(message = "请填写公告标题")
    @Size(max = 200, message = "标题最长 200 个字符")
    private String title;

    @Schema(description = "正文")
    @NotBlank(message = "请填写公告正文")
    private String content;

    @Schema(description = "类型 1平台公告 2促销活动 3帮助中心")
    @Min(value = 1, message = "公告类型不合法")
    @Max(value = 3, message = "公告类型不合法")
    private Integer type;

    @Schema(description = "状态 1已发布 0草稿")
    @Min(value = 0, message = "状态取值不合法")
    @Max(value = 1, message = "状态取值不合法")
    private Integer status;
}
