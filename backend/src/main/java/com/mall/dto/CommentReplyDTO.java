package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端回复评价参数
 */
@Data
@Schema(description = "回复评价参数")
public class CommentReplyDTO {

    @Schema(description = "回复内容")
    @NotBlank(message = "请填写回复内容")
    @Size(max = 500, message = "回复内容最长 500 个字符")
    private String replyContent;
}
