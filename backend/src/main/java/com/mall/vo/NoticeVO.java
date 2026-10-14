package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告
 */
@Data
@Schema(description = "公告")
public class NoticeVO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "类型 1平台公告 2促销活动 3帮助中心")
    private Integer type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "状态 1已发布 0草稿")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
