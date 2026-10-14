package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 买家发表商品评价参数
 */
@Data
@Schema(description = "发表评价参数")
public class CommentAddDTO {

    @Schema(description = "订单项ID")
    @NotNull(message = "缺少订单项ID")
    private Long orderItemId;

    @Schema(description = "评分 1-5")
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分范围为 1-5")
    @Max(value = 5, message = "评分范围为 1-5")
    private Integer star;

    @Schema(description = "评价内容")
    @NotBlank(message = "请填写评价内容")
    @Size(max = 500, message = "评价内容最长 500 个字符")
    private String content;

    @Schema(description = "晒图地址集合")
    private List<String> images;

    @Schema(description = "是否匿名 0否 1是")
    @Min(value = 0, message = "是否匿名取值不合法")
    @Max(value = 1, message = "是否匿名取值不合法")
    private Integer isAnonymous;
}
